package com.campusoverflow.identity.infrastructure.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 个人信息字段加密（AES-256-GCM）与可检索摘要（HMAC-SHA256）。
 * AES 密钥来自 CO_FIELD_KEY；HMAC 使用独立的 CO_FIELD_MAC_KEY（缺省用 HKDF 从主密钥派生），
 * 避免同一把密钥同时承担加密与 MAC。生产密钥必须由环境注入，切勿提交到代码仓库。
 */
@Component
public class FieldEncryptor {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec aesKey;
    private final SecretKeySpec macKey;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptor(@Value("${co.security.field-key}") String base64Key,
                          @Value("${co.security.field-mac-key:}") String base64MacKey) {
        byte[] key = Base64.getDecoder().decode(base64Key);
        if (key.length != 32) {
            throw new IllegalStateException("co.security.field-key 必须是 Base64 编码的 32 字节密钥");
        }
        byte[] mac = base64MacKey == null || base64MacKey.isBlank()
                ? hkdf(key, "campus-overflow/field-mac")
                : Base64.getDecoder().decode(base64MacKey);
        if (mac.length != 32) {
            throw new IllegalStateException("co.security.field-mac-key 必须是 Base64 编码的 32 字节密钥");
        }
        if (Arrays.equals(key, mac)) {
            throw new IllegalStateException("字段加密 AES 密钥与 HMAC 密钥不能相同");
        }
        this.aesKey = new SecretKeySpec(key, "AES");
        this.macKey = new SecretKeySpec(mac, "HmacSHA256");
    }

    /** HKDF-SHA256（extract + expand）：从主密钥派生 HMAC 子密钥，避免 AES 与 HMAC 共用同一把 key。 */
    private static byte[] hkdf(byte[] ikm, String info) {
        try {
            Mac extract = Mac.getInstance("HmacSHA256");
            extract.init(new SecretKeySpec(new byte[32], "HmacSHA256"));
            byte[] prk = extract.doFinal(ikm);
            Mac expand = Mac.getInstance("HmacSHA256");
            expand.init(new SecretKeySpec(prk, "HmacSHA256"));
            expand.update(info.getBytes(StandardCharsets.UTF_8));
            expand.update((byte) 1);
            return Arrays.copyOf(expand.doFinal(), 32);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC 密钥派生失败", e);
        }
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("字段加密失败", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            return new String(cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("字段解密失败（密钥是否被更换？）", e);
        }
    }

    /** 确定性摘要，用于唯一性约束与精确查找，不可逆。 */
    public String digest(String plain) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(macKey);
            return HexFormat.of().formatHex(mac.doFinal(plain.toLowerCase().getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("摘要计算失败", e);
        }
    }
}
