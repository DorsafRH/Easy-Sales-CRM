package com.crm.modules.catalogue.mapper;

import com.crm.modules.catalogue.dto.CategorieResponse;
import com.crm.modules.catalogue.entity.Categorie;
import com.crm.modules.catalogue.service.CategorieService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper : {@link Categorie} → {@link CategorieResponse}.
 *
 * <p>{@code nbProduits} est ignoré ici et valorisé dans
 * {@link CategorieService}
 * via {@code ProduitRepository.countByCategorieIdAndStatut}.</p>
 *
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface CatalogueMapper {

    @Mapping(target = "nbProduits", ignore = true)
    CategorieResponse toResponse(Categorie categorie);
}