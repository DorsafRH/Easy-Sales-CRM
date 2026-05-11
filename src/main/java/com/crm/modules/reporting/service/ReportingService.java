package com.crm.modules.reporting.service;

import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.reporting.dto.ActiviteResponse;
import com.crm.modules.reporting.dto.ReportingKpisResponse;
import com.crm.modules.reporting.dto.ReportingKpisResponse.ActiviteRecenteItem;
import com.crm.modules.reporting.entity.Activite;
import com.crm.modules.reporting.mapper.ActiviteMapper;
import com.crm.modules.reporting.repository.ActiviteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.repository.DevisRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutDevis;
import com.crm.shared.exception.ResourceNotFoundException;
import com.crm.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/**
 * Implémentation du service de reporting.
 * Sprint 3 : KPIs enrichis avec opportunités et devis réels.
 *
 * @author Riahi Dorsaf
 * @see IReportingService
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService implements IReportingService {

    private final ClientRepository       clientRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final ActiviteRepository     activiteRepository;
    private final ActiviteMapper         activiteMapper;
    private final OpportuniteRepository  opportuniteRepository;
    private final DevisRepository        devisRepository;

    // ─────────────────────────────────────────────────────────
    //  KPIs
    // ─────────────────────────────────────────────────────────

    @Override
    public ReportingKpisResponse getKpis(String emailProprietaire, String periode) {

        ProprietaireEntreprise proprietaire = chargerProprietaire(emailProprietaire);
        long proprietaireId = proprietaire.getId();

        LocalDateTime since = resolverPeriode(periode);

        long nbClients = (since == null)
                ? clientRepository.countByProprietaireIdAndIsDeletedFalseAndStatut(
                proprietaireId, "ACTIF")
                : clientRepository.countByProprietaireIdAndIsDeletedFalseAndStatutAndDateCreationAfter(
                proprietaireId, "ACTIF", since);

        long nbOpportunites = opportuniteRepository.countByProprietaireId(proprietaireId);

        long nbDevis = devisRepository.countByProprietaireIdAndStatut(
                proprietaireId, StatutDevis.ENVOYE);

        List<Activite> activites = activiteRepository
                .findTop10ByProprietaireIdOrderByDateCreationDesc(proprietaireId);

        List<ActiviteRecenteItem> activiteRecente = activites.stream()
                .map(a -> ActiviteRecenteItem.builder()
                        .id(a.getEntiteId())
                        .type(a.getEntiteType())
                        .typeActivite(a.getType().name())
                        .titre(a.getTitre())
                        .soustitre(a.getDescription())
                        .dateRelative(dateRelative(a.getDateCreation()))
                        .entiteParentId(a.getEntiteParentId())
                        .build())
                .toList();

        return ReportingKpisResponse.builder()
                .nbClients(nbClients)
                .nbOpportunites(nbOpportunites)
                .chiffreAffaires(BigDecimal.ZERO)
                .nbDevis(nbDevis)
                .sparkline(List.of(0, 0, 0, 0, 0, 0, 0))
                .activiteRecente(activiteRecente)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    //  LISTE ACTIVITÉS PAGINÉE
    // ─────────────────────────────────────────────────────────

    @Override
    public PageResponse<ActiviteResponse> getActivites(String emailProprietaire,
                                                       int page, int size) {
        ProprietaireEntreprise proprietaire = chargerProprietaire(emailProprietaire);

        Page<ActiviteResponse> result = activiteRepository
                .findByProprietaireIdOrderByDateCreationDesc(
                        proprietaire.getId(), PageRequest.of(page, size))
                .map(a -> {
                    ActiviteResponse response = activiteMapper.toResponse(a);
                    response.setDateRelative(dateRelative(a.getDateCreation()));
                    return response;
                });

        return PageResponse.from(result);
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS PRIVÉS
    // ─────────────────────────────────────────────────────────

    private LocalDateTime resolverPeriode(String periode) {
        if (periode == null) return null;
        return switch (periode) {
            case "AUJOURD_HUI" -> LocalDate.now().atStartOfDay();
            case "CE_MOIS"     -> LocalDate.now().withDayOfMonth(1).atStartOfDay();
            case "CETTE_ANNEE" -> LocalDate.now().withDayOfYear(1).atStartOfDay();
            default            -> null;
        };
    }

    private ProprietaireEntreprise chargerProprietaire(String email) {
        return proprietaireRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable."));
    }

    private String dateRelative(LocalDateTime date) {
        if (date == null) return "";
        Duration d = Duration.between(date, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 1)  return "a l instant";
        if (minutes < 60) return "il y a " + minutes + " min";
        long heures = d.toHours();
        if (heures < 24)  return "il y a " + heures + " h";
        long jours = d.toDays();
        if (jours < 30)   return "il y a " + jours + " j";
        return "il y a " + (jours / 30) + " mois";
    }
}