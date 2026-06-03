package com.crm.modules.marketing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Data
public class PublicationRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    @NotBlank(message = "Le texte est obligatoire")
    private String texte;

    private String mediaUrl;

    private LocalDateTime dateProgrammation;

    private List<Long> comptesSociauxIds;
}
