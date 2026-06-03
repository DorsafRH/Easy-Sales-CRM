package com.crm.config.security;

import com.crm.config.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration Spring Security — Sprint 1 + Sprint 2.
 *
 * @author Riahi Dorsaf
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs*",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/entreprises/inscription"
    };
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    /*
    todo  :   supprimer les commentaires  ,ajouter java doc au methodes
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // ── Propriétaire — Sprint 1 ───────────────────────
                        .requestMatchers(HttpMethod.GET, "/entreprises/mon-compte/statut")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        .requestMatchers("/proprietaire/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        // ── Propriétaire — Sprint 2 ───────────────────────
                        .requestMatchers("/clients/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        .requestMatchers("/catalogue/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        .requestMatchers("/reporting/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        // ── Propriétaire — Sprint 3 ───────────────────────
                        .requestMatchers("/leads/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        .requestMatchers("/opportunites/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        .requestMatchers("/devis/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        .requestMatchers("/factures/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        .requestMatchers("/reunions/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")

                        // ── Marketing — Sprint 4 ──────────────────────────
                        // Callbacks publics (appelés par Meta / N8N, sans JWT)
                        .requestMatchers(HttpMethod.GET, "/marketing/oauth/callback")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST, "/marketing/n8n/callback")
                        .permitAll()

                        .requestMatchers("/marketing/**")
                        .hasAuthority("ROLE_PROPRIETAIRE")


                        // ── Super Admin ───────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/admin/entreprises/en-attente")
                        .hasAuthority("ROLE_SUPER_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/admin/entreprises")
                        .hasAuthority("ROLE_SUPER_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/admin/entreprises/*")
                        .hasAuthority("ROLE_SUPER_ADMIN")

                        .requestMatchers(HttpMethod.POST, "/admin/entreprises/*/decision")
                        .hasAuthority("ROLE_SUPER_ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/admin/entreprises/*")
                        .hasAuthority("ROLE_SUPER_ADMIN")

                        .requestMatchers("/admin/notifications/**")
                        .hasAuthority("ROLE_SUPER_ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuration CORS — autorise toutes les origines de développement.
     * <p>
     * {@code allowedOriginPatterns("*")} couvre toutes les origines
     * tout en autorisant {@code allowCredentials(true)}.
     * En production, remplacer par l'URL exacte du domaine.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Wildcard pattern — compatible avec allowCredentials(true)
        // Couvre localhost:8081 (Expo Web), 10.x.x.x (Expo Go), etc.
        config.setAllowedOriginPatterns(List.of("*"));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        // Durée du cache preflight — réduit les requêtes OPTIONS
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}