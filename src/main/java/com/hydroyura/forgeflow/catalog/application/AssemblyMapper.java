package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Маппер доменной {@link Assembly} в исходящий DTO {@link AssemblyView}.
 */
@Mapper
public interface AssemblyMapper {

    AssemblyMapper INSTANCE = Mappers.getMapper(AssemblyMapper.class);

    AssemblyView toView(Assembly assembly);
}
