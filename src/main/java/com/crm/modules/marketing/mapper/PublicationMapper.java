package com.crm.modules.marketing.mapper;

import com.crm.modules.marketing.dto.request.PublicationRequestDTO;
import com.crm.modules.marketing.dto.response.PublicationResponseDTO;
import com.crm.modules.marketing.entity.PublicationMarketing;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring", uses = {DiffusionMapper.class})
public interface PublicationMapper {

    @Mapping(target = "dateCreation", expression = "java(publication.getDateCreation() != null ? publication.getDateCreation().toString() : null)")
    @Mapping(target = "dateProgrammation", expression = "java(publication.getDateProgrammation() != null ? publication.getDateProgrammation().toString() : null)")
    @Mapping(target = "datePublication", expression = "java(publication.getDatePublication() != null ? publication.getDatePublication().toString() : null)")
    PublicationResponseDTO toResponse(PublicationMarketing publication);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "datePublication", ignore = true)
    @Mapping(target = "contenuGenereIa", ignore = true)
    @Mapping(target = "messageErreur", ignore = true)
    @Mapping(target = "proprietaire", ignore = true)
    @Mapping(target = "diffusions", ignore = true)
    void updateFromRequest(PublicationRequestDTO request, @MappingTarget PublicationMarketing publication);
}
