package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.ActiviteResponse;
import com.crm.modules.reporting.dto.CaMensuelDto;
import com.crm.modules.reporting.dto.ReportingKpisResponse;
import com.crm.modules.reporting.dto.StatsVentesResponse;
import com.crm.shared.response.PageResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contrat du service de reporting.
 *
 * @author Riahi Dorsaf
 * @see ReportingService
 */
public interface IReportingService {

    /**
     * Retourne les KPIs du tableau de bord filtrés par période.
     *
     * @param emailProprietaire email extrait du token JWT
     * @param periode           période de filtrage : AUJOURD_HUI | CE_MOIS | CETTE_ANNEE
     */
    ReportingKpisResponse getKpis(String emailProprietaire, String periode);

    /**
     * Retourne le chiffre d'affaires (factures PAYÉE) du mois précédent.
     *
     * @param emailProprietaire email extrait du token JWT
     */
    BigDecimal getChiffreAffairesMoisPrecedent(String emailProprietaire);

    /**
     * Retourne les statistiques de vente avancées pour le dashboard.
     * Inclut : leads actifs, taux de conversion, valeur pipeline,
     * panier moyen, répartition opportunités, top 3 opportunités.
     *
     * @param emailProprietaire email extrait du token JWT
     */
    StatsVentesResponse getStatsVentes(String emailProprietaire);

    /**
     * Variante de {@link #getStatsVentes(String)} ciblée par identifiant.
     * Utilisée par le reporting automatisé (n8n) qui dispose du proprietaireId
     * et non d'un JWT.
     *
     * @param proprietaireId identifiant du propriétaire
     */
    StatsVentesResponse getStatsVentes(Long proprietaireId);

    /**
     * Retourne le CA mensuel sur les 12 derniers mois glissants (factures PAYÉE).
     *
     * @param emailProprietaire email extrait du token JWT
     */
    List<CaMensuelDto> getCaParMois(String emailProprietaire);

    /**
     * Retourne la liste paginée de toutes les activités du propriétaire.
     *
     * @param emailProprietaire email extrait du token JWT
     * @param page              numéro de page (0-based)
     * @param size              taille de page
     */
    PageResponse<ActiviteResponse> getActivites(String emailProprietaire, int page, int size);
}