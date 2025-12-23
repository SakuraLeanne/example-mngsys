package com.example.mngsys.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mngsys.portal.controller.dto.AppMenuTreeNode;
import com.example.mngsys.portal.entity.AppMenuResource;
import com.example.mngsys.portal.entity.AppRole;
import com.example.mngsys.portal.entity.AppRoleMenu;
import com.example.mngsys.portal.entity.AppUserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
/**
 * AppMenuDeliveryService。
 */
public class AppMenuDeliveryService {

    private static final String MENU_CACHE_PREFIX = "app:menu:user:";
    private static final long MENU_CACHE_TTL_SECONDS = 1800;

    private final AppUserRoleService appUserRoleService;
    private final AppRoleService appRoleService;
    private final AppRoleMenuService appRoleMenuService;
    private final AppMenuResourceService appMenuResourceService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AppMenuDeliveryService(AppUserRoleService appUserRoleService,
                                  AppRoleService appRoleService,
                                  AppRoleMenuService appRoleMenuService,
                                  AppMenuResourceService appMenuResourceService,
                                  StringRedisTemplate stringRedisTemplate,
                                  ObjectMapper objectMapper) {
        this.appUserRoleService = appUserRoleService;
        this.appRoleService = appRoleService;
        this.appRoleMenuService = appRoleMenuService;
        this.appMenuResourceService = appMenuResourceService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<AppMenuTreeNode> loadMenus(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        String cacheKey = buildMenuCacheKey(userId);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            List<AppMenuTreeNode> nodes = parseCached(cached);
            if (nodes != null) {
                return nodes;
            }
        }
        List<AppMenuTreeNode> menus = queryMenus(userId);
        cacheMenus(cacheKey, menus);
        return menus;
    }

    private List<AppMenuTreeNode> queryMenus(Long userId) {
        List<AppUserRole> userRoles = appUserRoleService.list(new LambdaQueryWrapper<AppUserRole>()
                .eq(AppUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoles.stream().map(AppUserRole::getRoleId).distinct().collect(Collectors.toList());
        List<AppRole> roles = appRoleService.list(new LambdaQueryWrapper<AppRole>()
                .in(AppRole::getId, roleIds)
                .eq(AppRole::getStatus, 1));
        if (roles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> activeRoleIds = roles.stream().map(AppRole::getId).collect(Collectors.toList());
        List<AppRoleMenu> roleMenus = appRoleMenuService.list(new LambdaQueryWrapper<AppRoleMenu>()
                .in(AppRoleMenu::getRoleId, activeRoleIds));
        if (roleMenus.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> menuIds = roleMenus.stream().map(AppRoleMenu::getMenuId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(menuIds)) {
            return new ArrayList<>();
        }
        List<AppMenuResource> menus = appMenuResourceService.list(new LambdaQueryWrapper<AppMenuResource>()
                .in(AppMenuResource::getId, menuIds)
                .eq(AppMenuResource::getStatus, 1));
        return buildTree(menus);
    }

    private String buildMenuCacheKey(Long userId) {
        return MENU_CACHE_PREFIX + userId;
    }

    private List<AppMenuTreeNode> parseCached(String cached) {
        try {
            return objectMapper.readValue(cached, new TypeReference<List<AppMenuTreeNode>>() {
            });
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private void cacheMenus(String key, List<AppMenuTreeNode> menus) {
        try {
            String payload = objectMapper.writeValueAsString(menus);
            stringRedisTemplate.opsForValue().set(key, payload, MENU_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            // ignore cache write failure
        }
    }

    private List<AppMenuTreeNode> buildTree(List<AppMenuResource> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, AppMenuTreeNode> nodeMap = new HashMap<>();
        for (AppMenuResource menu : menus) {
            nodeMap.put(menu.getId(), toNode(menu));
        }
        List<AppMenuTreeNode> roots = new ArrayList<>();
        for (AppMenuResource menu : menus) {
            AppMenuTreeNode node = nodeMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    private AppMenuTreeNode toNode(AppMenuResource menu) {
        return new AppMenuTreeNode(menu.getId(), menu.getAppCode(), menu.getMenuCode(), menu.getMenuName(),
                menu.getMenuPath(), menu.getMenuType(), menu.getParentId(), menu.getPermission(), menu.getSort(),
                menu.getStatus());
    }

    private void sortTree(List<AppMenuTreeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        Comparator<AppMenuTreeNode> comparator = Comparator
                .comparing((AppMenuTreeNode node) -> node.getSort() == null ? 0 : node.getSort())
                .thenComparing(node -> node.getId() == null ? 0L : node.getId());
        nodes.sort(comparator);
        for (AppMenuTreeNode node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                node.getChildren().sort(comparator);
                sortTree(node.getChildren());
            }
        }
    }
}
