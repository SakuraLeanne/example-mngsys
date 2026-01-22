package com.dhgx.common.cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

public class Sm4CbcUtil {
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private Sm4CbcUtil() {}

    /**
     * 加密：返回 "ivBase64:cipherBase64"
     */
    public static String encryptToCombined(String plaintext, String keyBase64) {
        byte[] key = Base64.getDecoder().decode(keyBase64);
        if (key.length != 16) {
            throw new IllegalArgumentException("SM4 key must be 16 bytes");
        }

        byte[] iv = SecureRandoms.randomBytes(16);

        byte[] cipherBytes = encrypt(plaintext.getBytes(StandardCharsets.UTF_8), key, iv);

        String ivB64 = Base64.getEncoder().encodeToString(iv);
        String cipherB64 = Base64.getEncoder().encodeToString(cipherBytes);
        return ivB64 + ":" + cipherB64;
    }

    /**
     * 解密：入参 "ivBase64:cipherBase64"
     */
    public static String decryptFromCombined(String combined, String keyBase64) {
        String[] parts = combined.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid combined format: ivBase64:cipherBase64");
        }

        byte[] key = Base64.getDecoder().decode(keyBase64);
        if (key.length != 16) {
            throw new IllegalArgumentException("SM4 key must be 16 bytes");
        }

        byte[] iv = Base64.getDecoder().decode(parts[0]);
        if (iv.length != 16) {
            throw new IllegalArgumentException("SM4 iv must be 16 bytes");
        }

        byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);
        byte[] plainBytes = decrypt(cipherBytes, key, iv);

        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * 原始加密：SM4/CBC/PKCS5Padding（=PKCS7）
     */
    public static byte[] encrypt(byte[] plaintext, byte[] key16, byte[] iv16) {
        try {
            Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS5Padding", BouncyCastleProvider.PROVIDER_NAME);
            SecretKeySpec keySpec = new SecretKeySpec(key16, "SM4");
            IvParameterSpec ivSpec = new IvParameterSpec(iv16);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new RuntimeException("SM4 CBC encrypt failed", e);
        }
    }

    /**
     * 原始解密：SM4/CBC/PKCS5Padding（=PKCS7）
     */
    public static byte[] decrypt(byte[] cipherText, byte[] key16, byte[] iv16) {
        try {
            Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS5Padding", BouncyCastleProvider.PROVIDER_NAME);
            SecretKeySpec keySpec = new SecretKeySpec(key16, "SM4");
            IvParameterSpec ivSpec = new IvParameterSpec(iv16);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("SM4 CBC decrypt failed", e);
        }
    }

    /**
     * 生成 16字节Key（Base64）
     */
    public static String generateKeyBase64() {
        return Base64.getEncoder().encodeToString(SecureRandoms.randomBytes(16));
    }

    /**
     * 随机数工具（单独类也可以）
     */
    static final class SecureRandoms {
        private static final java.security.SecureRandom RND = new java.security.SecureRandom();
        static byte[] randomBytes(int len) {
            byte[] b = new byte[len];
            RND.nextBytes(b);
            return b;
        }
    }

    public static String encodeBase64(String val) {
        byte[] data = val.getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.getEncoder().encodeToString(data);
        return base64;
    }

    public static String decodeBase64(String val) {
        byte[] decode = Base64.getDecoder().decode(val);
        String result = new String(decode, StandardCharsets.UTF_8);
        return result;
    }

    public static String getfirst8last8(String str) {
        String first8Chars = getFirst8Chars(str);
        String last8Chars = getLast8Chars(str);
        return first8Chars+last8Chars;
    }

    // 获取前8位
    private static String getFirst8Chars(String str) {
        if (str == null || str.length() < 8) {
            return str;  // 返回原字符串或根据需求处理
        }
        return str.substring(0, 8);
    }

    // 获取后8位
    private static String getLast8Chars(String str) {
        if (str == null || str.length() < 8) {
            return str;  // 返回原字符串或根据需求处理
        }
        return str.substring(str.length() - 8);
    }
}
