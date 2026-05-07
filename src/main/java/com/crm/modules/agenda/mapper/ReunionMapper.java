package com.crm.modules.agenda.mapper;

import com.crm.modules.agenda.dto.ReunionParticipantDto;
import com.crm.modules.agenda.dto.ReunionResponse;
import com.crm.modules.agenda.entity.Reunion;
import com.crm.modules.agenda.entity.ReunionParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct pour la conversion {@link Reunion} → {@link ReunionResponse}.
 *
 * <p><b>Champs calculés dynamiquement par le service</b> (ignorés ici) :</p>
 * <ul>
 *   <li>{@code dateRelative} — calculé à partir de {@code dateHeure}</li>
 *   <li>{@code clientNom}    — résolu depuis {@code client.nomAffichage}</li>
 * </ul>
 *
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface ReunionMapper {

    /**
     * Convertit une entité {@link Reunion} en {@link ReunionResponse}.
     *
     * @param reunion entité à convertir
     * @return DTO de réponse
     */
    @Mapping(target = "clientId",    source = "client.id")
    @Mapping(target = "clientNom",   ignore = true)
    @Mapping(target = "dateHeure",   expression = "java(reunion.getDateHeure() != null ? reunion.getDateHeure().toString() : null)")
    @Mapping(target = "dateCreation",expression = "java(reunion.getDateCreation() != null ? reunion.getDateCreation().toString() : null)")
    @Mapping(target = "dateRelative",ignore = true)
    ReunionResponse toResponse(Reunion reunion);

    /**
     * Convertit un {@link ReunionParticipant} en {@link ReunionParticipantDto}.
     *
     * @param participant entité embarquée à convertir
     * @return DTO participant
     */
    ReunionParticipantDto toParticipantDto(ReunionParticipant participant);

    /**
     * Convertit un {@link ReunionParticipantDto} en {@link ReunionParticipant}.
     *
     * @param dto DTO à convertir
     * @return entité embarquée
     */
    ReunionParticipant toParticipant(ReunionParticipantDto dto);
}