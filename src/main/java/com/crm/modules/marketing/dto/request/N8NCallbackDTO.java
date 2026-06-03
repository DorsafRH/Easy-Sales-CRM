package com.crm.modules.marketing.dto.request;

import lombok.Data;

/**
 * Payload reçu depuis le workflow N8N après tentative de publication.
 *
 * @author Riahi Dorsaf
 */
@Data
public class N8NCallbackDTO {
    private Long publicationId;
    private Long compteSocialId;
    private String statut;
    private String idPublicationExt;
    private String urlPublication;
    private String messageErreur;
}
