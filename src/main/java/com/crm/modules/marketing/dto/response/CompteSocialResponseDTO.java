package com.crm.modules.marketing.dto.response;

import com.crm.shared.enums.TypeReseau;
import lombok.Builder;
import lombok.Data;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class CompteSocialResponseDTO {
    private Long id;
    private TypeReseau typeReseau;
    private String nomCompte;
    private String statutConnexion;
    private String dateConnexion;
}
