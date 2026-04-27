package com.crm.modules.client.mapper;

import com.crm.modules.client.dto.ClientResponse;
import com.crm.modules.client.entity.Client;
import com.crm.modules.client.entity.ClientEntreprise;
import com.crm.modules.client.entity.ClientIndividuel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper : Client → ClientResponse.
 *
 * Les champs calculés (nbContacts, chiffreAffaires) sont ignorés ici
 * et valorisés manuellement dans le service après le mapping.
 *
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface ClientMapper {

    // ── ClientIndividuel ──────────────────────────────────────────────────────

    @Mapping(target = "typeClient",      constant = "INDIVIDUEL")
    @Mapping(target = "raisonSociale",   ignore = true)
    @Mapping(target = "nbContacts",      ignore = true)
    @Mapping(target = "chiffreAffaires", ignore = true)
    ClientResponse toResponse(ClientIndividuel client);

    // ── ClientEntreprise ──────────────────────────────────────────────────────

    @Mapping(target = "typeClient",      constant = "ENTREPRISE")
    @Mapping(target = "nom",             ignore = true)
    @Mapping(target = "prenom",          ignore = true)
    @Mapping(target = "nbContacts",      ignore = true)
    @Mapping(target = "chiffreAffaires", ignore = true)
    ClientResponse toResponse(ClientEntreprise client);

    // ── Dispatch polymorphique ────────────────────────────────────────────────

    /**
     * Résout le type concret et délègue au bon mapper.
     *
     * @param client instance abstraite
     * @return ClientResponse correctement typé
     */
    default ClientResponse toResponse(Client client) {
        if (client instanceof ClientIndividuel individuel) {
            return toResponse(individuel);
        }
        if (client instanceof ClientEntreprise entreprise) {
            return toResponse(entreprise);
        }
        throw new IllegalArgumentException(
                "Type client inconnu : " + client.getClass().getSimpleName());
    }
}