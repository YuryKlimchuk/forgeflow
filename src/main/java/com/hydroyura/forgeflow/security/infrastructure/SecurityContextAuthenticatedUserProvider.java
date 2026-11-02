package com.hydroyura.forgeflow.security.infrastructure;

import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.security.api.AuthenticatedUser;
import com.hydroyura.forgeflow.security.api.AuthenticatedUserProvider;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Поставляет {@link AuthenticatedUser} из Spring Security context:
 * {@code sub} → {@code UserId}, issuer → realm.
 */
public class SecurityContextAuthenticatedUserProvider implements AuthenticatedUserProvider {

    private final Map<String, String> realmByIssuer;

    public SecurityContextAuthenticatedUserProvider(Map<String, String> realmByIssuer) {
        this.realmByIssuer = Objects.requireNonNull(realmByIssuer, "realmByIssuer must not be null");
    }

    @Override
    public Optional<AuthenticatedUser> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return Optional.empty();
        }
        Jwt jwt = jwtAuthentication.getToken();
        String realm = realmByIssuer.get(jwt.getIssuer().toString());
        if (realm == null) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedUser(UserId.of(jwt.getSubject()), realm));
    }
}
