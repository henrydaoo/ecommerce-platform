package com.ecommerce.platform.exception;

                                                                                     
public class TokenExpiredOrRevokedException extends ApiException {
    public TokenExpiredOrRevokedException() {
        super(ErrorCode.TOKEN_EXPIRED_OR_REVOKED, "Refresh token is invalid, expired, or revoked");
    }
}
