# --------------------
# Build stage
# --------------------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests


# --------------------
# Runtime stage
# --------------------
FROM eclipse-temurin:21-jre

# Install Tesseract OCR (Debian packages)
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-fra \
    tesseract-ocr-deu \
    tesseract-ocr-spa \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Set correct tessdata path (Debian)
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

WORKDIR /app

# Copy jar
COPY --from=builder /app/target/*.jar app.jar

# Create non-root user
RUN groupadd -r kyc && useradd -r -g kyc kyc
USER kyc

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
