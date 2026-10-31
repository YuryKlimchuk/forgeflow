package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.BuyView;
import com.hydroyura.forgeflow.catalog.domain.buy.Buy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Маппер доменного {@link Buy} в исходящий DTO {@link BuyView}.
 */
@Mapper
public interface BuyMapper {

    BuyMapper INSTANCE = Mappers.getMapper(BuyMapper.class);

    BuyView toView(Buy buy);
}
