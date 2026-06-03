package com.crm.modules.marketing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Data
public class GenererContenuRequestDTO {

    @NotBlank(message = "Le sujet est obligatoire")
    private String sujet;

    @NotBlank(message = "Le type de contenu est obligatoire")
    private String typeContenu;

    @NotBlank(message = "La tonalité est obligatoire")
    private String tonalite;

    @NotBlank(message = "La langue est obligatoire")
    private String langue;

    private List<String> motsCles;
}
