package com.example.mngsys.common;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class Test {
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128; // bits

    public static void main(String[] args) throws Exception {
        String plain = "1qazxsw2#";
        String aesKey = "pPx7rQ2GdSfHc8zWYmVBj5kR9LN4uT1C";

        byte[] iv = randomIv();
        byte[] key = sha256(aesKey); // 与服务端一致：对 aesKey 做 SHA-256 得到 32 字节
        byte[] cipherText = encrypt(plain.getBytes(StandardCharsets.UTF_8), key, iv);

        // 拼接 iv + cipherText，然后 Base64
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
        buffer.put(iv);
        buffer.put(cipherText);
        String payloadB64 = Base64.getEncoder().encodeToString(buffer.array());

        System.out.println("Encrypted Password (Base64 iv+cipher+tag):");
        System.out.println(payloadB64);

        // 解密示例
        String decrypted = decryptFromBase64(payloadB64, aesKey);
        System.out.println("Decrypted Password:");
        System.out.println(decrypted);
    }

    private static byte[] encrypt(byte[] plain, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), spec);
        return cipher.doFinal(plain);
    }

    private static String decryptFromBase64(String payloadB64, String aesKey) throws Exception {
        byte[] payload = Base64.getDecoder().decode(payloadB64);
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);
        byte[] key = sha256(aesKey);
        byte[] plain = decrypt(cipherText, key, iv);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static byte[] decrypt(byte[] cipherText, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), spec);
        return cipher.doFinal(cipherText);
    }

    private static byte[] sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] randomIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
