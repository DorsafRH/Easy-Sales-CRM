package com.crm.modules.reporting.dto;

import com.crm.shared.enums.TypeActivite;
import lombok.Builder;
import lombok.Data;

/**
 * DTO représentant une activité dans le journal CRM.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class ActiviteResponse {

    private Long         id;
    private TypeActivite type;
    private String       titre;
    private String       description;
    private Long         entiteId;
    private String       entiteType;

    /**
     * ID de l'entité parente — optionnel.
     * Pour les contacts : contient le clientId pour la navigation mobile.
     */
    private Long         entiteParentId;

    private String       dateRelative;
    private String       dateCreation;
}