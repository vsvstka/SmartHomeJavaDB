# Этап сборки
FROM maven:3.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Сборка проекта (Shade создаст fat JAR без JavaFX)
RUN mvn clean package -DskipTests

# Этап запуска — используем образ с JDK 17 на базе Ubuntu (содержит glibc)
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Установка системного OpenJFX и графических библиотек
# Используем openjfx из репозитория Ubuntu для надёжной работы с нативными компонентами
RUN apt-get update && apt-get install -y \
    openjfx \
    libgtk-3-0 \
    libgl1 \
    libxtst6 \
    libxrender1 \
    libxi6 \
    libxrandr2 \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# Копируем fat JAR (Shade создаёт файл без суффикса)
COPY --from=build /app/target/SmartHomeManager-1.0-SNAPSHOT.jar app.jar

# Копируем скрипт entrypoint
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Запуск приложения
ENTRYPOINT ["/entrypoint.sh"]
