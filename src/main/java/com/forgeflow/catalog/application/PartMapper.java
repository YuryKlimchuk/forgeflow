package com.forgeflow.catalog.application;

import com.forgeflow.catalog.api.PartView;
import com.forgeflow.catalog.domain.part.Part;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Маппер доменной {@link Part} в исходящий DTO {@link PartView}.
 */
@Mapper
public interface PartMapper {

    PartMapper INSTANCE = Mappers.getMapper(PartMapper.class);

    PartView toView(Part part);
}
