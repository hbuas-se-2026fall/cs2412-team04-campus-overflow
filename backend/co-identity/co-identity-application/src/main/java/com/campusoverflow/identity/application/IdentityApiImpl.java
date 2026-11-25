package com.campusoverflow.identity.application;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.identity.domain.CourseRepository;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.security.Role;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 开放主机服务的实现：把领域对象翻译为发布语言（UserSummary / CourseSummary）。 */
@Service
@Transactional(readOnly = true)
public class IdentityApiImpl implements IdentityApi {

    private final UserRepository users;
    private final CourseRepository courses;

    public IdentityApiImpl(UserRepository users, CourseRepository courses) {
        this.users = users;
        this.courses = courses;
    }

    @Override
    public Optional<UserSummary> findUser(long userId) {
        return users.findById(userId).map(IdentityApiImpl::toSummary);
    }

    @Override
    public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(userIds.stream().distinct().toList()).stream()
                .map(IdentityApiImpl::toSummary)
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }

    @Override
    public Optional<UserSummary> findByDisplayName(String displayName) {
        return users.findByDisplayName(displayName).map(IdentityApiImpl::toSummary);
    }

    @Override
    public Optional<CourseSummary> findCourse(long courseId) {
        return courses.findById(courseId)
                .map(c -> new CourseSummary(c.id(), c.code(), c.name(), c.term(), c.teacherId()));
    }

    @Override
    public boolean canManageCourse(long userId, Long courseId) {
        Optional<User> user = users.findById(userId);
        if (user.isEmpty()) {
            return false;
        }
        Role role = user.get().role();
        if (role == Role.ADMIN) {
            return true;
        }
        if (courseId == null) {
            // PO 决策（2026-11-25，审计 W-06）：公开问题（未关联课程）没有课程归属，
            // 全局 TEACHER 不再自动获得管理权（否则任何教师都能编辑/删除他人的公开问题、
            // 替他人采纳或认证）。公开问题仅：作者本人（由各聚合的 author 分支放行）或 ADMIN。
            return false;
        }
        return courses.findMember(courseId, userId).map(m -> m.role().canManage()).orElse(false);
    }

    @Override
    public boolean isCourseMember(long userId, long courseId) {
        return courses.findMember(courseId, userId).isPresent();
    }

    @Override
    public Set<Long> courseIdsOfUser(long userId) {
        return courses.findMembershipsOfUser(userId).stream()
                .map(m -> m.courseId())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static UserSummary toSummary(User u) {
        return new UserSummary(u.id(), u.displayName(), u.role(), u.verified(), u.college());
    }
}
