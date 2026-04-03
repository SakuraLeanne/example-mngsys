package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dhgx.portal.config.LegacyUserSyncProperties;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.mapper.PortalUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 手动触发的用户同步服务。
 */
@Service
public class LegacyUserSyncService {

    private static final Logger log = LoggerFactory.getLogger(LegacyUserSyncService.class);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_PASSWORD = "{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5Cw5IV/pY5PaaC2l5x4pnW5sA8vz";

    private final LegacyUserSyncProperties properties;
    private final PortalUserMapper portalUserMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public LegacyUserSyncService(LegacyUserSyncProperties properties, PortalUserMapper portalUserMapper) {
        this.properties = properties;
        this.portalUserMapper = portalUserMapper;
    }

    public SyncResult syncUsers(LocalDateTime start, LocalDateTime end, Integer pageSizeArg) {
        LocalDateTime startTime = start == null ? LocalDateTime.of(1970, 1, 1, 0, 0, 0) : start;
        LocalDateTime endTime = end == null ? LocalDateTime.now() : end;
        int pageSize = pageSizeArg == null || pageSizeArg <= 0 ? properties.getPageSize() : pageSizeArg;
        Map<String, Map<String, Object>> empCache = new HashMap<>();

        SyncResult result = new SyncResult();
        int pageNo = 1;
        int totalPages = 1;

        while (pageNo <= totalPages) {
            Map<String, Object> pageResp = queryAccountPage(startTime, endTime, pageSize, pageNo);
            Map<String, Object> esb = map(pageResp.get("ESB"));
            totalPages = intValue(map(map(esb.get("DATA")).get("SPLITPAGE")).get("TOTALPAGES"), totalPages);
            List<Map<String, Object>> rows = list(map(map(esb.get("DATA")).get("DATAINFOS")).get("DATAINFO"));
            for (Map<String, Object> row : rows) {
                result.total++;
                try {
                    upsertOne(row, empCache, result);
                    result.success++;
                } catch (Exception ex) {
                    result.failed++;
                    log.warn("sync user failed, code={}, err={}", stringValue(row.get("DESC13")), ex.getMessage());
                }
            }
            pageNo++;
        }
        return result;
    }

    private void upsertOne(Map<String, Object> account, Map<String, Map<String, Object>> empCache, SyncResult result) throws InterruptedException {
        String empCode = stringValue(account.get("DESC13"));
        if (!StringUtils.hasText(empCode)) {
            throw new IllegalArgumentException("DESC13工号为空");
        }
        PortalUser user = portalUserMapper.selectById(empCode);
        if (user == null) {
            user = new PortalUser();
            user.setId(empCode);
            user.setPassword(DEFAULT_PASSWORD);
            user.setMobileVerified(0);
            user.setEmailVerified(0);
        }

        applyAccountFields(user, account);

        Map<String, Object> emp = empCache.get(empCode);
        if (emp == null) {
            if (result.empCalls > 0 && properties.getEmpRequestIntervalMs() > 0) {
                Thread.sleep(properties.getEmpRequestIntervalMs());
            }
            emp = queryEmpByCode(empCode);
            result.empCalls++;
            if (emp != null) {
                empCache.put(empCode, emp);
            }
        }
        if (emp != null) {
            applyEmpFields(user, emp);
        }

        if (!StringUtils.hasText(user.getMobile())) {
            user.setMobile(stringValue(account.get("DESC1")));
        }
        if (!StringUtils.hasText(user.getUsername())) {
            user.setUsername(user.getMobile());
        }
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(DEFAULT_PASSWORD);
        }

        resolveMobileConflict(user, result);
        if (StringUtils.hasText(user.getMobile())) {
            user.setUsername(user.getMobile());
        }

        if (portalUserMapper.selectById(user.getId()) == null) {
            portalUserMapper.insert(user);
        } else {
            portalUserMapper.updateById(user);
        }
    }

    private void resolveMobileConflict(PortalUser user, SyncResult result) {
        if (!StringUtils.hasText(user.getMobile())) {
            return;
        }
        LambdaQueryWrapper<PortalUser> mobileWrapper = new LambdaQueryWrapper<>();
        mobileWrapper.eq(PortalUser::getMobile, user.getMobile())
                .ne(PortalUser::getId, user.getId());
        PortalUser occupied = portalUserMapper.selectOne(mobileWrapper);
        if (occupied == null) {
            return;
        }
        String fallback = StringUtils.hasText(occupied.getUsername()) ? occupied.getUsername() : occupied.getId();
        if (user.getMobile().equals(fallback)) {
            fallback = occupied.getId();
        }
        occupied.setMobile(fallback);
        occupied.setUsername(fallback);
        portalUserMapper.updateById(occupied);
        result.mobileOverwritten++;
    }

    private void applyAccountFields(PortalUser user, Map<String, Object> account) {
        user.setRealName(stringValue(account.get("DESC4")));
        user.setCompanyName(stringValue(account.get("DESC6")));
        user.setRemark(stringValue(account.get("DESC7")));
        user.setStatus(mapStatus(stringValue(account.get("DESC8")), stringValue(account.get("DESC11"))));
        String login = stringValue(account.get("DESC1"));
        if (StringUtils.hasText(login) && !StringUtils.hasText(user.getMobile())) {
            user.setMobile(login);
        }
    }

    private void applyEmpFields(PortalUser user, Map<String, Object> emp) {
        String mobile = stringValue(emp.get("DESC10"));
        if (StringUtils.hasText(mobile)) {
            user.setMobile(mobile);
        }
        String realName = stringValue(emp.get("DESC1"));
        if (StringUtils.hasText(realName)) {
            user.setRealName(realName);
        }
        String birthday = stringValue(emp.get("DESC7"));
        if (StringUtils.hasText(birthday)) {
            try {
                user.setBirthday(LocalDate.parse(birthday));
            } catch (Exception ignore) {
                log.warn("birthday parse failed, code={}, birthday={}", user.getId(), birthday);
            }
        }
        String genderCode = stringValue(emp.get("DESC4"));
        if ("2".equals(genderCode)) {
            user.setGender("F");
        } else if ("1".equals(genderCode)) {
            user.setGender("M");
        }
    }

    private int mapStatus(String desc8, String desc11) {
        if ("1".equals(desc11)) {
            return 0;
        }
        if ("1".equals(desc8)) {
            return 2;
        }
        return 1;
    }

    private Map<String, Object> queryAccountPage(LocalDateTime start, LocalDateTime end, int pageSize, int currentPage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("usercode", properties.getAccountUsercode());
        headers.set("password", properties.getAccountPassword());

        Map<String, Object> dataInfo = new HashMap<>();
        dataInfo.put("LASTMODIFYRECORDTIME", start.format(DATE_TIME_FMT) + "~" + end.format(DATE_TIME_FMT));

        Map<String, Object> dataInfos = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(dataInfo);
        dataInfos.put("DATAINFO", list);
        dataInfos.put("PUUID", UUID.randomUUID().toString().replace("-", ""));

        Map<String, Object> splitPage = new HashMap<>();
        splitPage.put("COUNTPERPAGE", String.valueOf(pageSize));
        splitPage.put("CURRENTPAGE", String.valueOf(currentPage));

        Map<String, Object> data = new HashMap<>();
        data.put("DATAINFOS", dataInfos);
        data.put("SPLITPAGE", splitPage);

        Map<String, Object> esb = new HashMap<>();
        esb.put("DATA", data);

        Map<String, Object> body = new HashMap<>();
        body.put("ESB", esb);

        ResponseEntity<Map> response = restTemplate.postForEntity(properties.getBaseUrl() + properties.getAccountPath(),
                new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException("账号分页接口返回为空");
        }
        return responseBody;
    }

    private Map<String, Object> queryEmpByCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("usercode", properties.getEmpUsercode());
        headers.set("password", properties.getEmpPassword());

        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        ResponseEntity<Map> response = restTemplate.postForEntity(properties.getBaseUrl() + properties.getEmpPath(),
                new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !"OK".equals(responseBody.get("Status"))) {
            return null;
        }
        return map(responseBody.get("Return"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Object obj) {
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return new ArrayList<>();
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    public static class SyncResult {
        private int total;
        private int success;
        private int failed;
        private int empCalls;
        private int mobileOverwritten;

        public int getTotal() {
            return total;
        }

        public int getSuccess() {
            return success;
        }

        public int getFailed() {
            return failed;
        }

        public int getEmpCalls() {
            return empCalls;
        }

        public int getMobileOverwritten() {
            return mobileOverwritten;
        }
    }
}
