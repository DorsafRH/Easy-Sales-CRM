package com.crm.modules.marketing.scheduler;

import com.crm.modules.marketing.service.IMarketingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Déclenche périodiquement la diffusion des publications programmées.
 * Remplace les anciens triggers Schedule de N8N pour la publication.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicationScheduler {

    private final IMarketingService marketingService;

    /**
     * Toutes les 60 s : publie les publications PROGRAMMEE dont la date est échue.
     */
    @Scheduled(fixedDelay = 60_000)
    public void diffuserPublicationsProgrammees() {
        try {
            marketingService.publierPublicationsProgrammees();
        } catch (Exception e) {
            log.error("[SCHEDULER] Erreur lors de la diffusion des publications programmées : {}",
                    e.getMessage());
        }
    }
}
