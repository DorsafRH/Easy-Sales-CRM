package com.crm.modules.vente.service;

import com.crm.modules.reporting.service.IActiviteService;
import com.crm.modules.vente.dto.FactureResponse;
import com.crm.modules.vente.entity.Facture;
import com.crm.modules.vente.mapper.FactureMapper;
import com.crm.modules.vente.repository.FactureRepository;
import com.crm.shared.enums.StatutFacture;
import com.crm.shared.enums.TypeActivite;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FactureService implements IFactureService {

    private final FactureRepository factureRepository;
    private final FactureMapper factureMapper;
    private final IActiviteService activiteService;

    @Transactional(readOnly = true)
    @Override
    public List<FactureResponse> lister(Long proprietaireId, StatutFacture statut) {
        return factureRepository.findByProprietaireIdOrderByDateCreationDesc(proprietaireId)
                .stream()
                .filter(f -> statut == null || f.getStatut() == statut)
                .map(this::enrichir)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public FactureResponse obtenir(Long id, Long proprietaireId) {
        return enrichir(charger(id, proprietaireId));
    }

    @Override
    public FactureResponse changerStatut(Long id, StatutFacture statut, Long proprietaireId) {
        Facture facture = charger(id, proprietaireId);
        validerTransition(facture.getStatut(), statut);

        facture.setStatut(statut);
        if (statut == StatutFacture.EMISE) facture.setDateEmission(LocalDateTime.now());
        if (statut == StatutFacture.PAYEE) facture.setDatePaiement(LocalDateTime.now());

        facture = factureRepository.save(facture);

        TypeActivite type = switch (statut) {
            case EMISE -> TypeActivite.FACTURE_EMISE;
            case PAYEE -> TypeActivite.FACTURE_PAYEE;
            case ANNULEE -> TypeActivite.FACTURE_ANNULEE;
            default -> TypeActivite.FACTURE_CREEE;
        };

        activiteService.enregistrer(
                type, "Facture " + statut.name().toLowerCase(), facture.getNumero(),
                facture.getId(), "FACTURE", null, facture.getProprietaire());

        return enrichir(facture);
    }

    private Facture charger(Long id, Long proprietaireId) {
        return factureRepository.findByIdAndProprietaireId(id, proprietaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
    }

    private void validerTransition(StatutFacture actuel, StatutFacture nouveau) {
        boolean ok = switch (actuel) {
            case BROUILLON -> nouveau == StatutFacture.EMISE;
            case EMISE ->
                    nouveau == StatutFacture.PAYEE || nouveau == StatutFacture.ANNULEE || nouveau == StatutFacture.EN_RETARD;
            case EN_RETARD -> nouveau == StatutFacture.PAYEE || nouveau == StatutFacture.ANNULEE;
            default -> false;
        };
        if (!ok) throw new BusinessException("Transition invalide : " + actuel + " → " + nouveau);
    }

    private FactureResponse enrichir(Facture f) {
        FactureResponse r = factureMapper.toResponse(f);
        r.setLignes(f.getLignes().stream().map(factureMapper::toLigneResponse).toList());
        r.setDateRelative(dateRelative(f.getDateCreation()));
        return r;
    }

    private String dateRelative(LocalDateTime date) {
        if (date == null) return "";
        Duration d = Duration.between(date, LocalDateTime.now());
        long min = d.toMinutes();
        if (min < 1) return "à l'instant";
        if (min < 60) return "il y a " + min + " min";
        long h = d.toHours();
        if (h < 24) return "il y a " + h + "h";
        return "il y a " + d.toDays() + "j";
    }
}