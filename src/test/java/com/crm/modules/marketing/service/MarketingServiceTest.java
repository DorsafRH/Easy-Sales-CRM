package com.crm.modules.marketing.service;

import com.crm.modules.catalogue.dto.ProduitResponse;
import com.crm.modules.catalogue.service.ICategorieService;
import com.crm.modules.catalogue.service.IProduitService;
import com.crm.modules.marketing.dto.request.AmeliorerContenuRequestDTO;
import com.crm.modules.marketing.dto.request.GenererContenuRequestDTO;
import com.crm.modules.marketing.dto.request.GenererPublicationRequestDTO;
import com.crm.modules.marketing.dto.response.GenererContenuResponseDTO;
import com.crm.modules.marketing.mapper.CompteSocialMapper;
import com.crm.modules.marketing.mapper.PublicationMapper;
import com.crm.modules.marketing.repository.CompteSocialConnecteRepository;
import com.crm.modules.marketing.repository.PublicationMarketingRepository;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.shared.enums.PorteePublication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link MarketingService} — génération de publication assistée par IA.
 * Point clé : le backend récupère les vraies données produit et calcule le prix promo ;
 * le LLM ne fait que rédiger. On capture le prompt transmis à Groq pour le prouver.
 *
 * @author Riahi Dorsaf
 */
@ExtendWith(MockitoExtension.class)
class MarketingServiceTest {

    @Mock private PublicationMarketingRepository publicationRepository;
    @Mock private CompteSocialConnecteRepository compteRepository;
    @Mock private ProprietaireRepository proprietaireRepository;
    @Mock private PublicationMapper publicationMapper;
    @Mock private CompteSocialMapper compteSocialMapper;
    @Mock private GroqService groqService;
    @Mock private FacebookPublicationService facebookPublicationService;
    @Mock private IProduitService produitService;
    @Mock private ICategorieService categorieService;

    @InjectMocks
    private MarketingService marketingService;

    @Test
    @DisplayName("genererPublication : injecte le nom du produit et le prix promo CALCULÉ dans le prompt")
    void genererPublication_ancrePrixEtNomDansLePrompt() {
        GenererPublicationRequestDTO req = new GenererPublicationRequestDTO();
        req.setPortee(PorteePublication.PRODUITS);
        req.setProduitIds(List.of(20L));
        req.setRemise(20);

        ProduitResponse produit = new ProduitResponse();
        produit.setNom("Développement Web");
        produit.setPrixTTC(new BigDecimal("100.000"));
        when(produitService.obtenirProduit(20L, 7L)).thenReturn(produit);

        GenererContenuResponseDTO reponse = GenererContenuResponseDTO.builder()
                .contenuAmeliore("post").build();
        when(groqService.genererContenu(any(GenererContenuRequestDTO.class))).thenReturn(reponse);

        GenererContenuResponseDTO resultat = marketingService.genererPublication(req, 7L);

        assertThat(resultat).isSameAs(reponse);

        ArgumentCaptor<GenererContenuRequestDTO> captor =
                ArgumentCaptor.forClass(GenererContenuRequestDTO.class);
        verify(groqService).genererContenu(captor.capture());
        GenererContenuRequestDTO delegue = captor.getValue();

        // Le prompt contient le vrai nom, le prix et le prix promo calculé (100 * (1-0,20) = 80)
        assertThat(delegue.getSujet())
                .contains("Développement Web")
                .contains("100 TND")
                .contains("-20%")
                .contains("80 TND");
        // Défauts appliqués
        assertThat(delegue.getTypeContenu()).isEqualTo("post_facebook");
        assertThat(delegue.getTonalite()).isEqualTo("professionnel");
        assertThat(delegue.getLangue()).isEqualTo("fr");
    }

    @Test
    @DisplayName("ameliorerContenu : délègue le raffinage à GroqService")
    void ameliorerContenu_delegueAGroq() {
        AmeliorerContenuRequestDTO req = new AmeliorerContenuRequestDTO();
        req.setTexte("Ancien texte");
        req.setConsigne("plus court");
        req.setTonalite("promotionnel");

        GenererContenuResponseDTO reponse = GenererContenuResponseDTO.builder()
                .contenuAmeliore("Nouveau texte").build();
        when(groqService.ameliorer("Ancien texte", "plus court", "promotionnel")).thenReturn(reponse);

        GenererContenuResponseDTO resultat = marketingService.ameliorerContenu(req);

        assertThat(resultat).isSameAs(reponse);
        verify(groqService).ameliorer("Ancien texte", "plus court", "promotionnel");
    }
}
