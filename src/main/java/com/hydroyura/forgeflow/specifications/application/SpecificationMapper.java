package com.hydroyura.forgeflow.specifications.application;

import com.hydroyura.forgeflow.specifications.api.SpecificationRowView;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRow;

/**
 * Маппер строки спецификации в публичное представление.
 */
public class SpecificationMapper {

    public SpecificationRowView toView(SpecificationRow row) {
        return new SpecificationRowView(row.item(), row.quantity().value());
    }
}
