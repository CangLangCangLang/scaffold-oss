package com.scaffold.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class Aes256UtilTest
{
    private static final String KEY = "scaffold-test-master-key-very-long-and-strong";

    @Test
    void encryptThenDecryptRoundtrip()
    {
        String plain = "myDbPassword!23#";
        String enc = Aes256Util.encrypt(KEY, plain);
        assertThat(enc).isNotEqualTo(plain);
        assertThat(Aes256Util.isEncrypted(enc)).isTrue();
        assertThat(Aes256Util.decrypt(KEY, enc)).isEqualTo(plain);
    }

    @Test
    void encryptIsRandomized()
    {
        String enc1 = Aes256Util.encrypt(KEY, "same-password");
        String enc2 = Aes256Util.encrypt(KEY, "same-password");
        assertThat(enc1).isNotEqualTo(enc2); // 因为 IV 随机
        assertThat(Aes256Util.decrypt(KEY, enc1)).isEqualTo("same-password");
        assertThat(Aes256Util.decrypt(KEY, enc2)).isEqualTo("same-password");
    }

    @Test
    void encryptIsIdempotentForAlreadyEncrypted()
    {
        String enc = Aes256Util.encrypt(KEY, "x");
        String again = Aes256Util.encrypt(KEY, enc);
        assertThat(again).isEqualTo(enc);
    }

    @Test
    void decryptPlainTextPassesThrough()
    {
        // 兼容历史明文：没 ENC(...) 前缀直接原样返回
        assertThat(Aes256Util.decrypt(KEY, "rawPassword")).isEqualTo("rawPassword");
    }

    @Test
    void nullAndEmptyAreSafe()
    {
        assertThat(Aes256Util.encrypt(KEY, null)).isNull();
        assertThat(Aes256Util.encrypt(KEY, "")).isEqualTo("");
        assertThat(Aes256Util.decrypt(KEY, null)).isNull();
        assertThat(Aes256Util.decrypt(KEY, "")).isEqualTo("");
    }

    @Test
    void wrongKeyThrows()
    {
        String enc = Aes256Util.encrypt(KEY, "x");
        assertThatThrownBy(() -> Aes256Util.decrypt("different-key", enc))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void emptyMasterKeyRejected()
    {
        assertThatThrownBy(() -> Aes256Util.encrypt("", "x"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> Aes256Util.encrypt(null, "x"))
                .isInstanceOf(IllegalStateException.class);
    }
}
