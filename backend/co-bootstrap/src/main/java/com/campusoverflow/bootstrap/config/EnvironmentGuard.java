package com.campusoverflow.bootstrap.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 生产启动前的配置自检：prod profile 下禁止 demo、禁止默认密钥/默认数据库密码。
 *
 * <p>这是 SEC-03/SEC-04 的兜底：即使 compose 或部署脚本漏配，应用也会拒绝启动，
 * 而不是带着仓库里的已知密钥/演示管理员跑在公网上。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnvironmentGuard implements ApplicationRunner {

    private static final String DEV_FIELD_KEY = "GB7LwwDgcWpslCMsYtIkTbDrzER+IbjJwhtSu7LPz1I=";
    private static final String DEV_DB_PASSWORD = "campus_dev_pwd";
    /** application-dev.yml 里公开的 HMAC 密钥；审计 2-15：原先漏拦，生产注入它等于用仓库里的公开密钥 */
    private static final String DEV_MAC_KEY = "ZGV2LW1hYy1rZXktMzItYnl0ZXMtZm9yLWRldi1vayE=";

    private final Environment env;

    public EnvironmentGuard(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> profiles = Arrays.asList(env.getActiveProfiles());
        if (!profiles.contains("prod")) {
            return;
        }
        if (profiles.contains("demo")) {
            throw new IllegalStateException("生产环境禁止启用 demo profile（DemoDataSeeder 会创建已知密码的管理员）");
        }
        String fieldKey = env.getProperty("co.security.field-key", "");
        String macKey = env.getProperty("co.security.field-mac-key", "");
        if (fieldKey.isBlank() || DEV_FIELD_KEY.equals(fieldKey)) {
            throw new IllegalStateException("生产环境必须通过 CO_FIELD_KEY 注入新的字段加密密钥");
        }
        if (macKey.isBlank()) {
            throw new IllegalStateException("生产环境必须通过 CO_FIELD_MAC_KEY 注入独立的 HMAC 密钥");
        }
        // 审计 2-15：与加密密钥同值、或直接沿用仓库里公开的 dev MAC 密钥，均视为未配置。
        if (DEV_MAC_KEY.equals(macKey)) {
            throw new IllegalStateException("生产环境不能使用 application-dev.yml 中的公开 HMAC 密钥");
        }
        if (macKey.equals(fieldKey)) {
            throw new IllegalStateException("CO_FIELD_MAC_KEY 必须与 CO_FIELD_KEY 不同（AES 与 HMAC 不得共用同一密钥）");
        }
        String dbPassword = env.getProperty("spring.datasource.password", "");
        if (dbPassword.isBlank() || DEV_DB_PASSWORD.equals(dbPassword)) {
            throw new IllegalStateException("生产环境必须通过 DB_PASSWORD 注入非默认数据库密码");
        }
    }
}
