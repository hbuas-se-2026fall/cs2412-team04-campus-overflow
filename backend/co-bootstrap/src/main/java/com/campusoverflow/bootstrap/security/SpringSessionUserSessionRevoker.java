package com.campusoverflow.bootstrap.security;

import com.campusoverflow.identity.application.UserSessionRevoker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * 用 Spring Session 的 principal-name 索引撤销某用户的全部 Redis 会话。
 * 登录时 AuthController 会写入 PRINCIPAL_NAME_INDEX_NAME，索引才可用。
 *
 * <p>会话仓库用 {@link ObjectProvider} 延迟解析：Spring Session 自动配置未生效时
 * （如集成测试 {@code CampusOverflowFlowIT} 显式排除 SessionAutoConfiguration、
 * 用 MockHttpSession 隔离会话），构造器不会因缺少 bean 而失败，{@code revokeAll} 退化为空操作。
 */
@Component
public class SpringSessionUserSessionRevoker implements UserSessionRevoker {

    private final ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessions;

    public SpringSessionUserSessionRevoker(ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessions) {
        this.sessions = sessions;
    }

    @Override
    public void revokeAll(String username) {
        FindByIndexNameSessionRepository<? extends Session> repository = sessions.getIfAvailable();
        if (repository == null) {
            return;
        }
        repository.findByPrincipalName(username).keySet().forEach(repository::deleteById);
    }
}
