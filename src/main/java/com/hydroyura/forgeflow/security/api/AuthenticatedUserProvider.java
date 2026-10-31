package com.hydroyura.forgeflow.security.api;

import java.util.Optional;

/**
 * Порт получения текущего аутентифицированного пользователя.
 */
public interface AuthenticatedUserProvider {

    Optional<AuthenticatedUser> current();
}
