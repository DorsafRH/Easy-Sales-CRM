package com.crm.modules.marketing.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class GenererContenuResponseDTO {
    private String contenuGenere;
    private String contenuAmeliore;
    private Integer tokensUtilises;
}
