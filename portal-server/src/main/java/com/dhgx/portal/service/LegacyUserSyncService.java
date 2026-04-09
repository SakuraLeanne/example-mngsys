package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
    private static final int MOBILE_LOCK_SIZE = 256;

    private final LegacyUserSyncProperties properties;
    private final PortalUserMapper portalUserMapper;
    private final TbAppUserMapper tbAppUserMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ReentrantLock[] mobileLocks = new ReentrantLock[MOBILE_LOCK_SIZE];

    public LegacyUserSyncService(LegacyUserSyncProperties properties,
                                 PortalUserMapper portalUserMapper,
                                 TbAppUserMapper tbAppUserMapper) {
        this.properties = properties;
        this.portalUserMapper = portalUserMapper;
        this.tbAppUserMapper = tbAppUserMapper;
        for (int i = 0; i < MOBILE_LOCK_SIZE; i++) {
            mobileLocks[i] = new ReentrantLock();
        }
    }

    public SyncResult syncUsers(LocalDateTime start, LocalDateTime end, Integer pageSizeArg) {
        LocalDateTime startTime = start == null ? LocalDateTime.of(1970, 1, 1, 0, 0, 0) : start;
        LocalDateTime endTime = end == null ? LocalDateTime.now() : end;
        int pageSize = pageSizeArg == null || pageSizeArg <= 0 ? properties.getPageSize() : pageSizeArg;
        Map<String, Map<String, Object>> empCache = new HashMap<>();

        SyncResult result = new SyncResult();
        log.info("legacy user sync start, start={}, end={}, pageSize={}", startTime, endTime, pageSize);
        syncApiUsers(startTime, endTime, pageSize, empCache, result);
        syncTbAppUsers(result, pageSize);
        log.info("legacy user sync finished, apiTotal={}, apiSuccess={}, apiFailed={}, tbTotal={}, tbInserted={}, tbUpdated={}, tbFailed={}, tbManualReview={}, tbSkippedDuplicate={}, tbSkippedNoMobile={}",
                result.apiTotal, result.apiSuccess, result.apiFailed,
                result.tbTotal, result.tbInserted, result.tbUpdated, result.tbFailed,
                result.tbManualReview, result.tbSkippedDuplicate, result.tbSkippedNoMobile);
        return result;
    }

    /**
     * 异步执行同步任务。
     */
    @Async
    public void syncUsersAsync(LocalDateTime start, LocalDateTime end, Integer pageSizeArg) {
        syncUsers(start, end, pageSizeArg);
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
            int pageSuccess = 0;
            for (Map<String, Object> row : rows) {
                result.apiTotal++;
                try {
                    upsertApiOne(row, empCache, result);
                    result.apiSuccess++;
                    pageSuccess++;
                } catch (Exception ex) {
                    result.apiFailed++;
                    log.warn("sync api user failed, code={}, err={}", stringValue(row.get("DESC13")), ex.getMessage());
                }
            }
            log.info("sync_api page progress: currentPage={}, totalPages={}, pageSize={}, pageSuccess={}, apiSuccessTotal={}",
                    pageNo, totalPages, rows.size(), pageSuccess, result.apiSuccess);
            pageNo++;
        }
        log.info("sync_api finished: totalPages={}, apiTotal={}, apiSuccess={}, apiFailed={}",
                totalPages, result.apiTotal, result.apiSuccess, result.apiFailed);
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
        PortalUser user = safeSelectPortalUserById(empCode);
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
        if (safeSelectPortalUserById(user.getId()) == null) {
            safeInsertPortalUser(user);
        } else {
            safeUpdatePortalUser(user);
        }
    }

    /**
     * 同步 tb_app_user 表中有效用户。
     * 只同步 status=0 且 del_flag=0 的记录。
     */
    private void syncTbAppUsers(SyncResult result, int pageSize) {
        int threads = Math.max(1, properties.getTbThreads());
        int chunkSize = Math.max(1, properties.getTbChunkSize());
        int queueSize = Math.max(threads, properties.getTbQueueSize());
        ExecutorService executor = new ThreadPoolExecutor(
                threads,
                threads,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        int currentPage = 1;
        int totalPages = 1;
        try {
            while (currentPage <= totalPages) {
                LambdaQueryWrapper<TbAppUser> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TbAppUser::getStatus, "0").eq(TbAppUser::getDelFlag, "0");
                Page<TbAppUser> page = safeSelectTbAppUserPage(currentPage, pageSize, queryWrapper);
                totalPages = (int) page.getPages();

                List<List<TbAppUser>> chunks = splitChunks(page.getRecords(), chunkSize);
                List<Future<SyncResult>> futures = new ArrayList<>();
                for (List<TbAppUser> chunk : chunks) {
                    futures.add(executor.submit(() -> processTbChunk(chunk)));
                }

                int pageProcessed = 0;
                for (Future<SyncResult> future : futures) {
                    try {
                        SyncResult chunkResult = future.get();
                        result.mergeFrom(chunkResult);
                        pageProcessed += chunkResult.getTbInserted() + chunkResult.getTbUpdated();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("sync tb_app_user chunk interrupted, currentPage={}", currentPage);
                    } catch (ExecutionException ex) {
                        result.tbFailed++;
                        log.warn("sync tb_app_user chunk failed, currentPage={}, err={}", currentPage, ex.getMessage());
                    }
                }

                log.info("sync_tb_app page progress: currentPage={}, totalPages={}, pageSize={}, chunkCount={}, threads={}, pageProcessed={}, tbProcessedTotal={}",
                        currentPage, totalPages, page.getRecords().size(), chunks.size(), threads, pageProcessed, result.tbInserted + result.tbUpdated);
                currentPage++;
            }
        } finally {
            executor.shutdown();
        }
        log.info("sync_tb_app finished: tbTotal={}, tbInserted={}, tbUpdated={}, tbFailed={}, tbManualReview={}, tbSkippedDuplicate={}, tbSkippedNoMobile={}",
                result.tbTotal, result.tbInserted, result.tbUpdated, result.tbFailed, result.tbManualReview,
                result.tbSkippedDuplicate, result.tbSkippedNoMobile);
    }

    private SyncResult processTbChunk(List<TbAppUser> chunk) {
        SyncResult chunkResult = new SyncResult();
        for (TbAppUser tbUser : chunk) {
            chunkResult.tbTotal++;
            String mobile = tbUser == null ? null : stringValue(tbUser.getPhonenumber());
            ReentrantLock lock = getMobileLock(mobile);
            lock.lock();
            try {
                syncOneTbAppUser(tbUser, chunkResult);
            } catch (ManualReviewRequiredException ex) {
                chunkResult.tbManualReview++;
                log.warn("sync tb_app_user manual review required, userId={}, reason={}",
                        tbUser == null ? null : tbUser.getUserId(), ex.getMessage());
            } catch (Exception ex) {
                chunkResult.tbFailed++;
                log.warn("sync tb_app_user failed, userId={}, err={}", tbUser == null ? null : tbUser.getUserId(), ex.getMessage());
            } finally {
                lock.unlock();
            }
        }
        return chunkResult;
    }

    private List<List<TbAppUser>> splitChunks(List<TbAppUser> users, int chunkSize) {
        List<List<TbAppUser>> chunks = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return chunks;
        }
        for (int i = 0; i < users.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, users.size());
            chunks.add(new ArrayList<>(users.subList(i, end)));
        }
        return chunks;
    }

    private ReentrantLock getMobileLock(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return mobileLocks[0];
        }
        int idx = Math.abs(mobile.hashCode()) % MOBILE_LOCK_SIZE;
        return mobileLocks[idx];
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
            user = safeSelectPortalUserById(userId);
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

        if (safeSelectPortalUserById(user.getId()) == null) {
            safeInsertPortalUser(user);
            result.tbInserted++;
        } else {
            safeUpdatePortalUser(user);
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
        if (SOURCE_TB_APP.equals(incomingSource) && SOURCE_TB_APP.equals(occupied.getCreateBy())) {
            boolean occupiedHasRealName = StringUtils.hasText(occupied.getRealName());
            boolean incomingHasRealName = StringUtils.hasText(user.getRealName());
            if (!occupiedHasRealName && incomingHasRealName) {
                // 允许覆盖：新数据完整性更高
            } else if (occupiedHasRealName && !incomingHasRealName) {
                throw new IllegalStateException("手机号被更完整的tb_app_user记录占用，不允许覆盖");
            } else {
                throw new ManualReviewRequiredException("组合4完整性规则未命中（需人工干预）");
            }
        }
        String fallback = StringUtils.hasText(occupied.getUsername()) ? occupied.getUsername() : occupied.getId();
        if (user.getMobile().equals(fallback)) {
            fallback = occupied.getId();
        }
        occupied.setMobile(fallback);
        occupied.setUsername(fallback);
        occupied.setUpdateBy(incomingSource);
        safeUpdatePortalUser(occupied);
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
        return safeSelectPortalUserOne(wrapper);
    }

    private PortalUser findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        LambdaQueryWrapper<PortalUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalUser::getUsername, username).last("limit 1");
        return safeSelectPortalUserOne(wrapper);
    }

    private PortalUser findByMobileExcludeId(String mobile, String userId) {
        LambdaQueryWrapper<PortalUser> mobileWrapper = new LambdaQueryWrapper<>();
        mobileWrapper.eq(PortalUser::getMobile, mobile)
                .ne(StringUtils.hasText(userId), PortalUser::getId, userId)
                .last("limit 1");
        return safeSelectPortalUserOne(mobileWrapper);
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

    private PortalUser safeSelectPortalUserById(String id) {
        return executeWithRetry(() -> portalUserMapper.selectById(id), "selectById portal_user");
    }

    private PortalUser safeSelectPortalUserOne(LambdaQueryWrapper<PortalUser> wrapper) {
        return executeWithRetry(() -> portalUserMapper.selectOne(wrapper), "selectOne portal_user");
    }

    private void safeUpdatePortalUser(PortalUser user) {
        executeWithRetry(() -> {
            portalUserMapper.updateById(user);
            return 1;
        }, "updateById portal_user");
    }

    private void safeInsertPortalUser(PortalUser user) {
        executeWithRetry(() -> {
            portalUserMapper.insert(user);
            return 1;
        }, "insert portal_user");
    }

    private Page<TbAppUser> safeSelectTbAppUserPage(int currentPage, int pageSize, LambdaQueryWrapper<TbAppUser> wrapper) {
        return executeWithRetry(() -> tbAppUserMapper.selectPage(new Page<>(currentPage, pageSize), wrapper), "selectPage tb_app_user");
    }

    private <T> T executeWithRetry(java.util.concurrent.Callable<T> callable, String action) {
        int maxAttempts = 2;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callable.call();
            } catch (Exception ex) {
                if (attempt >= maxAttempts || !isRetryableDbException(ex)) {
                    throw new IllegalStateException("db action failed: " + action, ex);
                }
                log.warn("db action retry, action={}, attempt={}, err={}", action, attempt, ex.getMessage());
                sleepSilently(300L);
            }
        }
        throw new IllegalStateException("db action failed: " + action);
    }

    private boolean isRetryableDbException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLTransientConnectionException || current instanceof SQLRecoverableException) {
                return true;
            }
            String name = current.getClass().getName();
            if (name.contains("CommunicationsException") || name.contains("CJCommunicationsException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static class SyncResult {
        private int apiTotal;
        private int apiSuccess;
        private int apiFailed;
        private int tbTotal;
        private int tbInserted;
        private int tbUpdated;
        private int tbFailed;
        private int tbManualReview;
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

        public int getTbManualReview() {
            return tbManualReview;
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

        public void mergeFrom(SyncResult other) {
            if (other == null) {
                return;
            }
            this.apiTotal += other.apiTotal;
            this.apiSuccess += other.apiSuccess;
            this.apiFailed += other.apiFailed;
            this.tbTotal += other.tbTotal;
            this.tbInserted += other.tbInserted;
            this.tbUpdated += other.tbUpdated;
            this.tbFailed += other.tbFailed;
            this.tbManualReview += other.tbManualReview;
            this.tbSkippedDuplicate += other.tbSkippedDuplicate;
            this.tbSkippedNoMobile += other.tbSkippedNoMobile;
            this.empCalls += other.empCalls;
            this.mobileOverwritten += other.mobileOverwritten;
        }
    }

    private static class ManualReviewRequiredException extends RuntimeException {
        ManualReviewRequiredException(String message) {
            super(message);
        }
    }
}
