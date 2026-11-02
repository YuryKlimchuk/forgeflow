package com.hydroyura.forgeflow.organizations.infrastructure;

import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.application.SubjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Поставляет текущий subject из Spring Security context (JWT {@code sub}).
 */
public class SecurityContextSubjectProvider implements SubjectProvider {

    @Override
    public UserId currentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return UserId.of(jwtAuthentication.getToken().getSubject());
        }
        throw new IllegalStateException("No authenticated subject");
    }
}
