package com.scaffold.common.annotation;

/**
 * 常见脱敏策略集。
 *
 * @author scaffold
 */
public enum SensitiveStrategy
{
    /** 默认：保留首尾各 1 位 */
    DEFAULT
    {
        @Override
        public String apply(String value, String mask)
        {
            if (value.length() <= 2) return repeat(mask, value.length());
            return value.charAt(0) + repeat(mask, value.length() - 2) + value.charAt(value.length() - 1);
        }
    },
    /** 中文姓名：保留姓 */
    CHINESE_NAME
    {
        @Override
        public String apply(String value, String mask)
        {
            if (value.length() <= 1) return value;
            return value.charAt(0) + repeat(mask, value.length() - 1);
        }
    },
    /** 身份证：保留前 6 后 4 */
    ID_CARD
    {
        @Override
        public String apply(String value, String mask)
        {
            if (value.length() <= 10) return value;
            return value.substring(0, 6) + repeat(mask, value.length() - 10) + value.substring(value.length() - 4);
        }
    },
    /** 手机号：保留前 3 后 4 */
    MOBILE
    {
        @Override
        public String apply(String value, String mask)
        {
            if (value.length() <= 7) return value;
            return value.substring(0, 3) + repeat(mask, value.length() - 7) + value.substring(value.length() - 4);
        }
    },
    /** 固话 / 座机：保留前 3 后 4，与 MOBILE 一致 */
    FIXED_PHONE
    {
        @Override
        public String apply(String value, String mask)
        {
            return MOBILE.apply(value, mask);
        }
    },
    /** 邮箱：保留首字母与 @ 之后部分 */
    EMAIL
    {
        @Override
        public String apply(String value, String mask)
        {
            int idx = value.indexOf('@');
            if (idx <= 1) return value;
            return value.charAt(0) + repeat(mask, idx - 1) + value.substring(idx);
        }
    },
    /** 银行卡：保留前 6 后 4 */
    BANK_CARD
    {
        @Override
        public String apply(String value, String mask)
        {
            if (value.length() <= 10) return value;
            return value.substring(0, 6) + repeat(mask, value.length() - 10) + value.substring(value.length() - 4);
        }
    },
    /** 地址：保留前 6 字符 */
    ADDRESS
    {
        @Override
        public String apply(String value, String mask)
        {
            if (value.length() <= 6) return value;
            return value.substring(0, 6) + repeat(mask, value.length() - 6);
        }
    },
    /** 密码 / 私钥：全部脱敏 */
    PASSWORD
    {
        @Override
        public String apply(String value, String mask)
        {
            return repeat(mask, value.length());
        }
    },
    /** 自定义，由 {@link SensitiveLog#prefixKeep()} / {@link SensitiveLog#suffixKeep()} 控制 */
    CUSTOM
    {
        @Override
        public String apply(String value, String mask)
        {
            return value; // CUSTOM 实际由 SensitiveJsonSerializer 处理
        }
    };

    public abstract String apply(String value, String mask);

    public static String repeat(String s, int count)
    {
        if (count <= 0) return "";
        StringBuilder builder = new StringBuilder(count * s.length());
        for (int i = 0; i < count; i++) builder.append(s);
        return builder.toString();
    }
}
