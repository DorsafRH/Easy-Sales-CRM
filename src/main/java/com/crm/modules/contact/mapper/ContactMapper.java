package com.crm.modules.contact.mapper;

import com.crm.modules.contact.dto.ContactResponse;
import com.crm.modules.contact.entity.Contact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper : {@link Contact} → {@link ContactResponse}.
 *
 * <p>{@code nomComplet} est calculé via l'expression Java {@link Contact#getNomComplet()}.
 * {@code clientId} et {@code clientNomAffichage} sont mappés depuis la relation
 * {@code client}.</p>
 *
 * @author Riahi Dorsaf
 */
@Mapper(componentModel = "spring")
public interface ContactMapper {

    @Mapping(target = "nomComplet", expression = "java(contact.getNomComplet())")
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientNomAffichage", source = "client.nomAffichage")
    ContactResponse toResponse(Contact contact);
}