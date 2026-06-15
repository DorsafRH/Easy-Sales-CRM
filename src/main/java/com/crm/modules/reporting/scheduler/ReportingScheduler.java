package com.crm.modules.reporting.scheduler;

import com.crm.modules.reporting.service.IRapportCommercialService;
import com.crm.shared.enums.PeriodeRapport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Déclenche périodiquement la diffusion des rapports commerciaux à tous les
 * propriétaires actifs (hebdomadaire / mensuel / annuel).
 *
 * <p>Même approche que {@code PublicationScheduler} : 100 % backend Spring, sans n8n.
 * Désactivé par défaut ({@code reporting.scheduler.enabled=false}) pour ne pas se
 * déclencher pendant le développement ; à activer en production. Les expressions cron
 * sont externalisées (surchargables par variable d'environnement) et évaluées en
 * heure de Tunis.</p>
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "reporting.scheduler.enabled", havingValue = "true")
public class ReportingScheduler {

    private static final String ZONE = "Africa/Tunis";

    private final IRapportCommercialService rapportCommercialService;

    /** Rapport hebdomadaire : lundi à 8h00 (semaine précédente). */
    @Scheduled(cron = "${reporting.cron.hebdomadaire}", zone = ZONE)
    public void rapportHebdomadaire() {
        declencher(PeriodeRapport.SEMAINE);
    }

    /** Rapport mensuel : le 1er du mois à 8h00 (mois précédent). */
    @Scheduled(cron = "${reporting.cron.mensuel}", zone = ZONE)
    public void rapportMensuel() {
        declencher(PeriodeRapport.MOIS);
    }

    /** Rapport annuel : le 1er janvier à 8h00 (année précédente). */
    @Scheduled(cron = "${reporting.cron.annuel}", zone = ZONE)
    public void rapportAnnuel() {
        declencher(PeriodeRapport.ANNEE);
    }

    private void declencher(PeriodeRapport periode) {
        try {
            log.info("[REPORTING SCHEDULER] Déclenchement des rapports {}", periode);
            int envoyes = rapportCommercialService.envoyerTousLesRapports(periode);
            log.info("[REPORTING SCHEDULER] Rapports {} terminés : {} envoyé(s)", periode, envoyes);
        } catch (Exception e) {
            log.error("[REPORTING SCHEDULER] Erreur lors des rapports {} : {}",
                    periode, e.getMessage());
        }
    }
}
