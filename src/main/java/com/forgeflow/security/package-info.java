/**
 * Аутентификация, авторизация и интеграция с Keycloak.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"organizations :: organizations", "shared :: shared"}
)
package com.forgeflow.security;
