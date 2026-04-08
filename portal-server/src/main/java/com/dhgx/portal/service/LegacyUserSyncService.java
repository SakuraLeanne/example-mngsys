package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dhgx.portal.config.LegacyUserSyncProperties;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.entity.TbAppUser;
import com.dhgx.portal.mapper.PortalUserMapper;
import com.dhgx.portal.mapper.TbAppUserMapper;
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
 * <p>
 * 同步分为两个阶段：
 * <ol>
 *     <li>外部接口同步：分页拉取账号数据（接口1），按工号查询员工详情（接口2），写入 portal_user。</li>
 *     <li>tb_app_user 补充同步：仅同步 status=0 且 del_flag=0 的用户，按手机号/用户名去重后写入 portal_user。</li>
 * </ol>
 * </p>
 * <p>
 * 冲突规则：
 * <ul>
 *     <li>手机号冲突时，若冲突记录来源是接口同步（create_by=sync_api），tb_app_user 数据不允许覆盖。</li>
 *     <li>否则允许覆盖，并将被覆盖记录手机号回退到其 username 或 id。</li>
 * </ul>
 * </p>
 */
@Service
public class LegacyUserSyncService {

    private static final Logger log = LoggerFactory.getLogger(LegacyUserSyncService.class);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 默认密码（BCrypt 哈希值）。
     * 明文密码：123456。
     */
    private static final String DEFAULT_PASSWORD = "{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5Cw5IV/pY5PaaC2l5x4pnW5sA8vz";
    private static final String SOURCE_API = "sync_api";
    private static final String SOURCE_TB_APP = "sync_tb_app";

    private final LegacyUserSyncProperties properties;
    private final PortalUserMapper portalUserMapper;
    private final TbAppUserMapper tbAppUserMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public LegacyUserSyncService(LegacyUserSyncProperties properties,
                                 PortalUserMapper portalUserMapper,
                                 TbAppUserMapper tbAppUserMapper) {
        this.properties = properties;
        this.portalUserMapper = portalUserMapper;
        this.tbAppUserMapper = tbAppUserMapper;
    }

    public SyncResult syncUsers(LocalDateTime start, LocalDateTime end, Integer pageSizeArg) {
        LocalDateTime startTime = start == null ? LocalDateTime.of(1970, 1, 1, 0, 0, 0) : start;
        LocalDateTime endTime = end == null ? LocalDateTime.now() : end;
        int pageSize = pageSizeArg == null || pageSizeArg <= 0 ? properties.getPageSize() : pageSizeArg;
        Map<String, Map<String, Object>> empCache = new HashMap<>();

        SyncResult result = new SyncResult();
        syncApiUsers(startTime, endTime, pageSize, empCache, result);
        syncTbAppUsers(result);
        return result;
    }

    /**
     * 同步外部接口用户（接口1/2）。
     *
     * @param startTime 增量开始时间
     * @param endTime   增量结束时间
     * @param pageSize  每页数量
     * @param empCache  员工详情缓存（同一次任务中同工号仅查询一次接口2）
     * @param result    同步统计
     */
    private void syncApiUsers(LocalDateTime startTime,
                              LocalDateTime endTime,
                              int pageSize,
                              Map<String, Map<String, Object>> empCache,
                              SyncResult result) {
        int pageNo = 1;
        int totalPages = 1;
        while (pageNo <= totalPages) {
            Map<String, Object> pageResp = queryAccountPage(startTime, endTime, pageSize, pageNo);
            Map<String, Object> esb = map(pageResp.get("ESB"));
            totalPages = intValue(map(map(esb.get("DATA")).get("SPLITPAGE")).get("TOTALPAGES"), totalPages);
            List<Map<String, Object>> rows = list(map(map(esb.get("DATA")).get("DATAINFOS")).get("DATAINFO"));
            for (Map<String, Object> row : rows) {
                result.apiTotal++;
                try {
                    upsertApiOne(row, empCache, result);
                    result.apiSuccess++;
                } catch (Exception ex) {
                    result.apiFailed++;
                    log.warn("sync api user failed, code={}, err={}", stringValue(row.get("DESC13")), ex.getMessage());
                }
            }
            pageNo++;
        }
    }

    /**
     * 同步单个接口用户。
     *
     * @param account 接口1账号记录
     * @param empCache 员工详情缓存
     * @param result 统计信息
     */
    private void upsertApiOne(Map<String, Object> account,
                              Map<String, Map<String, Object>> empCache,
                              SyncResult result) throws InterruptedException {
        String empCode = stringValue(account.get("DESC13"));
        if (!StringUtils.hasText(empCode)) {
            throw new IllegalArgumentException("DESC13工号为空");
        }
        PortalUser user = portalUserMapper.selectById(empCode);
        if (user == null) {
            user = new PortalUser();
            user.setId(empCode);
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

        fillRequiredFields(user, stringValue(account.get("DESC1")), SOURCE_API);

        resolveMobileConflict(user, SOURCE_API, result);
        if (portalUserMapper.selectById(user.getId()) == null) {
            portalUserMapper.insert(user);
        } else {
            portalUserMapper.updateById(user);
        }
    }

    /**
     * 同步 tb_app_user 表中有效用户。
     * 只同步 status=0 且 del_flag=0 的记录。
     */
    private void syncTbAppUsers(SyncResult result) {
        LambdaQueryWrapper<TbAppUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TbAppUser::getStatus, "0").eq(TbAppUser::getDelFlag, "0");
        List<TbAppUser> users = tbAppUserMapper.selectList(queryWrapper);
        for (TbAppUser tbUser : users) {
            result.tbTotal++;
            try {
                syncOneTbAppUser(tbUser, result);
            } catch (Exception ex) {
                result.tbFailed++;
                log.warn("sync tb_app_user failed, userId={}, err={}", tbUser == null ? null : tbUser.getUserId(), ex.getMessage());
            }
        }
    }

    /**
     * 同步单个 tb_app_user 记录。
     * <p>
     * 去重优先级：手机号优先、用户名其次。
     * </p>
     * <p>
     * 约束：
     * <ul>
     *     <li>phonenumber 为空直接跳过。</li>
     *     <li>若匹配到接口来源用户（create_by=sync_api），视为重复，跳过同步。</li>
     * </ul>
     * </p>
     */
    private void syncOneTbAppUser(TbAppUser tbUser, SyncResult result) {
        if (tbUser == null) {
            return;
        }
        String mobile = stringValue(tbUser.getPhonenumber());
        if (!StringUtils.hasText(mobile)) {
            result.tbSkippedNoMobile++;
            return;
        }

        PortalUser existedByMobile = findByMobile(mobile);
        if (isApiSource(existedByMobile)) {
            result.tbSkippedDuplicate++;
            return;
        }

        String userName = stringValue(tbUser.getUserName());
        if (StringUtils.hasText(userName)) {
            PortalUser existedByUsername = findByUsername(userName);
            if (isApiSource(existedByUsername)) {
                result.tbSkippedDuplicate++;
                return;
            }
        }

        PortalUser user = null;
        String userId = stringValue(tbUser.getUserId());
        if (StringUtils.hasText(userId)) {
            user = portalUserMapper.selectById(userId);
        }
        if (user == null) {
            user = existedByMobile != null ? existedByMobile : null;
        }
        if (user == null && StringUtils.hasText(userName)) {
            user = findByUsername(userName);
        }
        if (user == null) {
            user = new PortalUser();
            user.setId(StringUtils.hasText(userId) ? userId : UUID.randomUUID().toString().replace("-", ""));
        }

        user.setMobile(mobile);
        user.setUsername(mobile);
        user.setRealName(stringValue(tbUser.getFullName()));
        user.setNickName(stringValue(tbUser.getNickName()));
        user.setEmail(stringValue(tbUser.getEmail()));
        user.setRemark(stringValue(tbUser.getRemark()));
        user.setStatus(1);
        user.setGender(mapTbSex(tbUser.getSex()));
        if (StringUtils.hasText(tbUser.getBirthday())) {
            parseBirthday(tbUser.getBirthday(), user);
        }

        fillRequiredFields(user, mobile, SOURCE_TB_APP);
        resolveMobileConflict(user, SOURCE_TB_APP, result);

        if (portalUserMapper.selectById(user.getId()) == null) {
            portalUserMapper.insert(user);
            result.tbInserted++;
        } else {
            portalUserMapper.updateById(user);
            result.tbUpdated++;
        }
    }

    /**
     * 填充 portal_user 必填字段与同步来源标记。
     *
     * @param user 目标用户
     * @param defaultMobile 默认手机号（当目标手机号为空时兜底）
     * @param source 来源标识（sync_api / sync_tb_app）
     */
    private void fillRequiredFields(PortalUser user, String defaultMobile, String source) {
        if (!StringUtils.hasText(user.getMobile())) {
            user.setMobile(defaultMobile);
        }
        if (StringUtils.hasText(user.getMobile())) {
            user.setUsername(user.getMobile());
        }
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(DEFAULT_PASSWORD);
        }
        if (user.getMobileVerified() == null) {
            user.setMobileVerified(0);
        }
        if (user.getEmailVerified() == null) {
            user.setEmailVerified(0);
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setCreateBy(source);
        user.setUpdateBy(source);
    }

    /**
     * 处理手机号冲突。
     *
     * @param user 同步中的目标用户
     * @param incomingSource 本次同步来源
     * @param result 统计信息
     */
    private void resolveMobileConflict(PortalUser user, String incomingSource, SyncResult result) {
        if (!StringUtils.hasText(user.getMobile())) {
            return;
        }
        PortalUser occupied = findByMobileExcludeId(user.getMobile(), user.getId());
        if (occupied == null) {
            return;
        }
        if (SOURCE_TB_APP.equals(incomingSource) && isApiSource(occupied)) {
            throw new IllegalStateException("手机号被接口同步数据占用，不允许覆盖");
        }
        String fallback = StringUtils.hasText(occupied.getUsername()) ? occupied.getUsername() : occupied.getId();
        if (user.getMobile().equals(fallback)) {
            fallback = occupied.getId();
        }
        occupied.setMobile(fallback);
        occupied.setUsername(fallback);
        occupied.setUpdateBy(incomingSource);
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
            parseBirthday(birthday, user);
        }
        String genderCode = stringValue(emp.get("DESC4"));
        if ("2".equals(genderCode)) {
            user.setGender("F");
        } else if ("1".equals(genderCode)) {
            user.setGender("M");
        }
    }

    private void parseBirthday(String birthday, PortalUser user) {
        try {
            user.setBirthday(LocalDate.parse(birthday));
        } catch (Exception ignore) {
            log.warn("birthday parse failed, code={}, birthday={}", user.getId(), birthday);
        }
    }

    private String mapTbSex(String sex) {
        if ("1".equals(sex)) {
            return "M";
        }
        if ("2".equals(sex)) {
            return "F";
        }
        return null;
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

    private PortalUser findByMobile(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return null;
        }
        LambdaQueryWrapper<PortalUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalUser::getMobile, mobile).last("limit 1");
        return portalUserMapper.selectOne(wrapper);
    }

    private PortalUser findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        LambdaQueryWrapper<PortalUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalUser::getUsername, username).last("limit 1");
        return portalUserMapper.selectOne(wrapper);
    }

    private PortalUser findByMobileExcludeId(String mobile, String userId) {
        LambdaQueryWrapper<PortalUser> mobileWrapper = new LambdaQueryWrapper<>();
        mobileWrapper.eq(PortalUser::getMobile, mobile)
                .ne(StringUtils.hasText(userId), PortalUser::getId, userId)
                .last("limit 1");
        return portalUserMapper.selectOne(mobileWrapper);
    }

    private boolean isApiSource(PortalUser user) {
        return user != null && SOURCE_API.equals(user.getCreateBy());
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
        private int apiTotal;
        private int apiSuccess;
        private int apiFailed;
        private int tbTotal;
        private int tbInserted;
        private int tbUpdated;
        private int tbFailed;
        private int tbSkippedDuplicate;
        private int tbSkippedNoMobile;
        private int empCalls;
        private int mobileOverwritten;

        public int getApiTotal() {
            return apiTotal;
        }

        public int getApiSuccess() {
            return apiSuccess;
        }

        public int getApiFailed() {
            return apiFailed;
        }

        public int getTbTotal() {
            return tbTotal;
        }

        public int getTbInserted() {
            return tbInserted;
        }

        public int getTbUpdated() {
            return tbUpdated;
        }

        public int getTbFailed() {
            return tbFailed;
        }

        public int getTbSkippedDuplicate() {
            return tbSkippedDuplicate;
        }

        public int getTbSkippedNoMobile() {
            return tbSkippedNoMobile;
        }

        public int getEmpCalls() {
            return empCalls;
        }

        public int getMobileOverwritten() {
            return mobileOverwritten;
        }
    }
}
