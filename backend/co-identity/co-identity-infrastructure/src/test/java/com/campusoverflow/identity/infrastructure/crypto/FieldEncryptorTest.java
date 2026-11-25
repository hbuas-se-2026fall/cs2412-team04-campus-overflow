package com.campusoverflow.identity.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class FieldEncryptorTest {

    private static final String AES = Base64.getEncoder().encodeToString("a".repeat(32).getBytes(StandardCharsets.UTF_8));
    private static final String MAC = Base64.getEncoder().encodeToString("b".repeat(32).getBytes(StandardCharsets.UTF_8));

    @Test
    void encryptDecryptAndDigestUseSeparateKeys() {
        FieldEncryptor enc = new FieldEncryptor(AES, MAC);
        String cipher = enc.encrypt("student@campus.edu");
        assertThat(cipher).isNotEqualTo("student@campus.edu");
        assertThat(enc.decrypt(cipher)).isEqualTo("student@campus.edu");
        assertThat(enc.digest("Student@Campus.edu")).isEqualTo(enc.digest("student@campus.edu"));

        FieldEncryptor otherMac = new FieldEncryptor(AES,
                Base64.getEncoder().encodeToString("c".repeat(32).getBytes(StandardCharsets.UTF_8)));
        assertThat(otherMac.digest("student@campus.edu")).isNotEqualTo(enc.digest("student@campus.edu"));
        assertThat(otherMac.decrypt(cipher)).isEqualTo("student@campus.edu");
    }
}
