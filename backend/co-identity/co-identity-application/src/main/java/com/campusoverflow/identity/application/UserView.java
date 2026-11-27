package com.campusoverflow.identity.application;

import com.campusoverflow.identity.domain.UserStatus;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;

/** 管理员视图：学号/工号已脱敏。 */
public record UserView(long id, String maskedUsername, String displayName, Role role, boolean verified,
                       UserStatus status, String college, String className, Instant createdAt) {
}
