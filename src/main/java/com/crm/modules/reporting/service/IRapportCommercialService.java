package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.ProprietaireResumeResponse;
import com.crm.modules.reporting.dto.RapportCommercialResponse;
import com.crm.shared.enums.PeriodeRapport;

import java.util.List;

/**
 * Contrat du service de reporting commercial automatisé (multi-tenant, sans JWT).
 *
 * @author Riahi Dorsaf
 * @see RapportCommercialService
 */
public interface IRapportCommercialService {

    /**
     * Liste les propriétaires actifs destinataires d'un rapport (énumération n8n).
     */
    List<ProprietaireResumeResponse> listerProprietairesActifs();

    /**
     * Génère le rapport commercial agrégé d'un propriétaire pour une période écoulée.
     *
     * @param proprietaireId identifiant du propriétaire
     * @param periode        SEMAINE | MOIS | ANNEE (période complète précédente)
     */
    RapportCommercialResponse genererRapport(Long proprietaireId, PeriodeRapport periode);

    /**
     * Régénère le rapport d'un propriétaire, y injecte la synthèse IA fournie, puis
     * l'envoie par email au propriétaire concerné.
     *
     * @param proprietaireId identifiant du propriétaire
     * @param periode        SEMAINE | MOIS | ANNEE (période complète précédente)
     * @param syntheseIa     synthèse rédigée par l'IA (optionnelle, ajoutée par n8n)
     * @return le rapport effectivement envoyé (synthèse IA incluse)
     */
    RapportCommercialResponse envoyerRapport(Long proprietaireId, PeriodeRapport periode, String syntheseIa);
}
