# Base image
FROM eclipse-temurin:17.0.13_11-jdk

# DEFINIR DIRECTORIO RAIZ DE NUESTRO CONTENEDOR
WORKDIR /root

ARG JAR_FILE=target/file-manager-0.0.1-SNAPSHOT.jar
# Copy changing the the name
#COPY ${JAR_FILE} app-videogames.jar
COPY target/file-manager-0.0.1-SNAPSHOT.jar file-manager.jar

# INFORMAR EL PUERTO DONDE SE EJECUTA EL CONTENEDOR (INFORMATIVO)
EXPOSE 8081

# COPIAR Y PEGAR ARCHIVOS DENTRO DEL CONTENEDOR
COPY ./pom.xml /root
COPY ./.mvn /root/.mvn
COPY ./mvnw /root

# DESCARGAR LAS DEPENDENCIAS
RUN ./mvnw dependency:go-offline
