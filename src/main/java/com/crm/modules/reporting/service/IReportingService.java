package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.ActiviteResponse;
import com.crm.modules.reporting.dto.ReportingKpisResponse;
import com.crm.shared.response.PageResponse;

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
     * Retourne la liste paginée de toutes les activités du propriétaire.
     *
     * @param emailProprietaire email extrait du token JWT
     * @param page              numéro de page (0-based)
     * @param size              taille de page
     */
    PageResponse<ActiviteResponse> getActivites(String emailProprietaire, int page, int size);
}