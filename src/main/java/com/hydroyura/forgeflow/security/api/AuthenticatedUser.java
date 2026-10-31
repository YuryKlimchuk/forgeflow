package com.hydroyura.forgeflow.security.api;

import com.hydroyura.forgeflow.organizations.api.UserId;
import java.util.Objects;

/**
 * Аутентифицированный пользователь: идентификатор и realm.
 */
public record AuthenticatedUser(UserId userId, String realm) {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(realm, "realm must not be null");
    }
}
