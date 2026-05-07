package com.crm.modules.reporting.mapper;

import com.crm.modules.reporting.dto.ActiviteResponse;
import com.crm.modules.reporting.entity.Activite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct pour la conversion {@link Activite} → {@link ActiviteResponse}.
 *
 * <p><b>Pourquoi MapStruct ?</b>
 * Le code de mapping est généré à la compilation — aucune réflexion à
 * l'exécution. Plus performant que ModelMapper et type-safe : si un champ
 * disparaît de l'entité, le projet ne compile plus.</p>
 *
 * <p>{@code componentModel = "spring"} permet à Spring d'injecter ce mapper
 * via {@code @Autowired} / {@code @RequiredArgsConstructor}.</p>
 *
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface ActiviteMapper {

    /**
     * Convertit une entité {@link Activite} en {@link ActiviteResponse}.
     *
     * <p>{@code dateRelative} est calculé dynamiquement dans le service —
     * il n'existe pas dans l'entité, donc on l'ignore ici et on le
     * renseigne manuellement après le mapping.</p>
     *
     * <p>{@code dateCreation} est formaté en ISO string pour l'affichage mobile.</p>
     */
    @Mapping(target = "dateRelative", ignore = true)
    @Mapping(target = "dateCreation", expression = "java(activite.getDateCreation() != null ? activite.getDateCreation().toString() : null)")
    ActiviteResponse toResponse(Activite activite);
}