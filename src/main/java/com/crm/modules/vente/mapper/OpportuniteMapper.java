package com.crm.modules.vente.mapper;

import com.crm.modules.vente.dto.OpportuniteResponse;
import com.crm.modules.vente.entity.Opportunite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface OpportuniteMapper {

    @Mapping(target = "clientId",          source = "client.id")
    @Mapping(target = "clientNom",         source = "client.nomAffichage")
    @Mapping(target = "leadId",            source = "lead.id")
    @Mapping(target = "leadNom",           source = "lead.nom")
    @Mapping(target = "dateCloturePrevue", expression = "java(o.getDateCloturePrevue() != null ? o.getDateCloturePrevue().toString() : null)")
    @Mapping(target = "dateCreation",      expression = "java(o.getDateCreation()      != null ? o.getDateCreation().toString()      : null)")
    @Mapping(target = "dateModification",  expression = "java(o.getDateModification()  != null ? o.getDateModification().toString()  : null)")
    @Mapping(target = "dateRelative",      ignore = true)
    OpportuniteResponse toResponse(Opportunite o);
}