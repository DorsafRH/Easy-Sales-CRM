package com.crm.modules.marketing.service;

import com.crm.modules.marketing.entity.CompteSocialConnecte;
import com.crm.modules.marketing.repository.CompteSocialConnecteRepository;
import com.crm.modules.utilisateur.entity.ProprietaireEntreprise;
import com.crm.modules.utilisateur.repository.ProprietaireRepository;
import com.crm.shared.enums.TypeReseau;
import com.crm.shared.exception.BusinessException;
import com.crm.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service OAuth Meta (Facebook / Instagram).
 * Génère l'URL d'autorisation, échange le code contre un token,
 * récupère les Pages gérées et sauvegarde les comptes connectés.
 *
 * @author Riahi Dorsaf
 */
@Slf4j
@Service
public class MetaOAuthService {

    private static final String SCOPES =
            "pages_manage_posts,pages_read_engagement,instagram_content_publish,"
                    + "instagram_basic,email,public_profile";

    private final RestClient restClient = RestClient.create();
    private final Map<String, Long> stateVersProprietaire = new ConcurrentHashMap<>();

    private final CompteSocialConnecteRepository compteRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final String appId;
    private final String appSecret;
    private final String redirectUri;
    private final String graphApiVersion;

    public MetaOAuthService(CompteSocialConnecteRepository compteRepository,
                            ProprietaireRepository proprietaireRepository,
                            @Value("${meta.oauth.app-id}") String appId,
                            @Value("${meta.oauth.app-secret}") String appSecret,
                            @Value("${meta.oauth.redirect-uri}") String redirectUri,
                            @Value("${meta.oauth.graph-api-version}") String graphApiVersion) {
        this.compteRepository = compteRepository;
        this.proprietaireRepository = proprietaireRepository;
        this.appId = appId;
        this.appSecret = appSecret;
        this.redirectUri = redirectUri;
        this.graphApiVersion = graphApiVersion;
    }

    public String construireUrlOAuth(Long proprietaireId) {
        String state = UUID.randomUUID().toString();
        stateVersProprietaire.put(state, proprietaireId);
        return UriComponentsBuilder
                .fromHttpUrl("https://www.facebook.com/" + graphApiVersion + "/dialog/oauth")
                .queryParam("client_id", appId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", SCOPES)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build().toUriString();
    }

    public String traiterCallback(String code, String state) {
        ProprietaireEntreprise proprietaire = chargerProprietaire(resoudreProprietaire(state));
        String userToken = echangerCodeContreToken(code);
        List<Map<String, Object>> pages = recupererPages(userToken);
        return sauvegarderComptes(pages, proprietaire);
    }

    private Long resoudreProprietaire(String state) {
        Long proprietaireId = stateVersProprietaire.remove(state);
        if (proprietaireId == null) {
            throw new BusinessException("State OAuth invalide ou expiré.");
        }
        return proprietaireId;
    }

    private String echangerCodeContreToken(String code) {
        String url = UriComponentsBuilder
                .fromHttpUrl(grapheBase() + "/oauth/access_token")
                .queryParam("client_id", appId)
                .queryParam("client_secret", appSecret)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code", code)
                .build().toUriString();
        Object token = envoyerGet(url).get("access_token");
        if (token == null) {
            throw new BusinessException("Échec de l'échange du code OAuth Meta.");
        }
        return token.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> recupererPages(String userToken) {
        String url = UriComponentsBuilder
                .fromHttpUrl(grapheBase() + "/me/accounts")
                .queryParam("access_token", userToken)
                .build().toUriString();
        Object data = envoyerGet(url).get("data");
        if (!(data instanceof List<?> liste) || liste.isEmpty()) {
            throw new BusinessException("Aucune page Facebook gérée trouvée.");
        }
        return (List<Map<String, Object>>) data;
    }

    private String sauvegarderComptes(List<Map<String, Object>> pages,
                                      ProprietaireEntreprise proprietaire) {
        String premierNom = null;
        for (Map<String, Object> page : pages) {
            CompteSocialConnecte compte = compteRepository.save(construireCompte(page, proprietaire));
            if (premierNom == null) {
                premierNom = compte.getNomCompte();
            }
        }
        log.info("[META] {} page(s) connectée(s) pour proprietaire={}",
                pages.size(), proprietaire.getId());
        return pages.size() + " page(s) connectée(s) : " + premierNom;
    }

    private CompteSocialConnecte construireCompte(Map<String, Object> page,
                                                 ProprietaireEntreprise proprietaire) {
        return CompteSocialConnecte.builder()
                .typeReseau(TypeReseau.FACEBOOK)
                .nomCompte(String.valueOf(page.get("name")))
                .identifiantExterne(String.valueOf(page.get("id")))
                .accessToken(String.valueOf(page.get("access_token")))
                .statutConnexion("CONNECTE")
                .dateConnexion(LocalDateTime.now())
                .proprietaire(proprietaire)
                .build();
    }

    private Map<?, ?> envoyerGet(String url) {
        try {
            return restClient.get().uri(URI.create(url)).retrieve().body(Map.class);
        } catch (RestClientException e) {
            log.error("[META] Erreur appel API : {}", e.getMessage());
            throw new BusinessException("Erreur lors de l'appel à l'API Meta : " + e.getMessage());
        }
    }

    private ProprietaireEntreprise chargerProprietaire(Long id) {
        return proprietaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire introuvable"));
    }

    private String grapheBase() {
        return "https://graph.facebook.com/" + graphApiVersion;
    }
}
