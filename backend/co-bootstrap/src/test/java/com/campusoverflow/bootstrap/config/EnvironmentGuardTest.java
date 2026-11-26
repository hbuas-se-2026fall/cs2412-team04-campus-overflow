package com.campusoverflow.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** 生产启动自检：禁止默认密钥、公开的 dev HMAC 密钥、以及 AES/HMAC 共用同一把密钥（审计 2-15）。 */
class EnvironmentGuardTest {

    private static final String DEV_FIELD_KEY = "GB7LwwDgcWpslCMsYtIkTbDrzER+IbjJwhtSu7LPz1I=";
    private static final String DEV_MAC_KEY = "ZGV2LW1hYy1rZXktMzItYnl0ZXMtZm9yLWRldi1vayE=";
    private static final String OTHER_KEY = "ZmllbGQta2V5LTMyLWJ5dGVzLWZvci1wcm9kISE=";

    private EnvironmentGuard guard(String profile, String fieldKey, String macKey, String dbPassword) {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.profiles.active", profile)
                .withProperty("co.security.field-key", fieldKey)
                .withProperty("co.security.field-mac-key", macKey)
                .withProperty("spring.datasource.password", dbPassword);
        return new EnvironmentGuard(env);
    }

    @Test
    void nonProdProfileSkipsAllChecks() {
        assertThatCode(() -> guard("dev", "", "", "").run(null)).doesNotThrowAnyException();
    }

    @Test
    void prodAcceptsProperlyRotatedKeys() {
        assertThatCode(() -> guard("prod", OTHER_KEY, "bWFjLWtleS0zMi1ieXRlcy1mb3ItcHJvZCEhIQ==", "Str0ngPwd!")
                .run(null)).doesNotThrowAnyException();
    }

    @Test
    void prodRejectsDevFieldKey() {
        assertThatThrownBy(() -> guard("prod", DEV_FIELD_KEY, "bWFjLWtleS0zMi1ieXRlcy1mb3ItcHJvZCEhIQ==", "Str0ngPwd!")
                .run(null)).hasMessageContaining("CO_FIELD_KEY");
    }

    @Test
    void prodRejectsPublicDevMacKey() {
        assertThatThrownBy(() -> guard("prod", OTHER_KEY, DEV_MAC_KEY, "Str0ngPwd!")
                .run(null)).hasMessageContaining("公开 HMAC 密钥");
    }

    @Test
    void prodRejectsSameKeyForAesAndHmac() {
        assertThatThrownBy(() -> guard("prod", OTHER_KEY, OTHER_KEY, "Str0ngPwd!")
                .run(null)).hasMessageContaining("必须与 CO_FIELD_KEY 不同");
    }

    @Test
    void prodRejectsDemoProfile() {
        assertThatThrownBy(() -> guard("prod,demo", OTHER_KEY, "bWFjLWtleS0zMi1ieXRlcy1mb3ItcHJvZCEhIQ==", "Str0ngPwd!")
                .run(null)).hasMessageContaining("demo");
    }

    @Test
    void prodRejectsDefaultDbPassword() {
        assertThatThrownBy(() -> guard("prod", OTHER_KEY, "bWFjLWtleS0zMi1ieXRlcy1mb3ItcHJvZCEhIQ==", "campus_dev_pwd")
                .run(null)).hasMessageContaining("DB_PASSWORD");
    }
}
