package com.crm.modules.marketing.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Page de réseau social connectée, exposée à l'orchestrateur n8n pour la collecte
 * d'engagement (machine-à-machine). Évite que n8n stocke les jetons.
 *
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class PageConnecteeDTO {
    private String pageId;
    private String accessToken;
    private Long proprietaireId;
}
