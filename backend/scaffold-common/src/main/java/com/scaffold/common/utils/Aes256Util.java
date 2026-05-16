package com.scaffold.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-CBC 可逆加密小工具（M-8 数据源密码 / 第三方 secret 等"用时还原"场景）。
 *
 * <p>设计点：
 * <ul>
 *   <li>密钥来源：调用方传入 master key（可从 application.yml 注入，或环境变量）。
 *       密钥长度任意 — 内部用 SHA-256 派生成 32 字节，再走 AES-256/CBC/PKCS5。</li>
 *   <li>每次加密随机 16 字节 IV，与密文一起 base64 拼接（IV ‖ ciphertext），自带 IV 不需要单独存。</li>
 *   <li>密文带 magic 前缀 {@code "ENC("} 与后缀 {@code ")"}，便于在 DB 一眼区分明文 / 密文，
 *       同时支持"已加密的字段不再加密"的幂等判断（{@link #isEncrypted(String)}）。</li>
 *   <li>不做密钥轮转 — 项目级密钥推荐通过 K8s Secret / 启动注入。</li>
 * </ul>
 *
 * <p>典型用法：
 * <pre>
 *   String enc = Aes256Util.encrypt(masterKey, "myDbPassword");
 *   // 入库 enc → "ENC(base64...)"
 *   String plain = Aes256Util.decrypt(masterKey, enc);
 * </pre>
 *
 * @author scaffold
 */
public final class Aes256Util
{
    private static final String ALGO = "AES/CBC/PKCS5Padding";
    private static final int IV_LEN = 16;
    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";

    private Aes256Util() {}

    /**
     * 判断字符串是否是本工具加密产物。返 {@code true} 时调用方应跳过二次加密（幂等保护）。
     */
    public static boolean isEncrypted(String s)
    {
        return s != null && s.startsWith(PREFIX) && s.endsWith(SUFFIX);
    }

    /**
     * 加密。
     *
     * @param masterKey 主密钥（任意长度，内部 SHA-256 派生 32 字节）
     * @param plain 明文（{@code null} → 返 {@code null}；空串 → 返空串）
     * @return 形如 {@code ENC(base64...)} 的密文
     */
    public static String encrypt(String masterKey, String plain)
    {
        if (plain == null) return null;
        if (plain.isEmpty()) return "";
        if (isEncrypted(plain)) return plain;
        try
        {
            byte[] key = derive(masterKey);
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_LEN + enc.length];
            System.arraycopy(iv, 0, combined, 0, IV_LEN);
            System.arraycopy(enc, 0, combined, IV_LEN, enc.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined) + SUFFIX;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("AES 加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密。明文（即不是 {@code ENC(...)} 前缀）原样返回，方便兼容历史明文记录。
     */
    public static String decrypt(String masterKey, String input)
    {
        if (input == null || input.isEmpty()) return input;
        if (!isEncrypted(input)) return input;
        try
        {
            byte[] key = derive(masterKey);
            String body = input.substring(PREFIX.length(), input.length() - SUFFIX.length());
            byte[] combined = Base64.getDecoder().decode(body);
            if (combined.length <= IV_LEN)
            {
                throw new IllegalArgumentException("ciphertext too short");
            }
            byte[] iv = new byte[IV_LEN];
            byte[] ciphertext = new byte[combined.length - IV_LEN];
            System.arraycopy(combined, 0, iv, 0, IV_LEN);
            System.arraycopy(combined, IV_LEN, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("AES 解密失败: " + e.getMessage(), e);
        }
    }

    /** SHA-256 派生 32 字节 key。 */
    private static byte[] derive(String masterKey)
    {
        if (masterKey == null || masterKey.isEmpty())
        {
            throw new IllegalArgumentException("masterKey 不能为空");
        }
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(masterKey.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("SHA-256 unavailable: " + e.getMessage(), e);
        }
    }
}
