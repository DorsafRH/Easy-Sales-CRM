package com.crm.modules.marketing.mapper;

import com.crm.modules.marketing.dto.response.CompteSocialResponseDTO;
import com.crm.modules.marketing.entity.CompteSocialConnecte;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface CompteSocialMapper {

    @Mapping(target = "dateConnexion", expression = "java(compte.getDateConnexion() != null ? compte.getDateConnexion().toString() : null)")
    CompteSocialResponseDTO toResponse(CompteSocialConnecte compte);
}
