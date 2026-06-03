package com.crm.modules.marketing.dto.response;

import com.crm.shared.enums.StatutPublication;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Data
@Builder
public class PublicationResponseDTO {
    private Long id;
    private String titre;
    private String texte;
    private String mediaUrl;
    private StatutPublication statut;
    private String dateCreation;
    private String dateProgrammation;
    private String datePublication;
    private List<DiffusionResponseDTO> diffusions;
}
