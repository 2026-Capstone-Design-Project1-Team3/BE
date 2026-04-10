package com.server.talkup_be.config;

import com.server.talkup_be.service.RedisBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

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

            // 블랙리스트에 없으면 정상적으로 유효성 검사 진행
            try {
                jwtProvider.validateAndGetClaims(token);
                // (이후 SecurityContext에 유저 정보 저장하는 로직 등이 들어갑니다)
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        // 다음 필터나 컨트롤러로 무사히 통과
        filterChain.doFilter(request, response);
    }
}