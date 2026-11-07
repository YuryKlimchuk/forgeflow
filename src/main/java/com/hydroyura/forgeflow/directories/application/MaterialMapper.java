package com.hydroyura.forgeflow.directories.application;

import com.hydroyura.forgeflow.directories.api.MaterialView;
import com.hydroyura.forgeflow.directories.domain.material.Material;

/**
 * Маппер материала в публичное представление.
 */
public class MaterialMapper {

    public MaterialView toView(Material material) {
        return new MaterialView(
                material.getId(),
                material.getNumber(),
                material.getName(),
                material.getStandardType(),
                material.getStandard(),
                material.getUnit(),
                material.getCreatedAt(),
                material.getUpdatedAt());
    }
}
