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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AppMenuDeliveryService。
 * <p>
 * 负责根据用户角色聚合菜单权限、生成树形菜单并缓存，提供前台菜单下发能力。
 * </p>
 */
@Service
public class AppMenuDeliveryService {

    /** 用户菜单缓存前缀。 */
    private static final String MENU_CACHE_PREFIX = "app:menu:user:";
    /** 菜单缓存过期时间（秒）。 */
    private static final long MENU_CACHE_TTL_SECONDS = 1800;

    /** 用户角色服务。 */
    private final AppUserRoleService appUserRoleService;
    /** 角色服务。 */
    private final AppRoleService appRoleService;
    /** 角色菜单关联服务。 */
    private final AppRoleMenuService appRoleMenuService;
    /** 菜单资源服务。 */
    private final AppMenuResourceService appMenuResourceService;
    /** Redis 模板，用于缓存菜单。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，注入依赖。
     */
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

    /**
     * 根据用户 ID 获取菜单树，先读缓存，未命中则查询数据库并回写缓存。
     *
     * @param userId 用户 ID
     * @return 菜单树节点列表
     */
    public List<AppMenuTreeNode> loadMenus(String userId) {
        if (!StringUtils.hasText(userId)) {
            return new ArrayList<>();
        }
        String cacheKey = buildMenuCacheKey(userId);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            List<AppMenuTreeNode> nodes = parseCached(cached);
            if (nodes != null) {
                return nodes;
            }
        }
        List<AppMenuTreeNode> menus = queryMenus(userId);
        cacheMenus(cacheKey, menus);
        return menus;
    }

    /**
     * 查询数据库构建菜单树。
     *
     * @param userId 用户 ID
     * @return 菜单树节点列表
     */
    private List<AppMenuTreeNode> queryMenus(String userId) {
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

    /**
     * 构建缓存键。
     *
     * @param userId 用户 ID
     * @return 缓存键名
     */
    private String buildMenuCacheKey(String userId) {
        return MENU_CACHE_PREFIX + userId;
    }

    /**
     * 解析缓存内容。
     *
     * @param cached 缓存的 JSON 字符串
     * @return 反序列化后的菜单列表，失败时返回 null
     */
    private List<AppMenuTreeNode> parseCached(String cached) {
        try {
            return objectMapper.readValue(cached, new TypeReference<List<AppMenuTreeNode>>() {
            });
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    /**
     * 写入缓存。
     *
     * @param key   缓存键
     * @param menus 菜单列表
     */
    private void cacheMenus(String key, List<AppMenuTreeNode> menus) {
        try {
            String payload = objectMapper.writeValueAsString(menus);
            stringRedisTemplate.opsForValue().set(key, payload, MENU_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            // ignore cache write failure
        }
    }

    /**
     * 将菜单列表转换为树形结构。
     *
     * @param menus 菜单实体列表
     * @return 树形节点列表
     */
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

    /**
     * 将菜单实体转换为树节点。
     *
     * @param menu 菜单实体
     * @return 树节点对象
     */
    private AppMenuTreeNode toNode(AppMenuResource menu) {
        return new AppMenuTreeNode(menu.getId(), menu.getAppCode(), menu.getMenuCode(), menu.getMenuName(),
                menu.getMenuPath(), menu.getMenuType(), menu.getParentId(), menu.getPermission(), menu.getSort(),
                menu.getStatus());
    }

    /**
     * 按 sort/id 对树进行递归排序。
     *
     * @param nodes 树节点列表
     */
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
