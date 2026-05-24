#!/bin/sh
echo "=== Generating config.properties ==="
cat > /app/config.properties << CONF
db.type=${DB_TYPE}
postgresql.driver=org.postgresql.Driver
postgresql.url=jdbc:postgresql://${POSTGRES_HOST:-db_postgres}:5432/${POSTGRES_DB:-smarthome}
postgresql.user=${POSTGRES_USER:-smarthome_user}
postgresql.password=${POSTGRES_PASSWORD:-smarthome_password}
sqlserver.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
sqlserver.url=jdbc:sqlserver://${MSSQL_HOST:-db_mssql}:1433;databaseName=${MSSQL_DB:-smarthome};encrypt=false;trustServerCertificate=true;
sqlserver.user=${MSSQL_USER:-sa}
sqlserver.password=${MSSQL_PASSWORD:-StrongPass123!}
CONF
echo "Config generated."
cat /app/config.properties
echo "========================="

exec java \
    --module-path /usr/share/openjfx/lib \
    --add-modules javafx.controls,javafx.graphics \
    -Dglass=gtk3 \
    -Dprism.order=sw \
    -jar /app/app.jar
