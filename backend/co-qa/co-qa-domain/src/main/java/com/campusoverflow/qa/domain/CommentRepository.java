package com.campusoverflow.qa.domain;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Optional<Comment> findById(long id);

    /** 以行级锁读取评论，供“回复 / 删除父评论”串行化。 */
    Optional<Comment> findByIdForUpdate(long id);

    /** 某个顶层评论下的全部回复，按发布时间升序。 */
    List<Comment> findReplies(long parentId);

    Comment save(Comment comment);

    void delete(Comment comment);
}
