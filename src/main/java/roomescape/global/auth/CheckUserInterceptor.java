package roomescape.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.global.exception.AuthorizationException;

@Component
public class CheckUserInterceptor implements HandlerInterceptor {

    private final JwtManager jwtManager;

    public CheckUserInterceptor(JwtManager jwtManager) {
        this.jwtManager = jwtManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long memberId = jwtManager.parseToken(request);
        if (memberId == -1L) {
            throw new AuthorizationException("로그인 후 이용하세요.");
        }
        return true;
    }
}