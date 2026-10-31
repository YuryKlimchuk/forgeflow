package com.hydroyura.forgeflow.organizations.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizationsValueTypesTest {

    @Test
    void organizationIdFactoryAndValidation() {
        UUID uuid = UUID.randomUUID();

        assertThat(OrganizationId.of(uuid).value()).isEqualTo(uuid);
        assertThat(OrganizationId.generate().value()).isNotNull();
        assertThatThrownBy(() -> OrganizationId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void ownerIdFactoryAndValidation() {
        UUID uuid = UUID.randomUUID();

        assertThat(OwnerId.of(uuid).value()).isEqualTo(uuid);
        assertThat(OwnerId.generate().value()).isNotNull();
        assertThatThrownBy(() -> OwnerId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void userIdRejectsNullAndBlank() {
        assertThat(UserId.of("user-1").value()).isEqualTo("user-1");
        assertThatThrownBy(() -> UserId.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserId.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
