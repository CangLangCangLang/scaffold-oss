package com.scaffold.common.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BizCodeTest
{
    @Test
    void allCodesExposeStableErrorKey()
    {
        for (BizCode code : BizCode.values())
        {
            assertThat(code.errorKey()).startsWith("BIZ_");
            assertThat(code.defaultMessage()).isNotBlank();
            assertThat(code.httpStatus()).isPositive();
        }
    }

    @Test
    void rateLimitedReturns429()
    {
        assertThat(BizCode.RATE_LIMITED.httpStatus()).isEqualTo(429);
    }

    @Test
    void duplicateSubmitReturns409()
    {
        assertThat(BizCode.DUPLICATE_SUBMIT.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }
}
