package com.hydroyura.forgeflow.technology.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OperationContractsTest {

    @Test
    void addOperationRequest_twoArgConstructor_setsNullEquipmentId() {
        AddOperationRequest request = new AddOperationRequest(10, "Сверление");

        assertThat(request.number()).isEqualTo(10);
        assertThat(request.name()).isEqualTo("Сверление");
        assertThat(request.equipmentId()).isNull();
    }

    @Test
    void addOperationRequest_withEquipmentId_preservesReference() {
        EquipmentId equipmentId = EquipmentId.generate();

        AddOperationRequest request = new AddOperationRequest(10, "Сверление", equipmentId);

        assertThat(request.equipmentId()).isEqualTo(equipmentId);
    }

    @Test
    void operationView_twoArgConstructor_setsNullEquipmentId() {
        OperationView view = new OperationView(10, "Сверление");

        assertThat(view.number()).isEqualTo(10);
        assertThat(view.name()).isEqualTo("Сверление");
        assertThat(view.equipmentId()).isNull();
    }

    @Test
    void operationView_withEquipmentId_preservesReference() {
        EquipmentId equipmentId = EquipmentId.generate();

        OperationView view = new OperationView(10, "Сверление", equipmentId);

        assertThat(view.equipmentId()).isEqualTo(equipmentId);
    }
}
