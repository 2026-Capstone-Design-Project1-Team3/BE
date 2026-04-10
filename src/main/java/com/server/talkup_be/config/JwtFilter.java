package com.server.talkup_be.config;

import com.server.talkup_be.service.RedisBlacklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisBlacklistService redisBlacklistService;

    public JwtFilter(JwtProvider jwtProvider, RedisBlacklistService redisBlacklistService) {
        this.jwtProvider = jwtProvider;
        this.redisBlacklistService = redisBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // 토큰이 있는 경우에만 검사
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 블랙리스트에 있는 토큰이면 요청 거절!
            if (redisBlacklistService.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("로그아웃된 토큰입니다.");
                return; // 더 이상 진행하지 않고 여기서 차단
            }

            // 블랙리스트에 없으면 정상적으로 유효성 검사 진행(다시 보자...)
            try {
                // 1. 토큰 해독해서 정보 꺼내기
                Claims claims = jwtProvider.validateAndGetClaims(token);
                String userId = claims.getSubject(); // 토큰 만들 때 넣었던 userId (UUID)

                // 2. 스프링 시큐리티에게 인증되었다고 알려줄 '인증 객체' 만들기
                // UsernamePasswordAuthenticationToken(사용자 식별자, 비밀번호=null, 권한목록)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );

                // 3. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        // 다음 필터나 컨트롤러로 무사히 통과
        filterChain.doFilter(request, response);
    }
}