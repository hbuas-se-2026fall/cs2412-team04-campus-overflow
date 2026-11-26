package com.campusoverflow.shared.page;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageRequestTest {

    @Test
    void clampsPageAndSizeAndComputesOffsetAsLong() {
        assertThat(new PageRequest(0, 100).page()).isEqualTo(1);
        assertThat(new PageRequest(0, 100).size()).isEqualTo(50);
        assertThat(new PageRequest(1, 50).offset()).isZero();
        PageRequest huge = new PageRequest(Integer.MAX_VALUE, 50);
        assertThat(huge.page()).isEqualTo(PageRequest.MAX_PAGE);
        assertThat(huge.offset()).isEqualTo((PageRequest.MAX_PAGE - 1L) * 50);
        assertThat(huge.offset()).isPositive();
    }
}
