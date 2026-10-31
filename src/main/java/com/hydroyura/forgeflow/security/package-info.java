/**
 * Аутентификация, авторизация и интеграция с Keycloak.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"organizations :: organizations", "shared :: shared"}
)
package com.hydroyura.forgeflow.security;
