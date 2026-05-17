package com.crm.modules.vente.mapper;

import com.crm.modules.vente.dto.DevisResponse;
import com.crm.modules.vente.dto.LigneDevisResponse;
import com.crm.modules.vente.entity.Devis;
import com.crm.modules.vente.entity.LigneDevis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface DevisMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNom", source = "client.nomAffichage")
    @Mapping(target = "opportuniteId", source = "opportunite.id")
    @Mapping(target = "opportuniteTitre", source = "opportunite.titre")
    @Mapping(target = "dateCreation", expression = "java(d.getDateCreation()    != null ? d.getDateCreation().toString()    : null)")
    @Mapping(target = "dateModification", expression = "java(d.getDateModification()!= null ? d.getDateModification().toString() : null)")
    @Mapping(target = "dateRelative", ignore = true)
    DevisResponse toResponse(Devis d);

    @Mapping(target = "produitId", source = "produit.id")
    @Mapping(target = "produitNom", source = "produit.nom")
    LigneDevisResponse toLigneResponse(LigneDevis ligne);
}