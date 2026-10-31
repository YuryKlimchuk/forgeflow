package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.VzkView;
import com.hydroyura.forgeflow.catalog.domain.vzk.Vzk;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Маппер доменного {@link Vzk} в исходящий DTO {@link VzkView}.
 */
@Mapper
public interface VzkMapper {

    VzkMapper INSTANCE = Mappers.getMapper(VzkMapper.class);

    VzkView toView(Vzk vzk);
}
