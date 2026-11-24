package com.campusoverflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsernamesTest {

    @Test
    void normalizeTrimsAndLowercases() {
        assertThat(Usernames.normalize("  T2001 ")).isEqualTo("t2001");
        assertThat(Usernames.normalize("Admin01")).isEqualTo("admin01");
        assertThat(Usernames.normalize(null)).isNull();
    }
}
