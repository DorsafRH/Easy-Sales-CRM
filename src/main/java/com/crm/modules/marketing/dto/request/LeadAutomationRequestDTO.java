package com.crm.modules.marketing.dto.request;

import com.crm.shared.enums.SourceLead;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Lead qualifié transmis par n8n (chatbot Messenger / commentaire) pour création
 * côté Spring. Le propriétaire est résolu à partir de {@code pageId} (multi-tenant) ;
 * le {@code nom} provient de l'API profil Messenger, l'email/téléphone des regex n8n,
 * le {@code score} et le {@code resume} du LLM.
 *
 * @author Riahi Dorsaf
 */
@Data
public class LeadAutomationRequestDTO {

    @NotBlank(message = "L'identifiant de page Meta est obligatoire")
    private String pageId;

    /** PSID Messenger de l'expéditeur (traçabilité/déduplication). */
    private String psid;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String email;

    private String telephone;

    @NotNull(message = "La source est obligatoire")
    private SourceLead source;

    @Min(value = 0, message = "Le score minimum est 0")
    @Max(value = 100, message = "Le score maximum est 100")
    private Integer score;

    /** Résumé du besoin produit par le LLM → descriptionBesoin. */
    private String resume;
}
