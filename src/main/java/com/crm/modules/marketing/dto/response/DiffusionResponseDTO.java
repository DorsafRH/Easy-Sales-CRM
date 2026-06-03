package com.crm.modules.marketing.dto.response;

import com.crm.shared.enums.StatutDiffusion;
import lombok.Builder;
import lombok.Data;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class DiffusionResponseDTO {
    private Long id;
    private StatutDiffusion statutDiffusion;
    private String dateDiffusion;
    private String messageErreur;
    private String urlPublication;
    private CompteSocialResponseDTO compteSocial;
}
