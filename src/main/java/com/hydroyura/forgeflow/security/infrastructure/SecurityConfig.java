package com.hydroyura.forgeflow.security.infrastructure;

import com.hydroyura.forgeflow.security.application.IssuerProperties;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security: OAuth2 Resource Server с двумя доверенными Keycloak
 * issuer'ами, выбор decoder'а по claim {@code iss}.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(IssuerProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtIssuerAuthenticationManagerResolver authenticationManagerResolver) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(authenticationManagerResolver));
        return http.build();
    }

    @Bean
    JwtIssuerAuthenticationManagerResolver authenticationManagerResolver(IssuerProperties properties) {
        Map<String, AuthenticationManager> managers = new HashMap<>();
        for (IssuerProperties.Issuer issuer : properties.issuers()) {
            JwtDecoder decoder = NimbusJwtDecoder
                    .withJwkSetUri(issuer.issuer() + "/protocol/openid-connect/certs")
                    .build();
            JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
            managers.put(issuer.issuer(), provider::authenticate);
        }
        return new JwtIssuerAuthenticationManagerResolver(managers::get);
    }

    @Bean
    SecurityContextAuthenticatedUserProvider securityContextAuthenticatedUserProvider(IssuerProperties properties) {
        return new SecurityContextAuthenticatedUserProvider(properties.realmByIssuer());
    }
}
