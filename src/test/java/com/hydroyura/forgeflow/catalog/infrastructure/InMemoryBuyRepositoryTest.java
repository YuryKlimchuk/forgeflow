package com.hydroyura.forgeflow.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.domain.buy.Buy;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryBuyRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgA = OrganizationId.generate();
    private final OrganizationId orgB = OrganizationId.generate();
    private final InMemoryBuyRepository repository = new InMemoryBuyRepository();

    private static Buy buy(OrganizationId org, String number, String name) {
        return Buy.create(BuyId.generate(), number, name, org, NOW, NOW);
    }

    @Test
    void findById_returnsOnlyActiveBuyOfOrganization() {
        Buy buy = buy(orgA, "Б-1", "Покупное");
        repository.save(buy);

        assertThat(repository.findById(buy.getId(), orgA)).contains(buy);
        assertThat(repository.findById(buy.getId(), orgB)).isEmpty();

        buy.markDeleted();
        repository.save(buy);
        assertThat(repository.findById(buy.getId(), orgA)).isEmpty();
    }

    @Test
    void findAll_returnsActiveBuysOfOrganizationSortedByNumber() {
        Buy b2 = buy(orgA, "Б-2", "Вторая");
        Buy b1 = buy(orgA, "Б-1", "Первая");
        Buy other = buy(orgB, "Б-3", "Чужая");
        Buy deleted = buy(orgA, "Б-0", "Удалённая");
        deleted.markDeleted();
        repository.save(b2);
        repository.save(b1);
        repository.save(other);
        repository.save(deleted);

        List<Buy> result = repository.findAll(orgA);

        assertThat(result).extracting(Buy::getNumber).containsExactly("Б-1", "Б-2");
    }

    @Test
    void existsByNumber_checksOnlyActiveBuysOfOrganization() {
        Buy buy = buy(orgA, "Б-1", "Покупное");
        repository.save(buy);

        assertThat(repository.existsByNumber("Б-1", orgA)).isTrue();
        assertThat(repository.existsByNumber("Б-1", orgB)).isFalse();
        assertThat(repository.existsByNumber("Б-2", orgA)).isFalse();

        buy.markDeleted();
        repository.save(buy);
        assertThat(repository.existsByNumber("Б-1", orgA)).isFalse();
    }
}
