package com.crm.modules.marketing.service;

import com.crm.modules.marketing.dto.request.ReactionsSeedRequestDTO;
import com.crm.modules.marketing.dto.response.MarketingOverviewResponseDTO;
import com.crm.modules.marketing.dto.response.StatistiquesPublicationDTO;
import com.crm.modules.marketing.dto.response.TopPostReactionsDTO;

import java.util.List;

/**
 * Statistiques du module marketing : leads issus des réseaux sociaux, publications
 * et engagement (réactions). Sert le dashboard mobile (lecture, JWT) et reçoit
 * l'engagement public collecté/injecté (écriture, machine-à-machine).
 *
 * @author Riahi Dorsaf
 */
public interface IMarketingStatsService {

    /** Vue d'ensemble du dashboard pour le propriétaire courant. */
    MarketingOverviewResponseDTO getOverview(Long proprietaireId);

    /** Publications les plus engageantes du propriétaire. */
    List<TopPostReactionsDTO> getTopPosts(Long proprietaireId);

    /** Enregistre (ou met à jour) l'engagement public des publications d'une page. */
    void upsertReactions(ReactionsSeedRequestDTO request);

    /** Statistiques Facebook d'une publication PUBLIEE (vues, engagement, courbe). */
    StatistiquesPublicationDTO getStatistiquesPublication(Long publicationId, Long proprietaireId);
}
