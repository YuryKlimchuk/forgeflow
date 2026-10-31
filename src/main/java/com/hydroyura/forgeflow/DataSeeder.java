package com.hydroyura.forgeflow;

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
import com.hydroyura.forgeflow.organizations.api.OrganizationService;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.domain.Quantity;
import com.hydroyura.forgeflow.specifications.domain.Specification;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import tools.jackson.databind.ObjectMapper;

/**
 * Загружает демо-данные каталога и спецификаций при старте.
 *
 * <p>Работает только при {@code forgeflow.seed.enabled=true}; если файла нет —
 * молча пропускает. Все данные принадлежат демо-организации.</p>
 */
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String PART_PATTERN = "Д-\\d{4}";
    private static final String ASSEMBLY_PATTERN = "СБ-\\d{4}";
    private static final String BUY_PATTERN = "ПОК-\\d{4}";
    private static final String STANDART_PATTERN = "СТ-\\d{4}";
    private static final String VZK_PATTERN = "ВЗК-\\d{4}";

    private final PartRepository partRepository;
    private final AssemblyRepository assemblyRepository;
    private final BuyRepository buyRepository;
    private final StandartRepository standartRepository;
    private final VzkRepository vzkRepository;
    private final SpecificationRepository specificationRepository;
    private final OrganizationService organizationService;
    private final OrganizationId organizationId;
    private final SeedProperties properties;
    private final ObjectMapper objectMapper;

    public DataSeeder(
            PartRepository partRepository,
            AssemblyRepository assemblyRepository,
            BuyRepository buyRepository,
            StandartRepository standartRepository,
            VzkRepository vzkRepository,
            SpecificationRepository specificationRepository,
            OrganizationService organizationService,
            OrganizationId organizationId,
            SeedProperties properties,
            ObjectMapper objectMapper) {
        this.partRepository = Objects.requireNonNull(partRepository, "partRepository must not be null");
        this.assemblyRepository = Objects.requireNonNull(assemblyRepository, "assemblyRepository must not be null");
        this.buyRepository = Objects.requireNonNull(buyRepository, "buyRepository must not be null");
        this.standartRepository = Objects.requireNonNull(standartRepository, "standartRepository must not be null");
        this.vzkRepository = Objects.requireNonNull(vzkRepository, "vzkRepository must not be null");
        this.specificationRepository = Objects.requireNonNull(specificationRepository, "specificationRepository must not be null");
        this.organizationService = Objects.requireNonNull(organizationService, "organizationService must not be null");
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

        Path catalogFile = Path.of(properties.dataFile());
        if (!Files.exists(catalogFile)) {
            log.info("Seed-файл каталога не найден, загрузка пропущена: {}", catalogFile.toAbsolutePath());
            return;
        }

        organizationService.updateSettings(organizationId, new OrganizationSettings(
                PART_PATTERN, ASSEMBLY_PATTERN, BUY_PATTERN, STANDART_PATTERN, VZK_PATTERN));

        List<SeedEntry> entries = List.of(objectMapper.readValue(catalogFile.toFile(), SeedEntry[].class));
        Instant now = Instant.now();
        Map<String, UUID> index = new HashMap<>();

        for (SeedEntry entry : entries) {
            switch (entry.type()) {
                case ASSEMBLY -> {
                    Assembly assembly = Assembly.create(
                            AssemblyId.generate(), entry.number(), entry.name(), organizationId, now, now);
                    assemblyRepository.save(assembly);
                    index.put(key(entry), assembly.getId().value());
                }
                case PART -> {
                    Part part = Part.create(
                            PartId.generate(), entry.number(), entry.name(), organizationId, now, now);
                    partRepository.save(part);
                    index.put(key(entry), part.getId().value());
                }
                case BUY -> {
                    Buy buy = Buy.create(
                            BuyId.generate(), entry.number(), entry.name(), organizationId, now, now);
                    buyRepository.save(buy);
                    index.put(key(entry), buy.getId().value());
                }
                case STANDART -> {
                    Standart standart = Standart.create(
                            StandartId.generate(), entry.number(), entry.name(), organizationId, now, now);
                    standartRepository.save(standart);
                    index.put(key(entry), standart.getId().value());
                }
                case VZK -> {
                    Vzk vzk = Vzk.create(
                            VzkId.generate(), entry.number(), entry.name(), organizationId, now, now);
                    vzkRepository.save(vzk);
                    index.put(key(entry), vzk.getId().value());
                }
            }
        }

        loadSpecifications(index);

        log.info("Seed-данные загружены: {} записей каталога", entries.size());
    }

    private void loadSpecifications(Map<String, UUID> index) throws Exception {
        Path specificationsFile = Path.of(properties.specificationsFile());
        if (!Files.exists(specificationsFile)) {
            log.info("Seed-файл спецификаций не найден, пропущен: {}", specificationsFile.toAbsolutePath());
            return;
        }

        List<SeedSpecification> specifications = List.of(
                objectMapper.readValue(specificationsFile.toFile(), SeedSpecification[].class));

        for (SeedSpecification specification : specifications) {
            UUID assemblyUuid = index.get(key(SeedEntryType.ASSEMBLY, specification.assemblyNumber()));
            if (assemblyUuid == null) {
                throw new IllegalStateException(
                        "Seed-спецификация ссылается на отсутствующую сборку: " + specification.assemblyNumber());
            }
            Specification aggregate = Specification.create(organizationId, AssemblyId.of(assemblyUuid));
            for (SeedSpecificationRow row : specification.rows()) {
                UUID itemUuid = index.get(key(row.type(), row.number()));
                if (itemUuid == null) {
                    throw new IllegalStateException(
                            "Seed-спецификация ссылается на отсутствующий элемент: " + row.type() + " " + row.number());
                }
                CatalogItemReference reference = new CatalogItemReference(toCatalogItemType(row.type()), itemUuid);
                aggregate.addRow(reference, new Quantity(row.quantity()));
            }
            specificationRepository.save(aggregate);
        }

        log.info("Seed-спецификации загружены: {}", specifications.size());
    }

    private static String key(SeedEntry entry) {
        return key(entry.type(), entry.number());
    }

    private static String key(SeedEntryType type, String number) {
        return type.name() + ":" + number;
    }

    private static CatalogItemType toCatalogItemType(SeedEntryType type) {
        return switch (type) {
            case PART -> CatalogItemType.PART;
            case ASSEMBLY -> CatalogItemType.ASSEMBLY;
            case BUY -> CatalogItemType.BUY;
            case STANDART -> CatalogItemType.STANDART;
            case VZK -> CatalogItemType.VZK;
        };
    }
}
