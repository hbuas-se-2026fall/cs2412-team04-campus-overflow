package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusoverflow.identity.domain.CourseMember;
import com.campusoverflow.identity.domain.CourseRepository;
import com.campusoverflow.identity.domain.CourseRole;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.identity.domain.UserStatus;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@code canManageCourse} 的权限口径（审计 W-06）。 */
class IdentityApiImplPermissionTest {

    private static final long TEACHER_ID = 2L;
    private static final long ADMIN_ID = 3L;
    private static final long STUDENT_ID = 4L;
    private static final long COURSE_ID = 100L;
    private static final Instant NOW = Instant.parse("2026-11-25T00:00:00Z");

    private UserRepository users;
    private CourseRepository courses;
    private IdentityApiImpl api;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        courses = mock(CourseRepository.class);
        api = new IdentityApiImpl(users, courses);

        when(users.findById(TEACHER_ID)).thenReturn(Optional.of(user(TEACHER_ID, Role.TEACHER)));
        when(users.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, Role.ADMIN)));
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(user(STUDENT_ID, Role.STUDENT)));
        when(courses.findMember(COURSE_ID, TEACHER_ID))
                .thenReturn(Optional.of(CourseMember.teacher(COURSE_ID, TEACHER_ID, NOW)));
        when(courses.findMember(COURSE_ID, STUDENT_ID))
                .thenReturn(Optional.of(CourseMember.student(COURSE_ID, STUDENT_ID, NOW)));
    }

    private static User user(long id, Role role) {
        return User.reconstitute(id, "u" + id, "用户" + id, "u" + id + "@campus.edu", "h", role,
                UserStatus.ACTIVE, true, null, null, NOW, 0);
    }

    @Test
    @DisplayName("公开问题（courseId=null）：全局 TEACHER 不再自动可管理（W-06 修复点）")
    void teacherCannotManagePublicQuestion() {
        assertThat(api.canManageCourse(TEACHER_ID, null)).isFalse();
    }

    @Test
    @DisplayName("公开问题（courseId=null）：ADMIN 仍可管理")
    void adminCanManagePublicQuestion() {
        assertThat(api.canManageCourse(ADMIN_ID, null)).isTrue();
    }

    @Test
    @DisplayName("课程问题：课程内 TEACHER/TA 可管理，学生不可")
    void courseQuestionFollowsCourseRole() {
        assertThat(api.canManageCourse(TEACHER_ID, COURSE_ID)).isTrue();
        assertThat(api.canManageCourse(STUDENT_ID, COURSE_ID)).isFalse();
    }

    @Test
    @DisplayName("非成员：不可管理该课程的问题")
    void nonMemberCannotManageCourseQuestion() {
        assertThat(api.canManageCourse(STUDENT_ID, 999L)).isFalse();
    }
}
