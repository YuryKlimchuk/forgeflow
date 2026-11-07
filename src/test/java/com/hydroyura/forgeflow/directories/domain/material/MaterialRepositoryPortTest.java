package com.hydroyura.forgeflow.directories.domain.material;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaterialRepositoryPortTest {

    @Test
    void repositoryPort_hasOnlyCrudMethodsAndNoDelete() {
        Class<MaterialRepository> port = MaterialRepository.class;

        assertThat(port.isInterface()).isTrue();
        assertThat(port.getPackageName()).isEqualTo("com.hydroyura.forgeflow.directories.domain.material");
        assertThat(port.getAnnotations()).isEmpty();

        Method save = declaredMethod(port, "save");
        assertThat(save.getReturnType()).isEqualTo(void.class);
        assertThat(save.getParameterTypes()).containsExactly(Material.class);

        Method findById = declaredMethod(port, "findById");
        assertThat(findById.getReturnType()).isEqualTo(Optional.class);
        assertThat(findById.getParameterTypes()).containsExactly(MaterialId.class, OrganizationId.class);

        Method findAll = declaredMethod(port, "findAll");
        assertThat(findAll.getReturnType()).isEqualTo(List.class);
        assertThat(findAll.getParameterTypes()).containsExactly(OrganizationId.class);

        Method existsByNumber = declaredMethod(port, "existsByNumber");
        assertThat(existsByNumber.getReturnType()).isEqualTo(boolean.class);
        assertThat(existsByNumber.getParameterTypes()).containsExactly(String.class, OrganizationId.class);

        assertThat(declaredMethodNames(port))
                .containsExactlyInAnyOrder("save", "findById", "findAll", "existsByNumber");
    }

    private static Method declaredMethod(Class<?> port, String name) {
        return Arrays.stream(port.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> declaredMethodNames(Class<?> port) {
        return Arrays.stream(port.getDeclaredMethods())
                .map(Method::getName)
                .toList();
    }
}
