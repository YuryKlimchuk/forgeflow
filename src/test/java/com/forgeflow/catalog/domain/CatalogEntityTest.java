package com.forgeflow.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgeflow.catalog.api.AssemblyId;
import com.forgeflow.catalog.api.BuyId;
import com.forgeflow.catalog.api.PartId;
import com.forgeflow.catalog.api.StandartId;
import com.forgeflow.catalog.api.VzkId;
import com.forgeflow.catalog.domain.assembly.Assembly;
import com.forgeflow.catalog.domain.buy.Buy;
import com.forgeflow.catalog.domain.part.Part;
import com.forgeflow.catalog.domain.standart.Standart;
import com.forgeflow.catalog.domain.vzk.Vzk;
import java.lang.reflect.Field;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.junit.jupiter.api.Test;

class CatalogEntityTest {

    @Test
    void assembly_structureAndIdStorage() {
        assertEntityStructure(Assembly.class, AssemblyId.class);
        var id = AssemblyId.generate();
        assertThat(Assembly.create(id).getId()).isEqualTo(id);
    }

    @Test
    void part_structureAndIdStorage() {
        assertEntityStructure(Part.class, PartId.class);
        var id = PartId.generate();
        assertThat(Part.create(id).getId()).isEqualTo(id);
    }

    @Test
    void buy_structureAndIdStorage() {
        assertEntityStructure(Buy.class, BuyId.class);
        var id = BuyId.generate();
        assertThat(Buy.create(id).getId()).isEqualTo(id);
    }

    @Test
    void standart_structureAndIdStorage() {
        assertEntityStructure(Standart.class, StandartId.class);
        var id = StandartId.generate();
        assertThat(Standart.create(id).getId()).isEqualTo(id);
    }

    @Test
    void vzk_structureAndIdStorage() {
        assertEntityStructure(Vzk.class, VzkId.class);
        var id = VzkId.generate();
        assertThat(Vzk.create(id).getId()).isEqualTo(id);
    }

    private static void assertEntityStructure(Class<?> entityClass, Class<?> idClass) {
        assertThat(entityClass.getAnnotation(Entity.class)).isNotNull();
        Field[] fields = entityClass.getDeclaredFields();
        assertThat(fields).hasSize(1);
        assertThat(fields[0].getType()).isEqualTo(idClass);
        assertThat(fields[0].getAnnotation(Identity.class)).isNotNull();
    }
}
