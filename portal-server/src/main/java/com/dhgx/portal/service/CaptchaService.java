package com.dhgx.portal.service;

import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.common.exception.LocalizedBusinessException;
import com.dhgx.portal.config.PortalProperties;
import com.dhgx.portal.dto.CaptchaImageResponse;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * CaptchaService。
 */
@Service
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "PORTAL:CAPTCHA:";
    private static final String CAPTCHA_DATA_PREFIX = "data:image/png;base64,";
    private static final String DEFAULT_CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";

    private final StringRedisTemplate stringRedisTemplate;
    private final PortalProperties portalProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CaptchaService(StringRedisTemplate stringRedisTemplate,
                          PortalProperties portalProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.portalProperties = portalProperties;
    }

    public CaptchaImageResponse generateCaptcha() {
        PortalProperties.Security.Captcha captcha = portalProperties.getSecurity().getCaptcha();
        String secret = captcha.getHashSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("captcha hashSecret 未配置");
        }
        int length = captcha.getLength() <= 0 ? 4 : captcha.getLength();
        String code = randomCode(length);
        System.out.println("图形验证码 ： "+code);
        String normalized = normalizeCode(code, captcha.isCaseInsensitive());
        String captchaId = UUID.randomUUID().toString();
        String hash = sha256(secret + ":" + normalized);
        long ttlSeconds = captcha.getTtlSeconds();
        stringRedisTemplate.opsForValue()
                .set(buildCaptchaKey(captchaId), hash, ttlSeconds, TimeUnit.SECONDS);
        String imageBase64 = CAPTCHA_DATA_PREFIX + imageToBase64(createImage(code));
        return new CaptchaImageResponse(captchaId, ttlSeconds, imageBase64);
    }

    public void verifyCaptcha(String captchaId, String captchaCode) {
        PortalProperties.Security.Captcha captcha = portalProperties.getSecurity().getCaptcha();
        String secret = captcha.getHashSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("captcha hashSecret 未配置");
        }
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new LocalizedBusinessException(ErrorCode.CAPTCHA_REQUIRED,
                    "error.captcha.required",
                    ErrorCode.CAPTCHA_REQUIRED.getMessage());
        }
        String key = buildCaptchaKey(captchaId);
        String storedHash = stringRedisTemplate.opsForValue().get(key);
        try {
            if (!StringUtils.hasText(storedHash)) {
                throw new LocalizedBusinessException(ErrorCode.CAPTCHA_INVALID,
                        "error.captcha.invalid",
                        ErrorCode.CAPTCHA_INVALID.getMessage());
            }
            String normalized = normalizeCode(captchaCode, captcha.isCaseInsensitive());
            String expected = sha256(secret + ":" + normalized);
            if (!storedHash.equals(expected)) {
                throw new LocalizedBusinessException(ErrorCode.CAPTCHA_INVALID,
                        "error.captcha.invalid",
                        ErrorCode.CAPTCHA_INVALID.getMessage());
            }
        } finally {
            stringRedisTemplate.delete(key);
        }
    }

    private String buildCaptchaKey(String captchaId) {
        return CAPTCHA_PREFIX + captchaId;
    }

    private String randomCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        int size = DEFAULT_CHARS.length();
        for (int i = 0; i < length; i++) {
            builder.append(DEFAULT_CHARS.charAt(secureRandom.nextInt(size)));
        }
        return builder.toString();
    }

    private BufferedImage createImage(String text) {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", "no");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.textproducer.font.size", "36");
        properties.setProperty("kaptcha.image.width", "140");
        properties.setProperty("kaptcha.image.height", "48");
        properties.setProperty("kaptcha.background.clear.from", "white");
        properties.setProperty("kaptcha.background.clear.to", "white");
        kaptcha.setConfig(new Config(properties));
        return kaptcha.createImage(text);
    }

    private String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("生成验证码图片失败", ex);
        }
    }

    private String normalizeCode(String code, boolean caseInsensitive) {
        String trimmed = code == null ? "" : code.trim();
        return caseInsensitive ? trimmed.toUpperCase(Locale.ROOT) : trimmed;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("计算验证码摘要失败", ex);
        }
    }
}
