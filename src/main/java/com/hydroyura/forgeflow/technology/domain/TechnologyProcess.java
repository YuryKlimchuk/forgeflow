package com.hydroyura.forgeflow.technology.domain;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

/**
 * Технологический процесс изготовления детали.
 *
 * <p>Агрегат связывает неизменяемую ссылку на деталь каталога, номер и наименование
 * процесса, набор операций и флаг дефолтного процесса. Удаление логическое.</p>
 */
@Entity
public class TechnologyProcess {

    @Identity
    private final TechnologyProcessId id;

    private final String number;
    private String name;
    private final PartId partId;
    private final OrganizationId organizationId;
    private boolean defaultProcess;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private final List<Operation> operations = new ArrayList<>();

    private TechnologyProcess(TechnologyProcessId id, String number, String name, PartId partId,
            OrganizationId organizationId, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.number = requireNotBlank(number, "number");
        this.name = requireNotBlank(name, "name");
        this.partId = Objects.requireNonNull(partId, "partId must not be null");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static TechnologyProcess create(TechnologyProcessId id, String number, String name, PartId partId,
            OrganizationId organizationId, Instant createdAt, Instant updatedAt) {
        return new TechnologyProcess(id, number, name, partId, organizationId, createdAt, updatedAt);
    }

    /**
     * Восстанавливает агрегат из персистентного представления без потерь.
     *
     * <p>Единственный способ построить процесс с {@code defaultProcess = true},
     * непустым {@code deletedAt} и набором операций. Операции копируются во
     * внутренний список в порядке возрастания номера без вызова {@link #addOperation},
     * чтобы не перезаписывать {@code updatedAt}.</p>
     */
    public static TechnologyProcess restore(
            TechnologyProcessId id, String number, String name, PartId partId,
            OrganizationId organizationId, boolean defaultProcess,
            Instant createdAt, Instant updatedAt, Instant deletedAt,
            List<Operation> operations) {
        TechnologyProcess process = new TechnologyProcess(id, number, name, partId, organizationId, createdAt, updatedAt);
        process.defaultProcess = defaultProcess;
        process.deletedAt = deletedAt;
        Objects.requireNonNull(operations, "operations must not be null").stream()
                .sorted(Comparator.comparingInt(Operation::number))
                .forEach(process.operations::add);
        return process;
    }

    public void rename(String newName) {
        this.name = requireNotBlank(newName, "name");
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        if (deletedAt != null) {
            return;
        }
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public void addOperation(int number, String name) {
        Operation operation = new Operation(number, name);
        if (operations.stream().anyMatch(existing -> existing.number() == operation.number())) {
            throw new DuplicateOperationException(number);
        }
        operations.add(insertionIndex(number), operation);
        this.updatedAt = Instant.now();
    }

    public void removeOperation(int number) {
        int index = indexOf(number);
        if (index < 0) {
            throw new OperationNotFoundException(number);
        }
        operations.remove(index);
        this.updatedAt = Instant.now();
    }

    public List<Operation> operations() {
        return Collections.unmodifiableList(operations);
    }

    void markAsDefault() {
        if (defaultProcess) {
            return;
        }
        this.defaultProcess = true;
        this.updatedAt = Instant.now();
    }

    void markAsNotDefault() {
        if (!defaultProcess) {
            return;
        }
        this.defaultProcess = false;
        this.updatedAt = Instant.now();
    }

    public TechnologyProcessId getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public PartId getPartId() {
        return partId;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public boolean isDefaultProcess() {
        return defaultProcess;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    private int insertionIndex(int number) {
        int index = 0;
        while (index < operations.size() && operations.get(index).number() < number) {
            index++;
        }
        return index;
    }

    private int indexOf(int number) {
        for (int i = 0; i < operations.size(); i++) {
            if (operations.get(i).number() == number) {
                return i;
            }
        }
        return -1;
    }

    private static String requireNotBlank(String value, String field) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
