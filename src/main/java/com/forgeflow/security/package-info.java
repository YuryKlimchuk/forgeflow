/**
 * Аутентификация, авторизация и интеграция с Keycloak.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"organizations :: organizations"}
)
package com.forgeflow.security;
