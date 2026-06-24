# ============================================================
#  Dockerfile — backend Spring Boot (crm-backend)
#  Emballe un .jar DÉJÀ compilé (par Maven, en local ou dans le CI).
#  Prérequis : avoir lancé `./mvnw clean package -DskipTests` au préalable
#  (le CI fera : étape Maven (build du jar) -> étape Docker (cette image)).
# ============================================================

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Utilisateur non-root (bonne pratique sécurité)
RUN useradd -r -u 1001 spring
USER spring

# Copier le .jar exécutable produit par Maven (target/*.jar)
COPY target/*.jar app.jar

# Le profil par défaut est 'local' dans application.yml :
# en conteneur/prod on l'override par variable d'env (voir docker-compose / Azure).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
