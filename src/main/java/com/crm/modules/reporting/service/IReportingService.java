package com.crm.modules.reporting.service;

import com.crm.modules.reporting.dto.ReportingKpisResponse;

/**
 * Contrat du service de reporting.
 * Agrège les indicateurs de performance de l'application CRM.
 *
 * @author Riahi Dorsaf
 * @see ReportingService
 */
public interface IReportingService {

    /**
     * Retourne les KPIs du tableau de bord pour le propriétaire connecté.
     *
     * @param emailProprietaire email extrait du token JWT
     * @return les indicateurs de performance
     */
    ReportingKpisResponse getKpis(String emailProprietaire);
}