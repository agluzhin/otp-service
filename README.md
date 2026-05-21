# OTP Service

REST-сервис для генерации и валидации одноразовых паролей (OTP) с поддержкой четырёх каналов доставки.

## Стек

| Компонент | Технология |
|---|---|
| Язык | Java 17 |
| HTTP-сервер | `com.sun.net.httpserver` (JDK built-in) |
| База данных | PostgreSQL 17 + JDBC (HikariCP) |
| Аутентификация | JWT (JJWT 0.12.5) |
| Пароли | BCrypt (jBCrypt) |
| Логирование | SLF4J + Log4j2 |
| Сборка | Maven |
| Тесты | JUnit 5 + Mockito + AssertJ |

## Быстрый старт

### 1. Требования

- Java 17+
- Maven 3.8+
- PostgreSQL 17

### 2. База данных

```sql
CREATE DATABASE otpservice;
```

Затем выполните DDL-скрипт:

```bash
psql -U postgres -d otpservice -f src/main/resources/db/init.sql
```

### 3. Конфигурация

Отредактируйте `src/main/resources/application.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/otpservice
db.username=postgres
db.password=your_password
jwt.secret=your-secret-min-32-characters-long!!
```

Для Email (`src/main/resources/email.properties`):
```properties
email.username=your@email.com
email.password=your_password
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587
```

Для SMS (`src/main/resources/sms.properties`) — запустите [SMPPSim](http://www.seleniumsoftware.com/user-guide.htm) и оставьте значения по умолчанию.

Для Telegram (`src/main/resources/telegram.properties`):
```properties
telegram.bot.token=YOUR_BOT_TOKEN
telegram.chat.id=YOUR_CHAT_ID
```

### 4. Сборка и запуск

```bash
mvn package -DskipTests
java -jar target/otp-service-1.0.0.jar
```

Сервер стартует на `http://localhost:8080`.

---

## API

### Публичные эндпоинты

#### `POST /auth/register` — регистрация

```json
{ "login": "alice", "password": "secret99", "role": "USER" }
```

Роли: `USER` или `ADMIN`. Второй администратор зарегистрирован быть не может.

**Ответ 201:**
```json
{ "message": "User registered successfully" }
```

---

#### `POST /auth/login` — вход

```json
{ "login": "alice", "password": "secret99" }
```

**Ответ 200:**
```json
{ "token": "eyJhbGci..." }
```

Токен передаётся в заголовке `Authorization: Bearer <token>` во всех защищённых запросах.

---

### API пользователя (роль USER или ADMIN)

#### `POST /otp/generate` — генерация OTP

```json
{
  "operationId": "payment-42",
  "channel": "EMAIL",
  "destination": "alice@example.com"
}
```

`channel`: `EMAIL` | `SMS` | `TELEGRAM` | `FILE`

**Ответ 200:**
```json
{ "message": "OTP code sent successfully" }
```

---

#### `POST /otp/validate` — валидация OTP

```json
{
  "operationId": "payment-42",
  "code": "483921"
}
```

**Ответ 200:**
```json
{ "message": "OTP code is valid" }
```

**Ответ 400** (неверный/истёкший/использованный код):
```json
{ "error": "OTP validation failed: code does not match" }
```

---

### API администратора (роль ADMIN)

#### `POST /admin/otp-config` — изменение конфигурации OTP

```json
{ "codeLength": 8, "ttlSeconds": 600 }
```

Допустимые значения: `codeLength` от 4 до 10, `ttlSeconds` от 60 до 3600.

---

#### `GET /admin/users` — список пользователей

**Ответ 200:**
```json
[
  { "id": 2, "login": "alice", "role": "USER" },
  { "id": 3, "login": "bob",   "role": "USER" }
]
```

---

#### `DELETE /admin/users/{id}` — удаление пользователя

Удаляет пользователя и все его OTP-коды.

**Ответ 200:**
```json
{ "message": "User deleted" }
```

---

## Сценарии использования

### Сценарий 1: Полный цикл OTP через Email

```bash
# 1. Регистрация
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"login":"alice","password":"secret99","role":"USER"}'

# 2. Логин
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"alice","password":"secret99"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 3. Генерация OTP
curl -X POST http://localhost:8080/otp/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"tx-001","channel":"EMAIL","destination":"alice@example.com"}'

# 4. Валидация (подставьте код из письма)
curl -X POST http://localhost:8080/otp/validate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"tx-001","code":"483921"}'
```

### Сценарий 2: OTP в файл (без внешних сервисов)

```bash
curl -X POST http://localhost:8080/otp/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"operationId":"tx-002","channel":"FILE","destination":"local"}'

# Код появится в otp_codes.txt в корне проекта
cat otp_codes.txt
```

### Сценарий 3: Администрирование

```bash
# Регистрация админа
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"adminpass","role":"ADMIN"}'

ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"adminpass"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Изменить конфиг OTP
curl -X POST http://localhost:8080/admin/otp-config \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"codeLength":8,"ttlSeconds":120}'

# Список пользователей
curl -X GET http://localhost:8080/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Удалить пользователя с id=2
curl -X DELETE http://localhost:8080/admin/users/2 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## Запуск тестов

```bash
mvn test
```

Тесты не требуют запущенной БД — все DAO-зависимости мокируются через Mockito.

Покрытие:
- `AuthServiceTest` — 8 тестов: регистрация, логин, JWT round-trip
- `OtpServiceTest` — 7 тестов: генерация, валидация, истечение, несовпадение кода
- `AdminServiceTest` — 8 тестов: конфиг, список, удаление
- `OtpConfigDaoTest` — 3 теста: получение конфига, пустая таблица, обновление

---

## Структура проекта

```
src/main/java/com/otpservice/
├── Main.java                        ← точка входа
├── config/                          ← AppConfig, DatabaseConfig (HikariCP)
├── model/                           ← User, OtpCode, OtpConfig, enums
├── exception/                       ← типизированные бизнес-исключения
├── dao/                             ← UserDao, OtpCodeDao, OtpConfigDao (JDBC)
├── service/
│   ├── AuthService.java             ← регистрация, логин, JWT
│   ├── OtpService.java              ← генерация и валидация OTP
│   ├── AdminService.java            ← администрирование
│   └── notification/                ← Email, SMS, Telegram, File каналы
├── handler/                         ← HTTP-обработчики (AuthHandler, AdminHandler, OtpHandler)
├── middleware/                      ← JwtFilter (декоратор над HttpHandler)
└── scheduler/                       ← OtpExpirationScheduler (помечает EXPIRED-коды)
```

## Механизм истечения OTP

`OtpExpirationScheduler` запускает фоновый поток каждые N секунд (задаётся в `application.properties`).
Он выполняет один UPDATE-запрос:

```sql
UPDATE otp_codes SET status = 'EXPIRED'
WHERE status = 'ACTIVE' AND expires_at < now()
```

Дополнительно `OtpService.validate()` проверяет срок действия при каждой валидации, не дожидаясь планировщика.

## Логирование

Логи пишутся в консоль и в `logs/otp-service.log` с ротацией по дате и размеру (10 MB).
Каждый входящий запрос логируется на уровне INFO с методом, путём и userId.
Ошибки аутентификации — WARN. Системные ошибки — ERROR.