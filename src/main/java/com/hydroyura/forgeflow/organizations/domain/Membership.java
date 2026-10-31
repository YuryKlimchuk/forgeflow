package com.hydroyura.forgeflow.organizations.domain;

import com.hydroyura.forgeflow.organizations.api.UserId;
import java.util.Objects;

/**
 * Участник организации. Не имеет поля роли: наличие membership означает
 * роль {@code member}.
 */
public record Membership(UserId userId) {

    public Membership {
        Objects.requireNonNull(userId, "userId must not be null");
    }
}
