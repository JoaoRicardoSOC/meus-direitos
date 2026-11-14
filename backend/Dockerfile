# Imagem da API Meus Direitos para deploy (Render ou qualquer serviço que rode containers).
# Configuração só por variáveis de ambiente: MD_DB_USUARIO, MD_DB_SENHA, MD_TOKEN_SEGREDO,
# MD_CORS_ORIGENS etc. (veja backend/db.properties.example). O db.properties nunca entra na imagem.

# ---- Etapa 1: compila com o Maven e junta as dependências do pom.xml
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Baixa as dependências antes de copiar o código: muda pouco, então fica em cache entre builds
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests compile dependency:copy-dependencies -DincludeScope=runtime

# ---- Etapa 2: só o Java para rodar (sem Maven nem código-fonte)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./lib

# O Render informa a porta em PORT; a API escuta em 0.0.0.0 nessa porta (AppConfig.serverUri)
ENV PORT=8080
EXPOSE 8080

# Limita a memória da JVM ao que o container tem (o plano gratuito do Render tem 512 MB)
CMD ["java", "-XX:MaxRAMPercentage=75", "-cp", "classes:lib/*", "br.com.fiap.meusdireitos.Main"]
