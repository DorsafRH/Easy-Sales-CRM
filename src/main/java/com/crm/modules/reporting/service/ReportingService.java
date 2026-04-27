package com.crm.modules.reporting.service;

import com.crm.modules.client.entity.Client;
import com.crm.modules.client.entity.ClientEntreprise;
import com.crm.modules.client.entity.ClientIndividuel;
import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.reporting.dto.ReportingKpisResponse;
import com.crm.modules.reporting.dto.ReportingKpisResponse.ActiviteRecenteItem;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service métier — Reporting.
 * Agrège les indicateurs de performance pour le tableau de bord mobile.
 *
 * Sprint 2 : nbClients réel, autres KPIs à 0 (câblés en Sprint 3).
 * Méthodes dérivées uniquement, aucun @Query.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService implements IReportingService {

    private final ClientRepository       clientRepository;
    private final ProprietaireRepository proprietaireRepository;

    @Override
    public ReportingKpisResponse getKpis(String emailProprietaire) {

        ProprietaireEntreprise proprietaire = proprietaireRepository
                .findByEmail(emailProprietaire)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable."));

        long proprietaireId = proprietaire.getId();

        // ── KPI réel Sprint 2 ─────────────────────────────────────────────
        long nbClients = clientRepository
                .countByProprietaireIdAndIsDeletedFalseAndStatut(proprietaireId, "ACTIF");

        // ── Activité récente — 5 derniers clients créés ───────────────────
        List<Client> recents = clientRepository
                .findTop5ByProprietaireIdAndIsDeletedFalseOrderByDateCreationDesc(
                        proprietaireId);

        List<ActiviteRecenteItem> activite = recents.stream()
                .map(c -> ActiviteRecenteItem.builder()
                        .id(c.getId())
                        .type("CLIENT")
                        .titre(c.getNomAffichage())
                        .soustitre(resolverTypeClient(c))
                        .dateRelative(dateRelative(c.getDateCreation()))
                        .build())
                .toList();

        return ReportingKpisResponse.builder()
                .nbClients(nbClients)
                .nbOpportunites(0L)
                .chiffreAffaires(BigDecimal.ZERO)
                .nbDevis(0L)
                .sparkline(List.of(0, 0, 0, 0, 0, 0, 0))
                .activiteRecente(activite)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Résout le libellé du type via instanceof.
     * Le discriminateur JPA n'est pas exposé comme champ public sur Client.
     */
    private String resolverTypeClient(Client client) {
        if (client instanceof ClientIndividuel) return "Individuel";
        if (client instanceof ClientEntreprise)  return "Entreprise";
        return "Client";
    }

    private String dateRelative(LocalDateTime date) {
        if (date == null) return "";
        Duration d = Duration.between(date, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 1)  return "à l'instant";
        if (minutes < 60) return "il y a " + minutes + " min";
        long heures = d.toHours();
        if (heures < 24)  return "il y a " + heures + " h";
        long jours = d.toDays();
        if (jours < 30)   return "il y a " + jours + " j";
        return "il y a " + (jours / 30) + " mois";
    }
}