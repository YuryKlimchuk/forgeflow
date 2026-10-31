package com.hydroyura.forgeflow.organizations.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrganizationSettingsTest {

    @Test
    void emptyHasNoRestrictions() {
        OrganizationSettings settings = OrganizationSettings.empty();

        assertThat(settings.partNumberPattern()).isNull();
        assertThat(settings.assemblyNumberPattern()).isNull();
        assertThat(settings.buyNumberPattern()).isNull();
        assertThat(settings.standartNumberPattern()).isNull();
        assertThat(settings.vzkNumberPattern()).isNull();
    }

    @Test
    void blankPatternsNormalizedToNull() {
        OrganizationSettings settings = new OrganizationSettings(" ", "", "  ", null, "VZK-[0-9]+");

        assertThat(settings.partNumberPattern()).isNull();
        assertThat(settings.assemblyNumberPattern()).isNull();
        assertThat(settings.buyNumberPattern()).isNull();
        assertThat(settings.standartNumberPattern()).isNull();
        assertThat(settings.vzkNumberPattern()).isEqualTo("VZK-[0-9]+");
    }

    @Test
    void recordsWithSameValuesAreEqual() {
        OrganizationSettings a = new OrganizationSettings("P-\\d+", null, null, null, null);
        OrganizationSettings b = new OrganizationSettings("P-\\d+", null, null, null, null);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(OrganizationSettings.empty());
    }

    @Test
    void matchesUsesFullMatchAndSkipsAbsentPattern() {
        OrganizationSettings settings = new OrganizationSettings("PART-[0-9]+", null, null, null, null);

        assertThat(settings.matchesPart("PART-42")).isTrue();
        assertThat(settings.matchesPart("42")).isFalse();
        assertThat(settings.matchesPart("X PART-42 Y")).isFalse();
        assertThat(settings.matchesPart(null)).isFalse();
        // отсутствующий паттерн — любой номер проходит
        assertThat(settings.matchesAssembly("что угодно")).isTrue();
        assertThat(settings.matchesBuy(null)).isTrue();
    }
}
