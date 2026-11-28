package com.campusoverflow.discovery.domain.notification;

import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.util.Optional;

public interface NotificationRepository {
    boolean exists(String sourceEventId, long recipientId);

    Notification save(Notification notification);

    Optional<Notification> findById(long id);

    PageResult<Notification> findByRecipient(long recipientId, PageRequest page);

    long countUnread(long recipientId);

    int markAllRead(long recipientId);

    /** 问题删除后清理指向该问题的通知，避免保留已删标题和 404 链接。 */
    int deleteByQuestionId(long questionId);
}
