package com.campusoverflow.identity.domain;

import java.util.Locale;

/**
 * 账号标识规范化。
 *
 * <p>数据库排序规则为 utf8mb4_0900_ai_ci（大小写不敏感），`T2001` 与 `t2001` 是同一账号。
 * 限流键、会话 principal-name 索引、禁用撤销都必须使用同一规范形式，否则大小写变体可以绕过
 * “同一账号失败次数限制”和“禁用后撤销会话”。</p>
 */
public final class Usernames {

    private Usernames() {
    }

    public static String normalize(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }
}
