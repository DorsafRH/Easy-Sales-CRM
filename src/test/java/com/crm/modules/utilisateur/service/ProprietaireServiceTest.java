package com.crm.modules.utilisateur.service;

import com.crm.modules.notification.service.NotificationService;
import com.crm.modules.utilisateur.dto.ModifierEntrepriseRequest;
import com.crm.modules.utilisateur.dto.ModifierProfilRequest;
import com.crm.modules.utilisateur.dto.ProfilProprietaireResponse;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link ProprietaireService} — consultation et mise à jour du profil,
 * garde-fou « aucun compte entreprise associé ».
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class ProprietaireServiceTest {

    @Mock private ProprietaireRepository proprietaireRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ProprietaireService proprietaireService;

    private ProprietaireEntreprise proprietaire;

    @BeforeEach
    void setUp() {
        proprietaire = new ProprietaireEntreprise();
        proprietaire.setId(1L);
        proprietaire.setNom("Ancien");
        proprietaire.setEmail("owner@ex.com");
    }

    @Test
    @DisplayName("consulterProfil : renvoie le profil du propriétaire")
    void consulterProfil_ok() {
        when(proprietaireRepository.findByEmail("owner@ex.com")).thenReturn(Optional.of(proprietaire));

        ProfilProprietaireResponse res = proprietaireService.consulterProfil("owner@ex.com");

        assertThat(res.getEmail()).isEqualTo("owner@ex.com");
    }

    @Test
    @DisplayName("consulterProfil : email inconnu → ResourceNotFoundException")
    void consulterProfil_introuvable() {
        when(proprietaireRepository.findByEmail("x@ex.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proprietaireService.consulterProfil("x@ex.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("modifierProfil : met à jour les champs et sauvegarde")
    void modifierProfil_ok() {
        ModifierProfilRequest req = new ModifierProfilRequest();
        req.setNom("Nouveau");
        req.setPrenom("Prenom");
        req.setTelephone("22334455");
        when(proprietaireRepository.findByEmail("owner@ex.com")).thenReturn(Optional.of(proprietaire));

        proprietaireService.modifierProfil("owner@ex.com", req);

        assertThat(proprietaire.getNom()).isEqualTo("Nouveau");
        verify(proprietaireRepository).save(proprietaire);
    }

    @Test
    @DisplayName("modifierEntreprise sans compte entreprise → BusinessException")
    void modifierEntreprise_sansCompte_leve() {
        proprietaire.setEntrepriseCompte(null);
        when(proprietaireRepository.findByEmail("owner@ex.com")).thenReturn(Optional.of(proprietaire));

        assertThatThrownBy(() ->
                proprietaireService.modifierEntreprise("owner@ex.com", new ModifierEntrepriseRequest()))
                .isInstanceOf(BusinessException.class);
        verify(proprietaireRepository, never()).save(any());
    }
}
