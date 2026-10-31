package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.api.BuyView;
import com.hydroyura.forgeflow.catalog.domain.buy.Buy;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BuyMapperTest {

    @Test
    void mapsBuyToView() {
        BuyId id = BuyId.generate();
        OrganizationId org = OrganizationId.generate();
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-01-02T00:00:00Z");
        Buy buy = Buy.create(id, "Б-1", "Покупное", org, created, updated);

        BuyView view = BuyMapper.INSTANCE.toView(buy);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.number()).isEqualTo("Б-1");
        assertThat(view.name()).isEqualTo("Покупное");
        assertThat(view.createdAt()).isEqualTo(created);
        assertThat(view.updatedAt()).isEqualTo(updated);
    }
}
