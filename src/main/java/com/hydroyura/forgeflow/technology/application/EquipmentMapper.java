package com.hydroyura.forgeflow.technology.application;

import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.EquipmentService;
import com.hydroyura.forgeflow.technology.api.EquipmentView;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;

/**
 * Маппер оборудования в публичное представление.
 */
public class EquipmentMapper {

    public EquipmentView toView(Equipment equipment) {
        return new EquipmentView(
                equipment.getId(),
                equipment.getNumber(),
                equipment.getName(),
                equipment.getType(),
                equipment.getCreatedAt(),
                equipment.getUpdatedAt());
    }
}
