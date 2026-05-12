package com.loopers.server.security;

import lombok.Value;

/** SecurityContext 에 담기는 인증 주체 */
@Value
public class UserPrincipal {
    int id;
    String loginId;
    String role;
}
