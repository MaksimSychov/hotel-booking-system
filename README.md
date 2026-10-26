# Hotel Booking System

Микросервисное REST API для бронирования отелей на **Spring Boot** и **Spring Cloud**.

Проект реализует:
- Управление отелями и номерами (CRUD, статистика загруженности).
- Бронирования с автоматическим подбором номера (равномерная нагрузка).
- Аутентификацию и авторизацию по **JWT** с ролями `USER` / `ADMIN` / `INTERNAL`.
- Распределённую согласованность по паттерну **Saga** с компенсацией.
- **Идемпотентность** запросов и корректную работу под нагрузкой.

---

## Структура проекта
```tree
hotel-booking-system/
├── .gitignore
├── pom.xml
├── README.md
│
├── api-gateway/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/hotel/gateway/
│       │   ├── filter/
│       │   │   └── TraceIdFilter.java
│       │   └── ApiGatewayApplication.java
│       └── resources/
│           └── application.yml
│
├── booking-service/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/hotel/booking/
│       │   │   ├── client/
│       │   │   │   └── HotelClient.java
│       │   │   ├── config/
│       │   │   │   ├── JwtService.java
│       │   │   │   ├── MdcTraceFilter.java
│       │   │   │   ├── RestClientConfig.java
│       │   │   │   └── SecurityConfig.java
│       │   │   ├── controller/
│       │   │   │   ├── BookingController.java
│       │   │   │   └── UserController.java
│       │   │   ├── dto/
│       │   │   │   ├── AuthRequest.java
│       │   │   │   ├── AuthResponse.java
│       │   │   │   ├── BookingRequest.java
│       │   │   │   ├── BookingResponse.java
│       │   │   │   ├── ErrorDto.java
│       │   │   │   ├── RegisterRequest.java
│       │   │   │   └── UserDto.java
│       │   │   ├── entity/
│       │   │   │   ├── Booking.java
│       │   │   │   ├── BookingStatus.java
│       │   │   │   ├── ProcessedRequest.java
│       │   │   │   ├── Role.java
│       │   │   │   └── User.java
│       │   │   ├── exception/
│       │   │   │   ├── ConflictException.java
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   └── NotFoundException.java
│       │   │   ├── repository/
│       │   │   │   ├── BookingRepository.java
│       │   │   │   ├── ProcessedRequestRepository.java
│       │   │   │   └── UserRepository.java
│       │   │   ├── service/
│       │   │   │   ├── AuthService.java
│       │   │   │   ├── BookingService.java
│       │   │   │   └── UserService.java
│       │   │   └── BookingServiceApplication.java
│       │   └── resources/
│       │       └── application.yml
│       └── test/java/com/hotel/booking/
│           ├── BookingConcurrencyTest.java
│           ├── BookingControllerTest.java
│           └── BookingSagaTest.java
│
├── eureka-server/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/hotel/eureka/
│       │   └── EurekaServerApplication.java
│       └── resources/
│           └── application.yml
│
└── hotel-service/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/hotel/hotel/
        │   │   ├── config/
        │   │   │   ├── MdcTraceFilter.java
        │   │   │   └── SecurityConfig.java
        │   │   ├── controller/
        │   │   │   ├── HotelController.java
        │   │   │   ├── RoomController.java
        │   │   │   └── StatsController.java
        │   │   ├── dto/
        │   │   │   ├── AvailabilityRequest.java
        │   │   │   ├── ErrorDto.java
        │   │   │   ├── HotelDto.java
        │   │   │   ├── RoomDto.java
        │   │   │   ├── RoomRequest.java
        │   │   │   └── RoomStatsDto.java
        │   │   ├── entity/
        │   │   │   ├── Hotel.java
        │   │   │   ├── Room.java
        │   │   │   └── RoomSlot.java
        │   │   ├── exception/
        │   │   │   ├── GlobalExceptionHandler.java
        │   │   │   └── NotFoundException.java
        │   │   ├── mapper/
        │   │   │   ├── HotelMapper.java
        │   │   │   └── RoomMapper.java
        │   │   ├── repository/
        │   │   │   ├── HotelRepository.java
        │   │   │   ├── RoomRepository.java
        │   │   │   └── RoomSlotRepository.java
        │   │   ├── service/
        │   │   │   ├── HotelService.java
        │   │   │   └── RoomService.java
        │   │   └── HotelServiceApplication.java
        │   └── resources/
        │       ├── application.yml
        │       └── data.sql
        └── test/java/com/hotel/hotel/
            └── RoomServiceTest.java
```

---

# Как пользоваться системой

## 1. Сборка

mvn clean install -U

## 2. Запуск

### Терминал 1 - Eureka Server
cd eureka-server  
mvn spring-boot:run

### Терминал 2 - Hotel Service
cd hotel-service   
mvn spring-boot:run

### Терминал 3 - Booking Service
cd booking-service  
mvn spring-boot:run

### Терминал 4 - API Gateway
cd api-gateway  
mvn spring-boot:run  

## 3. Проверка готовности

| Компонент | URL | Ожидаемый результат |
|---|---|---|
| Eureka Dashboard | http://localhost:8761 | 3 сервиса в статусе `UP` |
| Gateway health | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| Swagger Hotel | http://localhost:8081/swagger-ui/index.html | UI отображается |
| Swagger Booking | http://localhost:8082/swagger-ui/index.html | UI отображается |

## 4. Быстрый сценарий: бронирование
### Шаг 1. Регистрация → JWT
```powershell
$json = @{ username = "max"; password = "pass123" } | ConvertTo-Json -Compress
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)

try {
$r = Invoke-RestMethod -Uri "http://localhost:8080/user/register" `
      -Method Post `
-ContentType "application/json; charset=utf-8" `
-Body $bytes -ErrorAction Stop
$token = $r.token
Write-Host "TOKEN: $token" -ForegroundColor Cyan
} catch {
Write-Host "Status: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
Write-Host "Body: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
}
```
→ {"token":"eyJhbGciOiJIUzI1NiJ9..."}

### Шаг 2. Создание брони с автоподбором номера
```powershell
$json = @{ autoSelect = $true; startDate = "2026-11-10"; endDate = "2026-11-12"; requestId = "max-1" } | ConvertTo-Json -Compress
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
$headers = @{ Authorization = "Bearer $token" }
$booking = Invoke-RestMethod -Uri "http://localhost:8080/api/booking" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $bytes
$booking | Format-List
```
→ {"id":1,"roomId":1,"status":"CONFIRMED", ...}

### Шаг 3. История броней
```powershell
$headers = @{ Authorization = "Bearer $token" }
$history = Invoke-RestMethod -Uri "http://localhost:8080/api/bookings?page=0&size=10" `
  -Method Get `
-Headers $headers
$history | ConvertTo-Json -Depth 10
```

### Шаг 4. Отмена
```powershell
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/booking/1" `
  -Method Delete `
  -Headers $headers
Write-Host "Booking 1 cancelled" -ForegroundColor Green
```

Даты должны быть в будущем — иначе вернётся 400 Bad Request: startDate must be in the future.  

requestId должен быть уникальным для каждой новой брони; повторный запрос с тем же requestId вернёт существующую бронь (идемпотентность).

## 5. Администратор
Роль ADMIN нужна для управления отелями, номерами и пользователями.

### 1) Зарегистрируйтесь как обычный пользователь
```powershell
$json  = @{ username = "admin"; password = "admin123" } | ConvertTo-Json -Compress
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)

try {
    Invoke-RestMethod -Uri "http://localhost:8080/user/register" `
      -Method Post `
      -ContentType "application/json; charset=utf-8" `
      -Body $bytes -ErrorAction Stop
    Write-Host "Admin registered" -ForegroundColor Green
} catch {
    Write-Host "admin уже существует — ок, переходите к шагу 2" -ForegroundColor Yellow
}
```

### 2) Обновите роль через H2-консоль
Откройте http://localhost:8082/h2-console:


| Поле | Значение              |
|---|-----------------------|
| JDBC URL | jdbc:h2:mem:bookingdb |
| User Name | sa                    |
| Password | *(пусто)*             |

Нажмите **Connect** → в SQL-редакторе выполните:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'admin';
```

Нажмите **Run** — статус пользователя станет `ADMIN`.

### 3) Получите новый токен (с ролью ADMIN)
```powershell
$json  = @{ username = "admin"; password = "admin123" } | ConvertTo-Json -Compress
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)

$r = Invoke-RestMethod -Uri "http://localhost:8080/user/auth" `
  -Method Post `
  -ContentType "application/json; charset=utf-8" `
  -Body $bytes

$adminToken = $r.token
Write-Host "ADMIN TOKEN: $adminToken" -ForegroundColor Cyan
```

### 4) Создайте отель
```powershell
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
$json  = @{ name = "Grand Hotel"; address = "Moscow" } | ConvertTo-Json -Compress
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)

$hotel = Invoke-RestMethod -Uri "http://localhost:8080/api/hotels" `
  -Method Post `
  -Headers $adminHeaders `
  -ContentType "application/json; charset=utf-8" `
  -Body $bytes
$hotel | Format-List
```

### 5) Создайте номер
```powershell
$json  = @{ hotelId = $hotel.id; number = "A101"; available = $true } | ConvertTo-Json -Compress
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)

$room = Invoke-RestMethod -Uri "http://localhost:8080/api/rooms" `
  -Method Post `
  -Headers $adminHeaders `
  -ContentType "application/json; charset=utf-8" `
  -Body $bytes

$room | Format-List
```

## 6. Проверка компенсации (Saga)
### 1) Остановите hotel-service (Ctrl+C)

Переключитесь в терминал, где запущен `hotel-service`, и нажмите **`Ctrl+C`**.

### 2) Попробуйте создать бронь
```powershell
$json = @{
    roomId     = 1
    autoSelect = $false
    startDate  = "2026-12-01"
    endDate    = "2026-12-03"
    requestId  = "saga-" + (Get-Random)
} | ConvertTo-Json -Compress
$bytes = [System.Text.Encoding]::UTF8.GetBytes($json)

try {
    $booking = Invoke-RestMethod -Uri "http://localhost:8080/api/booking" `
      -Method Post -Headers @{ Authorization = "Bearer $token" } `
      -ContentType "application/json; charset=utf-8" `
      -Body $bytes -ErrorAction Stop
    $booking | Format-List
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "Status: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
    Write-Host "Body: $($reader.ReadToEnd())" -ForegroundColor Yellow
}
```

→ {"status":"CANCELLED", ...}

### 3) Перезапустите hotel-service
В **отдельном** окне PowerShell:

```powershell
cd hotel-service
mvn spring-boot:run
```

## 7. Негативные сценарии (чек-лист)
| Сценарий | Ожидаемый результат |
|---|---|
| `POST /api/booking` **без токена** | `401 Unauthorized` |
| `POST /api/booking` с `startDate` в прошлом | `400 Bad Request` |
| `POST /api/booking` с `endDate <= startDate` | `400 Bad Request` |
| `GET /api/booking/{id}` — **чужая** бронь | `403 Forbidden` |
| `POST /api/hotels` **USER-токеном** | `403 Forbidden` |
| `POST /api/booking` с пересекающимися датами | `409 Conflict` |
| `POST /api/booking` повторно с тем же `requestId` | `201 Created`, **та же бронь** |
| Остановить `hotel-service` → `POST /api/booking` | `201 Created`, `status = CANCELLED` (Saga) |

# Поддерживаемые эндпойнты

### Аутентификация и пользователи (Booking Service)

| Метод | Путь | Роли | Описание |
|---|---|---|---|
| `POST` | `/user/register` | public | Регистрация + JWT |
| `POST` | `/user/auth` | public | Аутентификация + JWT |
| `GET` | `/user/me` | USER, ADMIN | Текущий пользователь |
| `POST` | `/user` | ADMIN | Создать пользователя |
| `PATCH` | `/user?id=` | ADMIN | Обновить пользователя |
| `DELETE` | `/user?id=` | ADMIN | Удалить пользователя |
| `GET` | `/user` | ADMIN | Список пользователей |

### Бронирования (Booking Service)

| Метод | Путь | Роли | Описание |
|---|---|---|---|
| `POST` | `/api/booking` | USER, ADMIN | Создать бронь (`autoSelect` или `roomId`) |
| `GET` | `/api/bookings?page=0&size=10` | USER, ADMIN | История (пагинация) |
| `GET` | `/api/booking/{id}` | USER, ADMIN | Бронь по id (только своя) |
| `DELETE` | `/api/booking/{id}` | USER, ADMIN | Отменить бронь |

### Отели и номера (Hotel Service)

| Метод | Путь | Роли | Описание |
|---|---|---|---|
| `POST` | `/api/hotels` | ADMIN | Создать отель |
| `GET` | `/api/hotels` | USER, ADMIN | Список отелей |
| `DELETE` | `/api/hotels/{id}` | ADMIN | Удалить отель |
| `POST` | `/api/rooms` | ADMIN | Создать номер |
| `GET` | `/api/rooms` | USER, ADMIN | Свободные номера |
| `GET` | `/api/rooms/recommend` | USER, ADMIN | Рекомендуемые (по `times_booked`) |
| `GET` | `/api/rooms/stats` | ADMIN | Статистика загруженности |
| `POST` | `/api/rooms/{id}/confirm-availability` | INTERNAL | Подтверждение (Saga) |
| `POST` | `/api/rooms/{id}/release` | INTERNAL | Компенсация (Saga) |

### Служебные URL

| Компонент | URL |
|---|---|
| Eureka UI | http://localhost:8761 |
| Gateway health | http://localhost:8080/actuator/health |
| Swagger Hotel | http://localhost:8081/swagger-ui/index.html |
| Swagger Booking | http://localhost:8082/swagger-ui/index.html |
| H2 Console Booking | http://localhost:8082/h2-console |

# Автоматические тесты

mvn test

### Что покрыто тестами

| Тест | Что проверяет |
|---|---|
| `RoomServiceTest.recommend_sortedByTimesBookedThenId` | Сортировка по `times_booked`, при равенстве — по `id` |
| `RoomServiceTest.confirm_idempotent` | Повторный `requestId` не увеличивает `times_booked` |
| `RoomServiceTest.confirm_conflictOnOverlappingDates` | Пересечение дат → `IllegalStateException` |
| `RoomServiceTest.release_freesSlotAndDecrementsCount` | Компенсация освобождает слот и откатывает счётчик |
| `BookingControllerTest.create_unauthorized_401` | Без JWT → `401 Unauthorized` |
| `BookingControllerTest.create_authorized_201` | С JWT → `201 Created` |
| `BookingControllerTest.adminEndpoint_forbiddenForUser_403` | USER на админском эндпойнте → `403 Forbidden` |
| `BookingConcurrencyTest.multipleBookings_sameRoom_shouldConflictOnOverlap` | 1 успех + 4 конфликта при повторных бронированиях одного номера на пересекающиеся даты |
| `BookingSagaTest.confirm_success` | `/confirm` = 200 → `CONFIRMED` |
| `BookingSagaTest.confirm_fail_compensation` | `/confirm` = 500 → `CANCELLED` + `/release` |
| `BookingSagaTest.confirm_timeout_compensation` | `/confirm` = 5s delay → timeout → `CANCELLED` + `/release` |

Все тесты используют **WireMock** и **`@TestConfiguration`** — независимы от окружения и Eureka.
