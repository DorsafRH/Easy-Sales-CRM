package com.crm.modules.marketing.dto.request;

import com.crm.shared.enums.PorteePublication;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Requête de génération d'une publication Facebook par IA, pilotée par le catalogue.
 * L'utilisateur choisit une portée ; le backend récupère les données produit et calcule
 * les prix promo — le LLM ne fait que rédiger.
 *
 * @author Riahi Dorsaf
 */
@Data
public class GenererPublicationRequestDTO {

    @NotNull(message = "La portée est obligatoire")
    private PorteePublication portee;

    /** Identifiants des produits (si portée = PRODUITS). */
    private List<Long> produitIds;

    /** Identifiant de la catégorie (si portée = CATEGORIE). */
    private Long categorieId;

    /** Remise en pourcentage appliquée au calcul du prix promo (optionnel). */
    @Min(value = 0, message = "La remise ne peut pas être négative")
    @Max(value = 100, message = "La remise ne peut pas dépasser 100")
    private Integer remise;

    /** Consigne libre facultative (« nouvel arrivage », « promo été »…). */
    private String consigne;

    /** Tonalité du texte (professionnel par défaut). */
    private String tonalite;

    /** Langue du texte (fr par défaut). */
    private String langue;
}
