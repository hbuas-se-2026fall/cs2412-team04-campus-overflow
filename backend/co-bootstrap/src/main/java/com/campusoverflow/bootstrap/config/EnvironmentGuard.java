package com.campusoverflow.bootstrap.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 生产启动前的配置自检：prod profile 下禁止 demo、禁止默认密钥/默认数据库密码。
 *
 * <p>这是 SEC-03/SEC-04 的兜底：即使 compose 或部署脚本漏配，应用也会拒绝启动，
 * 而不是带着仓库里的已知密钥/演示管理员跑在公网上。</p>
 */
@Component
public class EnvironmentGuard implements ApplicationRunner {

    private static final String DEV_FIELD_KEY = "GB7LwwDgcWpslCMsYtIkTbDrzER+IbjJwhtSu7LPz1I=";
    private static final String DEV_DB_PASSWORD = "campus_dev_pwd";

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
        String dbPassword = env.getProperty("spring.datasource.password", "");
        if (dbPassword.isBlank() || DEV_DB_PASSWORD.equals(dbPassword)) {
            throw new IllegalStateException("生产环境必须通过 DB_PASSWORD 注入非默认数据库密码");
        }
    }
}
