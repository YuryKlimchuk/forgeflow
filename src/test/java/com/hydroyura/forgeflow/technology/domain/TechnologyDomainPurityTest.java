package com.hydroyura.forgeflow.technology.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.technology.domain.equipment.DuplicateEquipmentNumberException;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentNotFoundException;
import com.hydroyura.forgeflow.technology.domain.equipment.EquipmentRepository;
import java.lang.annotation.Annotation;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Архитектурная проверка: домен technology не содержит Spring/JPA/PostgreSQL/Flyway.
 */
class TechnologyDomainPurityTest {

    @Test
    void aggregateUsesOnlyJmoleculesDddAnnotations() {
        for (Class<?> type : List.of(TechnologyProcess.class, Equipment.class)) {
            for (Annotation annotation : type.getAnnotations()) {
                assertThat(annotation.annotationType().getPackageName())
                        .as("%s annotation %s", type.getSimpleName(), annotation.annotationType().getSimpleName())
                        .isEqualTo("org.jmolecules.ddd.annotation");
            }
        }
    }

    @Test
    void repositoryPortIsAnnotationFreeInterface() {
        assertThat(TechnologyProcessRepository.class.isInterface()).isTrue();
        assertThat(TechnologyProcessRepository.class.getAnnotations()).isEmpty();
        assertThat(EquipmentRepository.class.isInterface()).isTrue();
        assertThat(EquipmentRepository.class.getAnnotations()).isEmpty();
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
                TechnologyProcess.class,
                Operation.class,
                TechnologyProcessRepository.class,
                TechnologyProcessService.class,
                DuplicateTechnologyProcessNumberException.class,
                DuplicateOperationException.class,
                OperationNotFoundException.class,
                TechnologyProcessNotFoundException.class,
                InvalidDefaultProcessStateException.class,
                Equipment.class,
                EquipmentRepository.class,
                EquipmentNotFoundException.class,
                DuplicateEquipmentNumberException.class);
    }
}
