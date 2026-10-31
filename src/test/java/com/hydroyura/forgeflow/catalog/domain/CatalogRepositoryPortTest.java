package com.hydroyura.forgeflow.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.catalog.domain.buy.Buy;
import com.hydroyura.forgeflow.catalog.domain.buy.BuyRepository;
import com.hydroyura.forgeflow.catalog.domain.part.Part;
import com.hydroyura.forgeflow.catalog.domain.part.PartRepository;
import com.hydroyura.forgeflow.catalog.domain.standart.Standart;
import com.hydroyura.forgeflow.catalog.domain.standart.StandartRepository;
import com.hydroyura.forgeflow.catalog.domain.vzk.Vzk;
import com.hydroyura.forgeflow.catalog.domain.vzk.VzkRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogRepositoryPortTest {

    @Test
    void assemblyRepositoryPort() {
        assertCrudPort(AssemblyRepository.class, Assembly.class, AssemblyId.class);
    }

    @Test
    void buyRepositoryPort() {
        assertCrudPort(BuyRepository.class, Buy.class, BuyId.class);
    }

    @Test
    void standartRepositoryPort() {
        assertCrudPort(StandartRepository.class, Standart.class, StandartId.class);
    }

    @Test
    void vzkRepositoryPort() {
        assertCrudPort(VzkRepository.class, Vzk.class, VzkId.class);
    }

    @Test
    void partRepositoryPort() {
        assertCrudPort(PartRepository.class, Part.class, PartId.class);
    }

    private static void assertCrudPort(Class<?> portClass, Class<?> entityClass, Class<?> idClass) {
        assertThat(portClass.isInterface()).isTrue();
        assertThat(portClass.getPackageName()).startsWith("com.hydroyura.forgeflow.catalog.domain");
        assertThat(portClass.getAnnotations()).isEmpty();

        Method save = declaredMethod(portClass, "save");
        assertThat(save.getReturnType()).isEqualTo(void.class);
        assertThat(save.getParameterTypes()).containsExactly(entityClass);

        Method findById = declaredMethod(portClass, "findById");
        assertThat(findById.getReturnType()).isEqualTo(Optional.class);
        assertThat(findById.getParameterTypes()).containsExactly(idClass, OrganizationId.class);

        Method findAll = declaredMethod(portClass, "findAll");
        assertThat(findAll.getReturnType()).isEqualTo(List.class);
        assertThat(findAll.getParameterTypes()).containsExactly(OrganizationId.class);

        Method existsByNumber = declaredMethod(portClass, "existsByNumber");
        assertThat(existsByNumber.getReturnType()).isEqualTo(boolean.class);
        assertThat(existsByNumber.getParameterTypes()).containsExactly(String.class, OrganizationId.class);

        assertThat(declaredMethods(portClass)).containsExactlyInAnyOrder("save", "findById", "findAll", "existsByNumber");
    }

    private static Method declaredMethod(Class<?> portClass, String name) {
        return Arrays.stream(portClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> declaredMethods(Class<?> portClass) {
        return Arrays.stream(portClass.getDeclaredMethods())
                .map(Method::getName)
                .toList();
    }
}
