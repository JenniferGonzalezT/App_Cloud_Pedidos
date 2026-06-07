# ===================================================================
# PASO 1: Imagen base con Java 21 (JDK ligera)
# ===================================================================
FROM eclipse-temurin:21-jre-alpine

# Definimos el directorio de trabajo dentro del contenedor
WORKDIR /app

# ===================================================================
# PASO 2: Crear el directorio físico para Amazon EFS
# ===================================================================
# Es crucial crear la carpeta dentro del contenedor para que coincida 
# con la ruta real de montaje que definimos en las propiedades (/app/efs).
RUN mkdir -p /app/efs

# ===================================================================
# PASO 3: Copiar el archivo JAR compilado por el Pipeline
# ===================================================================
# GitHub Actions compilará la app y dejará el JAR en la carpeta target/
COPY target/*.jar app.jar

# Expone el puerto estándar de nuestra API REST
EXPOSE 8080

# ===================================================================
# PASO 4: Comando de ejecución de la aplicación
# ===================================================================
ENTRYPOINT ["java", "-jar", "app.jar"]
