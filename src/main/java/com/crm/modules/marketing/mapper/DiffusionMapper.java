package com.crm.modules.marketing.mapper;

import com.crm.modules.marketing.dto.response.DiffusionResponseDTO;
import com.crm.modules.marketing.entity.DiffusionPublication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring", uses = {CompteSocialMapper.class})
public interface DiffusionMapper {

    @Mapping(target = "dateDiffusion", expression = "java(diffusion.getDateDiffusion() != null ? diffusion.getDateDiffusion().toString() : null)")
    DiffusionResponseDTO toResponse(DiffusionPublication diffusion);
}
