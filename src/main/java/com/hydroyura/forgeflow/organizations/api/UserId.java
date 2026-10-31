package com.hydroyura.forgeflow.organizations.api;

import org.apache.commons.lang3.StringUtils;

/**
 * Непрозрачный непустой идентификатор пользователя (subject).
 *
 * <p>На этом этапе формат значения не привязан к внешней системе
 * идентификации и трактуется как непрозрачная строка.</p>
 */
public record UserId(String value) {

    public UserId {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}
