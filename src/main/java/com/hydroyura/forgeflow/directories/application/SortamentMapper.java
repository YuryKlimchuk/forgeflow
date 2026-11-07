package com.hydroyura.forgeflow.directories.application;

import com.hydroyura.forgeflow.directories.api.SortamentView;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;

/**
 * Маппер сортамента в публичное представление.
 */
public class SortamentMapper {

    public SortamentView toView(Sortament sortament) {
        return new SortamentView(
                sortament.getId(),
                sortament.getNumber(),
                sortament.getName(),
                sortament.getStandardType(),
                sortament.getStandard(),
                sortament.getUnit(),
                sortament.getCreatedAt(),
                sortament.getUpdatedAt());
    }
}
