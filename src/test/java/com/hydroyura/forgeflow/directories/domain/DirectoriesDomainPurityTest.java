package com.hydroyura.forgeflow.directories.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.directories.domain.material.DuplicateMaterialNumberException;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.directories.domain.material.MaterialNotFoundException;
import com.hydroyura.forgeflow.directories.domain.material.MaterialRepository;
import com.hydroyura.forgeflow.directories.domain.sortament.DuplicateSortamentNumberException;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentNotFoundException;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentRepository;
import java.lang.annotation.Annotation;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Архитектурная проверка: домен directories не содержит Spring/JPA/PostgreSQL/Flyway.
 */
class DirectoriesDomainPurityTest {

    @Test
    void aggregatesUseOnlyJmoleculesDddAnnotations() {
        for (Class<?> type : List.of(Material.class, Sortament.class)) {
            for (Annotation annotation : type.getAnnotations()) {
                assertThat(annotation.annotationType().getPackageName())
                        .as("%s annotation %s", type.getSimpleName(), annotation.annotationType().getSimpleName())
                        .isEqualTo("org.jmolecules.ddd.annotation");
            }
        }
    }

    @Test
    void repositoryPortsAreAnnotationFreeInterfaces() {
        assertThat(MaterialRepository.class.isInterface()).isTrue();
        assertThat(MaterialRepository.class.getAnnotations()).isEmpty();
        assertThat(SortamentRepository.class.isInterface()).isTrue();
        assertThat(SortamentRepository.class.getAnnotations()).isEmpty();
    }

    @Test
    void domainTypesDoNotUseInfrastructureAnnotations() {
        for (Class<?> type : domainTypes()) {
            for (Annotation annotation : type.getAnnotations()) {
                String annotationPackage = annotation.annotationType().getPackageName();
                assertThat(annotationPackage)
                        .as("%s annotation %s", type.getSimpleName(), annotation.annotationType().getSimpleName())
                        .doesNotContain("springframework", "jakarta.persistence", "org.postgresql", "flyway");
            }
        }
    }

    private static List<Class<?>> domainTypes() {
        return List.of(
                Material.class,
                Sortament.class,
                MaterialRepository.class,
                SortamentRepository.class,
                MaterialNotFoundException.class,
                DuplicateMaterialNumberException.class,
                SortamentNotFoundException.class,
                DuplicateSortamentNumberException.class);
    }
}
