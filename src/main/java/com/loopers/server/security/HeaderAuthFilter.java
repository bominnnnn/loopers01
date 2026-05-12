package com.loopers.server.security;

import com.loopers.server.model.User;
import com.loopers.server.repository.InMemoryUserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 커스텀 헤더 인증 필터
 * X-Loopers-LoginId / X-Loopers-LoginPw 헤더로 인증
 * JWT 토큰 인증(JwtAuthFilter)과 함께 사용 가능 — 둘 중 하나만 있어도 인증됨
 */
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    private final InMemoryUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public HeaderAuthFilter(InMemoryUserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 이미 JWT 필터에서 인증된 경우 건너뜀
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String loginId = request.getHeader("X-Loopers-LoginId");
        String password = request.getHeader("X-Loopers-LoginPw");

        if (loginId != null && password != null) {
            User user = userRepository.findByLoginId(loginId);
            if (user != null && passwordEncoder.matches(password, user.getPasswordHash())) {
                UserPrincipal principal = new UserPrincipal(user.getId(), user.getLoginId(), user.getRole());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
