package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.StandartView;
import com.hydroyura.forgeflow.catalog.domain.standart.Standart;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Маппер доменного {@link Standart} в исходящий DTO {@link StandartView}.
 */
@Mapper
public interface StandartMapper {

    StandartMapper INSTANCE = Mappers.getMapper(StandartMapper.class);

    StandartView toView(Standart standart);
}
