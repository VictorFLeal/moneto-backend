package com.moneto.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, long[]> requests = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 20;
    private static final int WINDOW_SECONDS = 60;

    private String getClientIP(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip  = getClientIP(request);
        long   now = Instant.now().getEpochSecond();

        requests.compute(ip, (k, val) -> {
            if (val == null || now - val[0] > WINDOW_SECONDS) {
                return new long[]{now, 1};
            }
            val[1]++;
            return val;
        });

        long[] tracker = requests.get(ip);
        if (tracker != null && tracker[1] > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Muitas tentativas. Aguarda 1 minuto.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}