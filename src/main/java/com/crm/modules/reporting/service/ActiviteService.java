package com.crm.modules.reporting.service;

import com.crm.modules.reporting.entity.Activite;
import com.crm.modules.reporting.repository.ActiviteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.shared.enums.TypeActivite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'enregistrement des activités CRM.
 *
 * @author Riahi Dorsaf
 * @see IActiviteService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActiviteService implements IActiviteService {

    private final ActiviteRepository activiteRepository;

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(TypeActivite type,
                            String titre,
                            String description,
                            Long entiteId,
                            String entiteType,
                            Long entiteParentId,
                            ProprietaireEntreprise proprietaire) {
        try {
            Activite activite = Activite.builder()
                    .type(type)
                    .titre(titre)
                    .description(description)
                    .entiteId(entiteId)
                    .entiteType(entiteType)
                    .entiteParentId(entiteParentId)
                    .proprietaire(proprietaire)
                    .build();
            activiteRepository.save(activite);
            log.debug("[ACTIVITE] Enregistrée — type={} entiteId={}", type, entiteId);
        } catch (Exception e) {
            log.warn("[ACTIVITE] Échec enregistrement — type={} : {}", type, e.getMessage());
        }
    }
}