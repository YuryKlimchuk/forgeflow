/**
 * Веб-слой: REST-адаптеры (driving adapters), зависят только от api-портов
 * доменных модулей.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"catalog :: catalog", "organizations :: organizations", "shared :: shared", "specifications :: specifications"}
)
package com.hydroyura.forgeflow.web;
