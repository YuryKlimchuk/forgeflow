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
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CatalogRepositoryPortTest {

    @Test
    void assemblyRepositoryPort() {
        assertPort(AssemblyRepository.class, Assembly.class, AssemblyId.class);
    }

    @Test
    void partRepositoryPort() {
        assertPort(PartRepository.class, Part.class, PartId.class);
    }

    @Test
    void buyRepositoryPort() {
        assertPort(BuyRepository.class, Buy.class, BuyId.class);
    }

    @Test
    void standartRepositoryPort() {
        assertPort(StandartRepository.class, Standart.class, StandartId.class);
    }

    @Test
    void vzkRepositoryPort() {
        assertPort(VzkRepository.class, Vzk.class, VzkId.class);
    }

    private static void assertPort(Class<?> portClass, Class<?> entityClass, Class<?> idClass) {
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
