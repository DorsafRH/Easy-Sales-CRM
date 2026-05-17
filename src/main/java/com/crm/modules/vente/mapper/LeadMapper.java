package com.crm.modules.vente.mapper;

import com.crm.modules.vente.dto.LeadResponse;
import com.crm.modules.vente.entity.Lead;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNom", source = "client.nomAffichage")
    @Mapping(target = "dateCreation", expression = "java(lead.getDateCreation()  != null ? lead.getDateCreation().toString()  : null)")
    @Mapping(target = "dateModification", expression = "java(lead.getDateModification() != null ? lead.getDateModification().toString() : null)")
    @Mapping(target = "dateRelative", ignore = true)
    LeadResponse toResponse(Lead lead);
}