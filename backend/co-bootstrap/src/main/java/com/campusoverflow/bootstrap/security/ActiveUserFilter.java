package com.campusoverflow.bootstrap.security;

import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.ActorAware;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 每个已认证请求都重新检查账号是否仍可登录。
 *
 * <p>SEC-01/02 的纵深防御：即使会话索引因历史大小写/迁移问题没有撤销成功，
 * 被禁用账号也不能继续访问任何接口（不只是 /auth/me）。管理员操作与公开接口
 * 不受影响（匿名请求直接放行）。</p>
 */
@Component
public class ActiveUserFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public ActiveUserFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof ActorAware aware) {
            Actor actor = aware.toActor();
            boolean active = users.findById(actor.userId()).map(u -> u.canLogin()).orElse(false);
            if (!active) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Unauthorized\","
                        + "\"status\":401,\"detail\":\"账号已被禁用\",\"code\":\"AUTH-401\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
