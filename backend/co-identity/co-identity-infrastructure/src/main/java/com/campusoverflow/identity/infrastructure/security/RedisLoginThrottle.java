package com.campusoverflow.identity.infrastructure.security;

import com.campusoverflow.identity.application.LoginThrottle;
import com.campusoverflow.identity.domain.Usernames;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 固定窗口计数的登录限流：同一账号 1 分钟内失败 N 次即锁定到窗口结束。
 *
 * <p><b>原子性（审计 AUTH-03）</b>：早期实现是「先 INCR 再（count==1 时）EXPIRE」两步，
 * 进程在两步之间中断会留下<b>无 TTL 的计数 key</b>，使该账号永久锁定。
 * 现改为单条 Lua 脚本：INCR 与首次 PEXPIRE 在同一原子操作内完成。
 *
 * <p><b>损坏值容错</b>：计数 key 若被外部写入非数字值，{@code isBlocked} 不再抛
 * {@link NumberFormatException}（那会让登录接口 500），而是告警并按「未锁定」处理，
 * 同时删除脏 key 自愈。
 */
@Component
public class RedisLoginThrottle implements LoginThrottle {

    private static final Logger log = LoggerFactory.getLogger(RedisLoginThrottle.class);

    private static final Duration WINDOW = Duration.ofMinutes(1);

    /**
     * KEYS[1] = 计数 key，ARGV[1] = 窗口毫秒数。
     * 仅在计数为 1（窗口内首次失败）时设置 TTL，避免每次失败都续期导致"永久锁定"。
     */
    private static final RedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
                    + "if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                    + "return c",
            Long.class);

    private final StringRedisTemplate redis;
    private final int maxFailures;

    public RedisLoginThrottle(StringRedisTemplate redis, @Value("${co.security.login-max-failures:5}") int maxFailures) {
        this.redis = redis;
        this.maxFailures = maxFailures;
    }

    @Override
    public boolean isBlocked(String username) {
        String key = key(username);
        String v = redis.opsForValue().get(key);
        if (v == null) {
            return false;
        }
        try {
            return Integer.parseInt(v) >= maxFailures;
        } catch (NumberFormatException e) {
            log.warn("登录限流计数损坏，按未锁定处理并删除脏 key: key={}, value={}", key, v);
            redis.delete(key);
            return false;
        }
    }

    @Override
    public void recordFailure(String username) {
        redis.execute(INCR_WITH_TTL, List.of(key(username)), String.valueOf(WINDOW.toMillis()));
    }

    @Override
    public void reset(String username) {
        redis.delete(key(username));
    }

    private static String key(String username) {
        return "id:login-fail:" + Usernames.normalize(username);
    }
}
