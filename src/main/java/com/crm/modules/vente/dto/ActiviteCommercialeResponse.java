package com.crm.modules.vente.dto;

import com.crm.shared.enums.ResultatActivite;
import com.crm.shared.enums.TypeActiviteCommerciale;
import lombok.Builder;
import lombok.Data;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class ActiviteCommercialeResponse {
    private Long                    id;
    private TypeActiviteCommerciale type;
    private String                  sujet;
    private String                  notes;
    private ResultatActivite        resultat;
    private Integer                 dureeMinutes;
    private String                  dateActivite;
    private String                  dateRelative;
    private Long                    leadId;
    private String                  leadNom;
    private Long                    opportuniteId;
    private String                  opportuniteTitre;
}