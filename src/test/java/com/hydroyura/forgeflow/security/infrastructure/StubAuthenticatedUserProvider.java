package com.hydroyura.forgeflow.security.infrastructure;

import com.hydroyura.forgeflow.security.api.AuthenticatedUser;
import com.hydroyura.forgeflow.security.api.AuthenticatedUserProvider;
import java.util.Optional;

/**
 * Тестовый двойник порта {@link AuthenticatedUserProvider} для unit-тестов.
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
