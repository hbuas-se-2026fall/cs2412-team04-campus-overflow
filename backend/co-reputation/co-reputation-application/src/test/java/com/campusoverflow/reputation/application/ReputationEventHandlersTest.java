package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 重放同一事件只能记一次账。
 * <p>原来这一条用 Mockito 断言 {@code ledger.append} 被调用一次，但它验证的是「处理器自己写账本」的实现细节；
 * 现在处理器把记账统一交给 {@link ReputationLedgerService}（账本唯一键 (eventId, userId, reason) 去重），
 * 所以改成真实服务 + 内存仓储，断言落在可观察结果上：声誉值、流水条数。</p>
 */
class ReputationEventHandlersTest {

    private final ReputationTestFixtures fx = new ReputationTestFixtures();

    @Test
    @DisplayName("同一条采纳事件投两次：账本只有一条，声誉只加一次")
    void repeatedAnswerAcceptedEventShouldOnlyApplyOnce() {
        fx.register(3L, Role.STUDENT);
        AnswerAcceptedEvent event = fx.accept(101L, 3L, 1L);

        fx.handlers.on(event);
        fx.handlers.on(event);
        fx.handlers.on(event);

        assertThat(fx.ledger.size(3L)).isEqualTo(1);
        assertThat(fx.ledger.countByUserAndReason(3L, ReputationReason.ANSWER_ACCEPTED)).isEqualTo(1);
        assertThat(fx.accounts.findByUserId(3L).orElseThrow().reputation()).isEqualTo(15);
    }
}
