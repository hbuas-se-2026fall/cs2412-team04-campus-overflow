package com.campusoverflow.shared.domain;

/** 未认证/会话已失效（映射为 401）。 */
public class UnauthorizedException extends DomainException {
    public UnauthorizedException(String code, String message) {
        super(code, message);
    }

    @Override
    public int suggestedStatus() {
        return 401;
    }
}
