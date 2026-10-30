package com.forgeflow.organizations.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Архитектурная проверка: домен organisations не содержит
 * Spring/JPA/инфраструктурных аннотаций.
 */
class OrganizationsDomainPurityTest {

    @Test
    void entitiesUseOnlyJmoleculesDddAnnotations() {
        for (Class<?> type : List.of(Owner.class, Organization.class)) {
            for (Annotation annotation : type.getAnnotations()) {
                assertThat(annotation.annotationType().getPackageName())
                        .as("%s annotation %s", type.getSimpleName(), annotation.annotationType().getSimpleName())
                        .isEqualTo("org.jmolecules.ddd.annotation");
            }
        }
    }

    @Test
    void repositoryPortsAreAnnotationFreeInterfaces() {
        for (Class<?> type : List.of(OwnerRepository.class, OrganizationRepository.class)) {
            assertThat(type.isInterface())
                    .as("%s is an interface", type.getSimpleName())
                    .isTrue();
            assertThat(type.getAnnotations())
                    .as("%s has no annotations", type.getSimpleName())
                    .isEmpty();
        }
    }
}
