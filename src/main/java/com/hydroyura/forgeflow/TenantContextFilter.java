package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.security.infrastructure.OrganizationContextLifecycle;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Оборачивает каждый запрос жизненным циклом организации: резолвит tenant из
 * аутентифицированного пользователя (серверно), устанавливает контекст до
 * обработчика и очищает {@code ThreadLocal} в {@code finally}.
 *
 * <p>Клиентский заголовок {@code X-Organization-Id} НЕ читается.</p>
 */
public class TenantContextFilter extends OncePerRequestFilter {

    private final OrganizationContextLifecycle lifecycle;

    public TenantContextFilter(OrganizationContextLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            lifecycle.run(() -> {
                try {
                    filterChain.doFilter(request, response);
                    return null;
                } catch (ServletException | IOException e) {
                    throw new ServletFilterRuntimeException(e);
                }
            });
        } catch (ServletFilterRuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServletException servletException) {
                throw servletException;
            }
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }

    private static final class ServletFilterRuntimeException extends RuntimeException {
        ServletFilterRuntimeException(Exception cause) {
            super(cause);
        }
    }
}
