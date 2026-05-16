package com.scaffold.common.core.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.scaffold.common.annotation.SensitiveLog;
import com.scaffold.common.annotation.SensitiveStrategy;

class SensitiveJsonSerializerTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mobileFieldIsMasked() throws Exception
    {
        Sample s = new Sample();
        s.phone = "13912345678";
        s.name = "张三";
        s.email = "[email protected]";
        s.idCard = "110101199001011234";
        s.password = "secret";
        s.address = "ShangHaiPudongDistrictTowerA";
        s.cardNumber = "6225888888888888";
        s.custom = "abcdefgh";

        String json = mapper.writeValueAsString(s);

        assertThat(json).contains("\"phone\":\"" + SensitiveStrategy.MOBILE.apply(s.phone, "*") + "\"");
        assertThat(json).contains("\"name\":\"" + SensitiveStrategy.CHINESE_NAME.apply(s.name, "*") + "\"");
        assertThat(json).contains("\"email\":\"" + SensitiveStrategy.EMAIL.apply(s.email, "*") + "\"");
        assertThat(json).contains("\"idCard\":\"" + SensitiveStrategy.ID_CARD.apply(s.idCard, "*") + "\"");
        assertThat(json).contains("\"password\":\"" + SensitiveStrategy.PASSWORD.apply(s.password, "*") + "\"");
        assertThat(json).contains("\"address\":\"" + SensitiveStrategy.ADDRESS.apply(s.address, "*") + "\"");
        assertThat(json).contains("\"cardNumber\":\"" + SensitiveStrategy.BANK_CARD.apply(s.cardNumber, "*") + "\"");
        assertThat(json).contains("\"custom\":\"ab****gh\"");
    }

    @Test
    void shortValueFallsBackToOriginal()
    {
        assertThat(SensitiveStrategy.MOBILE.apply("123", "*")).isEqualTo("123");
        assertThat(SensitiveStrategy.ID_CARD.apply("123", "*")).isEqualTo("123");
        assertThat(SensitiveStrategy.CHINESE_NAME.apply("Z", "*")).isEqualTo("Z");
    }

    static class Sample
    {
        @SensitiveLog(strategy = SensitiveStrategy.MOBILE)
        public String phone;
        @SensitiveLog(strategy = SensitiveStrategy.CHINESE_NAME)
        public String name;
        @SensitiveLog(strategy = SensitiveStrategy.EMAIL)
        public String email;
        @SensitiveLog(strategy = SensitiveStrategy.ID_CARD)
        public String idCard;
        @SensitiveLog(strategy = SensitiveStrategy.PASSWORD)
        public String password;
        @SensitiveLog(strategy = SensitiveStrategy.ADDRESS)
        public String address;
        @SensitiveLog(strategy = SensitiveStrategy.BANK_CARD)
        public String cardNumber;
        @SensitiveLog(strategy = SensitiveStrategy.CUSTOM, prefixKeep = 2, suffixKeep = 2)
        public String custom;
    }
}
