package com.forgeflow.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgeflow.catalog.api.AssemblyId;
import com.forgeflow.catalog.api.BuyId;
import com.forgeflow.catalog.api.PartId;
import com.forgeflow.catalog.api.StandartId;
import com.forgeflow.catalog.api.VzkId;
import com.forgeflow.catalog.domain.assembly.Assembly;
import com.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.forgeflow.catalog.domain.buy.Buy;
import com.forgeflow.catalog.domain.buy.BuyRepository;
import com.forgeflow.catalog.domain.part.Part;
import com.forgeflow.catalog.domain.part.PartRepository;
import com.forgeflow.catalog.domain.standart.Standart;
import com.forgeflow.catalog.domain.standart.StandartRepository;
import com.forgeflow.catalog.domain.vzk.Vzk;
import com.forgeflow.catalog.domain.vzk.VzkRepository;
import com.forgeflow.shared.api.OrganizationId;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogRepositoryPortTest {

    @Test
    void assemblyRepositoryPort() {
        assertSaveDeletePort(AssemblyRepository.class, Assembly.class, AssemblyId.class);
    }

    @Test
    void buyRepositoryPort() {
        assertSaveDeletePort(BuyRepository.class, Buy.class, BuyId.class);
    }

    @Test
    void standartRepositoryPort() {
        assertSaveDeletePort(StandartRepository.class, Standart.class, StandartId.class);
    }

    @Test
    void vzkRepositoryPort() {
        assertSaveDeletePort(VzkRepository.class, Vzk.class, VzkId.class);
    }

    @Test
    void partRepositoryPort() {
        assertThat(PartRepository.class.isInterface()).isTrue();
        assertThat(PartRepository.class.getPackageName()).startsWith("com.forgeflow.catalog.domain");
        assertThat(PartRepository.class.getAnnotations()).isEmpty();

        Method save = declaredMethod(PartRepository.class, "save");
        assertThat(save.getReturnType()).isEqualTo(void.class);
        assertThat(save.getParameterTypes()).containsExactly(Part.class);

        Method findById = declaredMethod(PartRepository.class, "findById");
        assertThat(findById.getReturnType()).isEqualTo(Optional.class);
        assertThat(findById.getParameterTypes()).containsExactly(PartId.class, OrganizationId.class);

        Method findAll = declaredMethod(PartRepository.class, "findAll");
        assertThat(findAll.getReturnType()).isEqualTo(List.class);
        assertThat(findAll.getParameterTypes()).containsExactly(OrganizationId.class);

        Method existsByNumber = declaredMethod(PartRepository.class, "existsByNumber");
        assertThat(existsByNumber.getReturnType()).isEqualTo(boolean.class);
        assertThat(existsByNumber.getParameterTypes()).containsExactly(String.class, OrganizationId.class);
    }

    private static void assertSaveDeletePort(Class<?> portClass, Class<?> entityClass, Class<?> idClass) {
        assertThat(portClass.isInterface()).isTrue();
        assertThat(portClass.getPackageName()).startsWith("com.forgeflow.catalog.domain");
        assertThat(portClass.getAnnotations()).isEmpty();

        Method save = declaredMethod(portClass, "save");
        assertThat(save.getReturnType()).isEqualTo(void.class);
        assertThat(save.getParameterTypes()).containsExactly(entityClass);

        Method delete = declaredMethod(portClass, "delete");
        assertThat(delete.getReturnType()).isEqualTo(void.class);
        assertThat(delete.getParameterTypes()).containsExactly(idClass);
    }

    private static Method declaredMethod(Class<?> portClass, String name) {
        return Arrays.stream(portClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
