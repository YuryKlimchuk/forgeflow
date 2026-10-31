package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryAssemblyRepository;
import com.hydroyura.forgeflow.catalog.infrastructure.InMemoryPartRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

class DataSeederTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InMemoryPartRepository parts = new InMemoryPartRepository();
    private final InMemoryAssemblyRepository assemblies = new InMemoryAssemblyRepository();
    private final OrganizationId orgId = OrganizationId.generate();

    @Test
    void loadsAssembliesAndParts_whenEnabledAndFileExists() throws Exception {
        Path file = tempDir.resolve("catalog.json");
        Files.writeString(file, """
                [
                  {"number": "А-1", "name": "Корпус", "type": "ASSEMBLY"},
                  {"number": "П-1", "name": "Винт", "type": "PART"}
                ]
                """);

        DataSeeder seeder = new DataSeeder(parts, assemblies, orgId, new SeedProperties(true, file.toString()), objectMapper);
        seeder.run();

        assertThat(assemblies.findAll(orgId)).hasSize(1);
        assertThat(assemblies.findAll(orgId).get(0).getNumber()).isEqualTo("А-1");
        assertThat(assemblies.findAll(orgId).get(0).getOrganizationId()).isEqualTo(orgId);

        assertThat(parts.findAll(orgId)).hasSize(1);
        assertThat(parts.findAll(orgId).get(0).getNumber()).isEqualTo("П-1");
        assertThat(parts.findAll(orgId).get(0).getOrganizationId()).isEqualTo(orgId);
    }

    @Test
    void skips_whenDisabled() throws Exception {
        Path file = tempDir.resolve("catalog.json");
        Files.writeString(file, """
                [{"number": "А-1", "name": "Корпус", "type": "ASSEMBLY"}]
                """);

        DataSeeder seeder = new DataSeeder(parts, assemblies, orgId, new SeedProperties(false, file.toString()), objectMapper);
        seeder.run();

        assertThat(assemblies.findAll(orgId)).isEmpty();
        assertThat(parts.findAll(orgId)).isEmpty();
    }

    @Test
    void skips_whenFileMissing() throws Exception {
        Path missing = tempDir.resolve("nope.json");

        DataSeeder seeder = new DataSeeder(parts, assemblies, orgId, new SeedProperties(true, missing.toString()), objectMapper);
        seeder.run();

        assertThat(assemblies.findAll(orgId)).isEmpty();
        assertThat(parts.findAll(orgId)).isEmpty();
    }
}
