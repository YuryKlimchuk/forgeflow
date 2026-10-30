package com.forgeflow.security.infrastructure;

import com.forgeflow.security.api.AuthenticatedUser;
import com.forgeflow.security.api.AuthenticatedUserProvider;
import java.util.Optional;

/**
 * Заглушка текущего аутентифицированного пользователя (без Keycloak).
 */
public class StubAuthenticatedUserProvider implements AuthenticatedUserProvider {

    private volatile AuthenticatedUser current;

    public void set(AuthenticatedUser user) {
        this.current = user;
    }

    public void clear() {
        this.current = null;
    }

    @Override
    public Optional<AuthenticatedUser> current() {
        return Optional.ofNullable(current);
    }
}
