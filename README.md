# SmartHomeJavaDB

## Требования

- **JDK** | 17 (LTS) |
- **IDE** | IntelliJ IDEA / Eclipse / VS Code (рекомендуется IDEA) |
- **Сборка** | Maven 3.8+ |
- **Контейнеризация** | Docker Desktop (с поддержкой WSL2 для Windows) |
- **Драйверы** | Подтягиваются автоматически через `pom.xml` |

## Клонируем репозиторий

```bash
git clone git@github.com:vsvstka/SmartHomeJavaDB.git
```

Переходим в папку репозитория:

```text
cd SmartHomeJavaDB
```
## Запуск с Postgres

В терминале пишем

```bash
#При первом запуске
docker compose --profile postgres up -d db_postgres --build
#При последующих
docker compose --profile postgres up -d db_postgres
#Чтобы остановить контейнер
docker compose --profile postgres down
```
Далее для запуска переходим в нашу IDE(Далее будет рассмотренно на примере IntelliJ IDEA)

Заходим в файл \src\main\resources\config.properties. Нам нужно убедится что первые 2 строки выглядят следующим образом:

```bash
db.type=postgresql
#db.type=sqlserver
```
Далее убедимся, что у нас JDK17:

File → Project Structure → Project:

SDK: выберите 17 (или установите через Add SDK → Download JDK)

Language level: 17

После того как мы в этом убедились открываем файл C:\Users\Daniil\SmartHomeJavaDB\src\main\java\smarthome\Launcher.java и запускаем его.
После чего у нас должен появится интерфейс нашего приложения. Вводим данные admin и admin

## Запуск с SQLServer

В терминале пишем

```bash
#При первом запуске
docker compose --profile mssql up -d db_mssql --build
#При последующих
docker compose --profile mssql up -d db_mssql
#Чтобы остановить контейнер
docker compose --profile mssql down
```

Так как SQLServer автоматически не запускает скрипты в Docker нам нужно сделать это в ручную

```bash
docker cp .\db\init\init_mssql.sql smarthome_db_mssql:/tmp/init.sql
docker exec -it smarthome_db_mssql /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P 'StrongPass123!' -i /tmp/init.sql
# Алитернатива если не сработало: docker exec -it smarthome_db_mssql /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P 'StrongPass123!' -i /tmp/init.sql
```

Далее для запуска переходим в нашу IDE(Далее будет рассмотренно на примере IntelliJ IDEA)

Заходим в файл \src\main\resources\config.properties. Нам нужно убедится что первые 2 строки выглядят следующим образом:

```bash
#db.type=postgresql
db.type=sqlserver
```
Далее убедимся, что у нас JDK17:

File → Project Structure → Project:

SDK: выберите 17 (или установите через Add SDK → Download JDK)

Language level: 17

После того как мы в этом убедились открываем файл src\main\java\smarthome\Launcher.java и запускаем его.
После чего у нас должен появится интерфейс нашего приложения. Вводим данные admin и admin
