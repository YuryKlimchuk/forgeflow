package com.hydroyura.forgeflow.security.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Конфигурация доверенных Keycloak issuer'ов: URL issuer'а и соответствующий
 * ForgeFlow realm.
 */
@ConfigurationProperties(prefix = "forgeflow.security")
public record IssuerProperties(List<Issuer> issuers) {

    public record Issuer(String issuer, String realm) {
    }

    public List<String> issuerUrls() {
        return issuers.stream().map(Issuer::issuer).toList();
    }

    public Map<String, String> realmByIssuer() {
        return issuers.stream().collect(Collectors.toMap(Issuer::issuer, Issuer::realm));
    }
}
