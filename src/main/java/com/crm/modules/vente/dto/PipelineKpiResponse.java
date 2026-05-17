package com.crm.modules.vente.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * KPIs du dashboard commercial VentesHome.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class PipelineKpiResponse {
    private Long nbLeads;
    private Long nbOpportunites;
    private BigDecimal montantPipeline;
    private BigDecimal montantGagne;
    private Long nbDevisEnCours;
    private Long nbFacturesImpayees;
    /**
     * opportunités par statut ex: {"PROSPECTION":3, "QUALIFICATION":2}
     */
    private Map<String, Long> repartitionStatuts;
}