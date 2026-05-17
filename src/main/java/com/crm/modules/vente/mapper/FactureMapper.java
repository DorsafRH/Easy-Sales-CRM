package com.crm.modules.vente.mapper;

import com.crm.modules.vente.dto.FactureResponse;
import com.crm.modules.vente.dto.LigneFactureResponse;
import com.crm.modules.vente.entity.Facture;
import com.crm.modules.vente.entity.LigneFacture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface FactureMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNom", source = "client.nomAffichage")
    @Mapping(target = "devisNumero", source = "devisOrigine.numero")
    @Mapping(target = "dateEcheance", expression = "java(f.getDateEcheance()  != null ? f.getDateEcheance().toString()  : null)")
    @Mapping(target = "dateEmission", expression = "java(f.getDateEmission()  != null ? f.getDateEmission().toString()  : null)")
    @Mapping(target = "datePaiement", expression = "java(f.getDatePaiement()  != null ? f.getDatePaiement().toString()  : null)")
    @Mapping(target = "dateCreation", expression = "java(f.getDateCreation()  != null ? f.getDateCreation().toString()  : null)")
    @Mapping(target = "dateRelative", ignore = true)
    FactureResponse toResponse(Facture f);

    LigneFactureResponse toLigneResponse(LigneFacture ligne);
}