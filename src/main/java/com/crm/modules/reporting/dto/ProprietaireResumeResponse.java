package com.crm.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Résumé minimal d'un propriétaire actif, pour l'énumération multi-tenant par n8n.
 *
 * <p>N'expose que l'identifiant et l'email (principe de moindre privilège) : n8n boucle
 * sur cette liste pour générer puis diffuser à chacun SON rapport.</p>
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class ProprietaireResumeResponse {
    private Long   id;
    private String email;
}
