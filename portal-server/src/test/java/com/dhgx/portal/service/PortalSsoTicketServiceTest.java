package com.dhgx.portal.service;

import com.dhgx.common.portal.dto.PortalSsoTicketLoginResponse;
import com.dhgx.portal.client.AuthClient;
import com.dhgx.portal.common.SsoTicketUtils;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.entity.PortalUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class PortalSsoTicketServiceTest {

    private static RedisServer redisServer;
    private static int redisPort;

    private StringRedisTemplate stringRedisTemplate;
    private PortalUserService portalUserService;
    private PortalSsoTicketService portalSsoTicketService;
    private AuthClient authClient;

    @BeforeAll
    static void startRedis() throws Exception {
        redisPort = findAvailablePort();
        redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("bind 127.0.0.1")
                .setting("save \"\"")
                .build();
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("127.0.0.1", redisPort);
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        stringRedisTemplate = new StringRedisTemplate(connectionFactory);
        portalUserService = Mockito.mock(PortalUserService.class);
        authClient = Mockito.mock(AuthClient.class);
        portalSsoTicketService = new PortalSsoTicketService(stringRedisTemplate, portalUserService, authClient, new ObjectMapper());
    }

    @Test
    void shouldVerifyAndConsumeTicketOnce() {
        String ticket = "abc123ticketvalue";
        writeTicket(ticket, "u-1", "biz-a", "https://biz-a.example.com/sso/callback", "", "satoken-u1");
        PortalUser user = createUser("u-1");
        given(portalUserService.getById("u-1")).willReturn(user);

        PortalSsoTicketService.VerifyResult first =
                portalSsoTicketService.verifyAndConsume("biz-a", ticket, "https://biz-a.example.com/sso/callback");
        PortalSsoTicketService.VerifyResult second =
                portalSsoTicketService.verifyAndConsume("biz-a", ticket, "https://biz-a.example.com/sso/callback");

        assertThat(first.isSuccess()).isTrue();
        PortalSsoTicketLoginResponse response = first.getLoginResponse();
        assertThat(response.getGSessionId()).startsWith("G-");
        assertThat(response.getLogoutToken()).startsWith("L-");
        assertThat(response.getUserId()).isEqualTo("u-1");
        assertThat(second.isSuccess()).isFalse();
        assertThat(second.getErrorCode()).isEqualTo(ErrorCode.SSO_TICKET_INVALID);
    }

    @Test
    void shouldReturnInvalidWhenExpired() throws Exception {
        String ticket = "expiredticketvalue123";
        writeTicket(ticket, "u-1", "biz-a", "https://biz-a.example.com/sso/callback", "", "satoken-u1");
        stringRedisTemplate.expire(SsoTicketUtils.buildTicketKey(ticket), 1, TimeUnit.SECONDS);

        Thread.sleep(1200);

        PortalSsoTicketService.VerifyResult result =
                portalSsoTicketService.verifyAndConsume("biz-a", ticket, "https://biz-a.example.com/sso/callback");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.SSO_TICKET_INVALID);
    }

    @Test
    void shouldInvalidateOnClientMismatch() {
        String ticket = "clientmismatch12345";
        writeTicket(ticket, "u-1", "biz-a", "https://biz-a.example.com/sso/callback", "", "satoken-u1");

        PortalSsoTicketService.VerifyResult result =
                portalSsoTicketService.verifyAndConsume("biz-b", ticket, "https://biz-a.example.com/sso/callback");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.SSO_TICKET_CLIENT_MISMATCH);
        assertThat(stringRedisTemplate.hasKey(SsoTicketUtils.buildTicketKey(ticket))).isFalse();
    }

    @Test
    void shouldInvalidateOnRedirectMismatch() {
        String ticket = "redirectmismatch123";
        writeTicket(ticket, "u-1", "biz-a", "https://biz-a.example.com/sso/callback", "", "satoken-u1");

        PortalSsoTicketService.VerifyResult result =
                portalSsoTicketService.verifyAndConsume("biz-a", ticket, "https://biz-a.example.com/sso/other");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.SSO_TICKET_REDIRECT_URI_MISMATCH);
        assertThat(stringRedisTemplate.hasKey(SsoTicketUtils.buildTicketKey(ticket))).isFalse();
    }

    @Test
    void shouldReturnStateMismatchWithoutConsuming() {
        String ticket = "statemismatch12345";
        writeTicket(ticket, "u-1", "biz-a", "https://biz-a.example.com/sso/callback", "state-value", "satoken-u1");

        PortalSsoTicketService.VerifyResult result =
                portalSsoTicketService.verifyAndConsume("biz-a", ticket, "https://biz-a.example.com/sso/callback", "another-state");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.SSO_TICKET_STATE_MISMATCH);
        assertThat(stringRedisTemplate.hasKey(SsoTicketUtils.buildTicketKey(ticket))).isTrue();
    }


    @Test
    void shouldLogoutByGlobalSession() {
        String ticket = "logoutticket123456";
        writeTicket(ticket, "u-3", "biz-a", "https://biz-a.example.com/sso/callback", "", "satoken-u3");
        PortalUser user = createUser("u-3");
        given(portalUserService.getById("u-3")).willReturn(user);

        PortalSsoTicketService.VerifyResult verifyResult =
                portalSsoTicketService.verifyAndConsume("biz-a", ticket, "https://biz-a.example.com/sso/callback");

        assertThat(verifyResult.isSuccess()).isTrue();
        PortalSsoTicketLoginResponse loginResponse = verifyResult.getLoginResponse();

        PortalSsoTicketService.VerifyResult logoutResult = portalSsoTicketService.logoutByGlobalSession(
                "biz-a", loginResponse.getGSessionId(), loginResponse.getLogoutToken());

        assertThat(logoutResult.isSuccess()).isTrue();
        assertThat(stringRedisTemplate.hasKey("PORTAL:GSESSION:" + loginResponse.getGSessionId())).isFalse();
        assertThat(stringRedisTemplate.hasKey("PORTAL:LOGOUT_TOKEN:" + loginResponse.getGSessionId())).isFalse();
        Mockito.verify(authClient).logoutByTokenValue("satoken-u3");
    }


    @Test
    void shouldKeepMappingsWhenAuthLogoutFailed() {
        String ticket = "logoutfailed123456";
        writeTicket(ticket, "u-4", "biz-a", "https://biz-a.example.com/sso/callback", "", "satoken-u4");
        PortalUser user = createUser("u-4");
        given(portalUserService.getById("u-4")).willReturn(user);
        given(authClient.logoutByTokenValue("satoken-u4")).willReturn(ApiResponse.failure(ErrorCode.SSO_TICKET_SYSTEM_ERROR));

        PortalSsoTicketService.VerifyResult verifyResult =
                portalSsoTicketService.verifyAndConsume("biz-a", ticket, "https://biz-a.example.com/sso/callback");

        assertThat(verifyResult.isSuccess()).isTrue();
        PortalSsoTicketLoginResponse loginResponse = verifyResult.getLoginResponse();

        PortalSsoTicketService.VerifyResult logoutResult = portalSsoTicketService.logoutByGlobalSession(
                "biz-a", loginResponse.getGSessionId(), loginResponse.getLogoutToken());

        assertThat(logoutResult.isSuccess()).isFalse();
        assertThat(logoutResult.getErrorCode()).isEqualTo(ErrorCode.SSO_TICKET_SYSTEM_ERROR);
        assertThat(stringRedisTemplate.hasKey("PORTAL:GSESSION:" + loginResponse.getGSessionId())).isTrue();
        assertThat(stringRedisTemplate.hasKey("PORTAL:LOGOUT_TOKEN:" + loginResponse.getGSessionId())).isTrue();
        Mockito.verify(authClient).logoutByTokenValue("satoken-u4");
    }

    @Test
    void shouldAllowConcurrentVerifyOnlyOnce() throws Exception {
        String ticket = "concurrentticket12345";
        writeTicket(ticket, "u-2", "biz-a", "https://biz-a.example.com/sso/callback", "", "satoken-u2");
        PortalUser user = createUser("u-2");
        given(portalUserService.getById("u-2")).willReturn(user);

        int attempts = 10;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch latch = new CountDownLatch(attempts);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < attempts; i++) {
            executor.submit(() -> {
                try {
                    PortalSsoTicketService.VerifyResult result =
                            portalSsoTicketService.verifyAndConsume("biz-a", ticket, "https://biz-a.example.com/sso/callback");
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();
        assertThat(successCount.get()).isEqualTo(1);
    }

    private void writeTicket(String ticket, String userId, String systemCode, String redirectUri, String state, String tokenValue) {
        Map<String, String> fields = new HashMap<>();
        fields.put("userId", userId);
        fields.put("systemCode", systemCode);
        fields.put("issuedAt", String.valueOf(Instant.now().toEpochMilli()));
        fields.put("redirectUriHash", SsoTicketUtils.hashRedirectUri(redirectUri));
        fields.put("stateHash", SsoTicketUtils.hashValue(state));
        fields.put("tokenValue", tokenValue);
        String key = SsoTicketUtils.buildTicketKey(ticket);
        stringRedisTemplate.opsForHash().putAll(key, fields);
        stringRedisTemplate.expire(key, 60, TimeUnit.SECONDS);
    }

    private PortalUser createUser(String userId) {
        PortalUser user = new PortalUser();
        user.setId(userId);
        user.setUsername("demo");
        user.setMobile("13800000000");
        user.setRealName("Demo User");
        return user;
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
