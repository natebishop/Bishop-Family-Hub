package com.familyhub.demo.service;

import com.familyhub.demo.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceRuleValidatorTest {

    private final RecurrenceRuleValidator validator = new RecurrenceRuleValidator();

    @Test
    void validDaily_passes() {
        assertThatCode(() -> validator.validate("FREQ=DAILY"))
                .doesNotThrowAnyException();
    }

    @Test
    void validWeekly_passes() {
        assertThatCode(() -> validator.validate("FREQ=WEEKLY;BYDAY=TU,TH"))
                .doesNotThrowAnyException();
    }

    @Test
    void validMonthly_passes() {
        assertThatCode(() -> validator.validate("FREQ=MONTHLY;BYDAY=1SU"))
                .doesNotThrowAnyException();
    }

    @Test
    void yearly_rejected() {
        assertThatThrownBy(() -> validator.validate("FREQ=YEARLY"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("YEARLY");
    }

    @Test
    void count_rejected() {
        assertThatThrownBy(() -> validator.validate("FREQ=WEEKLY;COUNT=10"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COUNT");
    }

    @Test
    void bysetpos_rejected() {
        assertThatThrownBy(() -> validator.validate("FREQ=MONTHLY;BYSETPOS=1;BYDAY=MO"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("BYSETPOS");
    }

    @Test
    void invalidSyntax_rejected() {
        assertThatThrownBy(() -> validator.validate("NOT_A_RULE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid recurrence rule");
    }

    @Test
    void emptyString_rejected() {
        assertThatThrownBy(() -> validator.validate(""))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void nullString_rejected() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be empty");
    }
}
