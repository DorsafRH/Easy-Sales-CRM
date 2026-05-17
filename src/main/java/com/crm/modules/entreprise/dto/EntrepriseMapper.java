package com.crm.modules.entreprise.dto;

import com.crm.modules.entreprise.entity.EntrepriseCompte;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper pour EntrepriseCompte ↔ EntrepriseCompteResponse.
 */
@Mapper(componentModel = "spring")
public interface EntrepriseMapper {

    /**
     * Conversion EntrepriseCompte → EntrepriseCompteResponse.
     * Les infos du propriétaire sont passées séparément via la méthode dédiée.
     */
    @Mapping(target = "proprietaireNom", ignore = true)
    @Mapping(target = "proprietairePrenom", ignore = true)
    @Mapping(target = "proprietaireEmail", ignore = true)
    @Mapping(target = "proprietaireTelephone", ignore = true)
    EntrepriseCompteResponse toResponse(EntrepriseCompte entreprise);

    /**
     * Conversion complète avec les informations du propriétaire.
     */
    default EntrepriseCompteResponse toResponseWithProprietaire(
            EntrepriseCompte entreprise,
            ProprietaireEntreprise proprietaire
    ) {
        EntrepriseCompteResponse response = toResponse(entreprise);
        if (proprietaire != null) {
            response.setProprietaireNom(proprietaire.getNom());
            response.setProprietairePrenom(proprietaire.getPrenom());
            response.setProprietaireEmail(proprietaire.getEmail());
            response.setProprietaireTelephone(proprietaire.getTelephone());
        }
        return response;
    }
}
