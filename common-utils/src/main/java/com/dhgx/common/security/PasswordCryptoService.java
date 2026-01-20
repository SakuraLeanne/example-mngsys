package com.dhgx.common.security;

import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * PasswordCryptoService。
 * <p>
 * 负责解密前端传输的加密密码，当前使用 AES/GCM/NoPadding，密钥由配置提供。
 * 密文格式：Base64(12字节IV + 密文)。
 * </p>
 */
public class PasswordCryptoService {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final PasswordEncryptProperties passwordEncryptProperties;

    public PasswordCryptoService(PasswordEncryptProperties passwordEncryptProperties) {
        this.passwordEncryptProperties = passwordEncryptProperties;
    }

    /**
     * 解密密码密文，若未开启加密校验则回退为明文。
     *
     * @param encryptedPassword 密文（Base64）
     * @param plainPassword     明文
     * @return 解密后的明文密码
     */
    public String decrypt(String encryptedPassword, String plainPassword) {
        boolean enabled = passwordEncryptProperties != null && passwordEncryptProperties.isEnabled();
        if (!enabled) {
            return plainPassword;
        }
        if (!StringUtils.hasText(encryptedPassword)) {
            throw new IllegalArgumentException("密码密文不能为空");
        }
        byte[] decoded = Base64.getDecoder().decode(encryptedPassword);
        if (decoded.length <= IV_LENGTH) {
            throw new IllegalArgumentException("密码密文格式不正确");
        }
        byte[] iv = new byte[IV_LENGTH];
        byte[] cipherText = new byte[decoded.length - IV_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(decoded);
        buffer.get(iv);
        buffer.get(cipherText);
        SecretKey key = buildSecretKey(passwordEncryptProperties.getAesKey());
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("密码解密失败", ex);
        }
    }

    private SecretKey buildSecretKey(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalStateException("AES 密钥未配置");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("初始化密码密钥失败", ex);
        }
    }
}
