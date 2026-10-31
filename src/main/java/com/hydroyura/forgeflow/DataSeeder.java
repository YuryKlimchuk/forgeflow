package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.catalog.domain.part.Part;
import com.hydroyura.forgeflow.catalog.domain.part.PartRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import tools.jackson.databind.ObjectMapper;

/**
 * Загружает демо-данные каталога из локального JSON-файла при старте.
 *
 * <p>Работает только при {@code forgeflow.seed.enabled=true}; если файла нет —
 * молча пропускает (info-лог). Данные принадлежат демо-организации.</p>
 */
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final PartRepository partRepository;
    private final AssemblyRepository assemblyRepository;
    private final OrganizationId organizationId;
    private final SeedProperties properties;
    private final ObjectMapper objectMapper;

    public DataSeeder(
            PartRepository partRepository,
            AssemblyRepository assemblyRepository,
            OrganizationId organizationId,
            SeedProperties properties,
            ObjectMapper objectMapper) {
        this.partRepository = Objects.requireNonNull(partRepository, "partRepository must not be null");
        this.assemblyRepository = Objects.requireNonNull(assemblyRepository, "assemblyRepository must not be null");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void run(String... args) throws Exception {
        if (!properties.enabled()) {
            log.info("Seed-загрузчик выключен (forgeflow.seed.enabled=false)");
            return;
        }

        Path file = Path.of(properties.dataFile());
        if (!Files.exists(file)) {
            log.info("Seed-файл не найден, загрузка пропущена: {}", file.toAbsolutePath());
            return;
        }

        List<SeedEntry> entries = List.of(objectMapper.readValue(file.toFile(), SeedEntry[].class));
        Instant now = Instant.now();
        int assemblies = 0;
        int parts = 0;

        for (SeedEntry entry : entries) {
            if (entry.type() == SeedEntryType.ASSEMBLY) {
                Assembly assembly = Assembly.create(
                        AssemblyId.generate(), entry.number(), entry.name(), organizationId, now, now);
                assemblyRepository.save(assembly);
                assemblies++;
            } else {
                Part part = Part.create(
                        PartId.generate(), entry.number(), entry.name(), organizationId, now, now);
                partRepository.save(part);
                parts++;
            }
        }

        log.info("Seed-данные загружены: {} сборок, {} деталей", assemblies, parts);
    }
}
