package com.crm.modules.reporting.service;

import com.crm.modules.client.repository.ClientRepository;
import com.crm.modules.reporting.dto.CaMensuelDto;
import com.crm.modules.reporting.mapper.ActiviteMapper;
import com.crm.modules.reporting.repository.ActiviteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.modules.vente.entity.Facture;
import com.crm.modules.vente.repository.DevisRepository;
import com.crm.modules.vente.repository.FactureRepository;
import com.crm.modules.vente.repository.LeadRepository;
import com.crm.modules.vente.repository.OpportuniteRepository;
import com.crm.shared.enums.StatutFacture;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link ReportingService} — résolution du propriétaire par e-mail et
 * agrégation du chiffre d'affaires (factures payées).
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ProprietaireRepository proprietaireRepository;
    @Mock private ActiviteRepository activiteRepository;
    @Mock private ActiviteMapper activiteMapper;
    @Mock private OpportuniteRepository opportuniteRepository;
    @Mock private DevisRepository devisRepository;
    @Mock private FactureRepository factureRepository;
    @Mock private LeadRepository leadRepository;

    @InjectMocks
    private ReportingService reportingService;

    private ProprietaireEntreprise proprietaire;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);
    }

    private Facture facturePayee(String ttc) {
        return Facture.builder().montantTtc(new BigDecimal(ttc)).statut(StatutFacture.PAYEE).build();
    }

    @Test
    @DisplayName("CA mois précédent = somme des TTC des factures payées")
    void caMoisPrecedent_somme() {
        when(proprietaireRepository.findByEmail("owner@ex.com")).thenReturn(Optional.of(proprietaire));
        when(factureRepository.findByProprietaireIdAndStatutAndDatePaiementBetween(
                eq(1L), eq(StatutFacture.PAYEE), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(facturePayee("100.000"), facturePayee("50.000")));

        BigDecimal ca = reportingService.getChiffreAffairesMoisPrecedent("owner@ex.com");

        assertThat(ca).isEqualByComparingTo("150.000");
    }

    @Test
    @DisplayName("CA par mois : renvoie 12 points")
    void caParMois_douzePoints() {
        when(proprietaireRepository.findByEmail("owner@ex.com")).thenReturn(Optional.of(proprietaire));
        when(factureRepository.findByProprietaireIdAndStatutAndDatePaiementBetween(
                eq(1L), eq(StatutFacture.PAYEE), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<CaMensuelDto> serie = reportingService.getCaParMois("owner@ex.com");

        assertThat(serie).hasSize(12);
    }

    @Test
    @DisplayName("propriétaire introuvable → ResourceNotFoundException")
    void proprietaireIntrouvable_leve() {
        when(proprietaireRepository.findByEmail("x@ex.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportingService.getChiffreAffairesMoisPrecedent("x@ex.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
