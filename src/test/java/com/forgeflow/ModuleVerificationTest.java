package com.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModuleVerificationTest {

    private static final List<String> EXPECTED_MODULES = List.of(
            "organizations", "adminpanel", "technology", "specifications",
            "catalog", "directories", "shared", "security");

    @Test
    void verifyModularity() {
        var modules = ApplicationModules.of(ForgeFlowApplication.class);

        assertThat(modules.stream()
                .map(module -> module.getIdentifier().toString())
                .toList())
                .containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);

        modules.verify();
    }
}
