# Prueba Fullstack CTI / AVAYA

## Validación rápida

Desde la raíz del proyecto puedes ejecutar toda la validación local con:

```powershell
.\scripts\test-all.ps1
```

Este comando ejecuta:

- Tests del backend con Gradle.
- Build del frontend Angular.
- Tests unitarios del frontend.

Para levantar la solución completa:

```powershell
$env:CTI_WEBSOCKET_URL = "ws://precook-overtone-syndrome.ngrok-free.dev"
.\scripts\run-backend.ps1
```

En otra terminal:

```powershell
.\scripts\run-frontend.ps1
```

URLs principales:

- Frontend: `http://localhost:4200`
- Backend health: `http://localhost:8080/health`
- Snapshot CTI: `http://localhost:8080/cti/snapshot`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Objetivo

El sistema se conecta a un Mock CTI Server tipo AVAYA AES, recibe eventos de llamadas por WebSocket, mantiene el estado actual en memoria y expone esa información al frontend mediante REST y Server-Sent Events.

## Arquitectura

```text
Mock CTI WebSocket
        |
        v
Backend Spring Boot
  - Cliente WebSocket CTI
  - Parser JSON
  - Procesador de eventos
  - Estado en memoria thread-safe
  - REST API
  - SSE hacia frontend
        |
        v
Frontend Angular
  - Carga inicial por REST
  - Actualización en vivo por SSE
  - Estado con Signals
  - Componentes standalone
```

## Backend

Ubicación: `backend/`

Tecnologías:

- Java 17
- Spring Boot
- Gradle
- Spring WebSocket
- REST API
- Server-Sent Events
- Actuator
- Swagger/OpenAPI

### Responsabilidades principales

- `websocket/CtiWebSocketClient`: conexión al Mock CTI Server, manejo de cierre, errores y reconexión con backoff.
- `service/CtiEventProcessor`: parsea mensajes JSON y delega la lógica de negocio.
- `service/CtiStateService`: mantiene llamadas, agentes y extensiones en memoria usando estructuras thread-safe.
- `service/EventStreamService`: publica snapshots al frontend mediante SSE.
- `controller/*`: expone los endpoints REST y el stream en vivo.

### Configuración

La URL del Mock CTI no está hardcodeada en código. Puede configurarse por variable de entorno:

```bash
CTI_WEBSOCKET_URL=ws://host-del-mock-cti
```

Configuración disponible:

| Variable | Descripción | Default |
| --- | --- | --- |
| `SERVER_PORT` | Puerto HTTP del backend | `8080` |
| `CTI_ENABLED` | Activa/desactiva conexión CTI | `true` |
| `CTI_WEBSOCKET_URL` | URL del Mock CTI Server | URL de ejemplo del enunciado |
| `CTI_RECONNECT_INITIAL_DELAY` | Primer delay de reconexión | `2s` |
| `CTI_RECONNECT_MAX_DELAY` | Delay máximo de reconexión | `30s` |
| `APP_CORS_ALLOWED_ORIGINS` | Origen permitido para Angular | `http://localhost:4200` |

### Endpoints

| Método | Endpoint | Uso |
| --- | --- | --- |
| `GET` | `/health` | Health simple requerido por la prueba |
| `GET` | `/calls/active` | Lista de llamadas activas |
| `GET` | `/agents` | Estado actual de agentes |
| `GET` | `/extensions` | Estado actual de extensiones |
| `GET` | `/cti/snapshot` | Snapshot completo para carga o diagnóstico |
| `GET` | `/cti/events` | Stream SSE para actualización en tiempo real |
| `GET` | `/swagger-ui.html` | Swagger UI |
| `GET` | `/actuator/health` | Health de Spring Actuator |

### Ejecutar backend

El repositorio incluye `scripts/run-backend.ps1` para descargar localmente JDK 17 y Gradle si no están instalados de forma global. Esto deja la ejecución más simple en Windows y no requiere modificar el PATH del sistema.

```powershell
$env:CTI_WEBSOCKET_URL = "ws://host-del-mock-cti"
.\scripts\run-backend.ps1
```

Si ya tienes Gradle instalado globalmente también puedes ejecutar:

```powershell
cd backend
$env:CTI_WEBSOCKET_URL = "ws://host-del-mock-cti"
gradle bootRun
```

### Tests backend

```bash
cd backend
gradle test
```

O desde la raíz, usando el script que descarga Gradle localmente:

```powershell
.\scripts\run-backend.ps1 test
```

## Frontend

Ubicación: `frontend/`

Tecnologías:

- Angular 17+ standalone components
- TypeScript
- HttpClient
- Signals
- Server-Sent Events con `EventSource`

### Responsabilidades principales

- `core/models/cti.models.ts`: contratos tipados del backend.
- `core/services/cti-api.ts`: consumo REST.
- `core/services/dashboard-store.ts`: estado de la UI, carga inicial, manejo de errores y stream en vivo.
- `features/dashboard/*`: dashboard y componentes de presentación.

### Ejecutar frontend

```bash
cd frontend
npm install
npm start
```

En Windows también puedes levantarlo desde la raíz:

```powershell
.\scripts\run-frontend.ps1
```

La aplicación queda disponible en:

```text
http://localhost:4200
```

La URL del backend está en:

```text
frontend/src/environments/environment.ts
```

Por defecto apunta a:

```text
http://localhost:8080
```

## Flujo de eventos

1. El backend abre una conexión WebSocket contra el Mock CTI Server.
2. Cada mensaje JSON se parsea como evento CTI.
3. El backend valida tipo de evento y campos mínimos.
4. El estado en memoria se actualiza de forma sincronizada.
5. Se recalculan estados derivados de agentes y extensiones.
6. Se publica un snapshot por SSE.
7. Angular actualiza la UI sin recargar la página.

## Manejo de concurrencia

El backend usa `ConcurrentHashMap` para las colecciones compartidas y sincroniza la aplicación de cada evento en `CtiStateService`. Esto evita estados intermedios inconsistentes entre llamadas, agentes y extensiones.

Las llamadas se representan como objetos inmutables. Cada transición reemplaza el estado anterior por uno nuevo.

## Idempotencia

El servicio de estado calcula una huella del evento recibido usando tipo, llamada, extensión, agente, teléfono y timestamp. Si el Mock CTI reenvía el mismo evento, el backend lo ignora y registra un log de debug.

Esto evita que eventos duplicados produzcan cambios repetidos o ruido innecesario hacia el frontend.

## Reconexión y resiliencia

`CtiWebSocketClient` detecta errores de transporte y cierres de sesión. Cuando ocurre una desconexión:

- Marca el CTI como desconectado.
- Registra un log claro.
- Programa reconexión con backoff.
- Continúa exponiendo por REST el último estado conocido.

## Manejo de estados en Angular

El dashboard refleja:

- Estado de carga.
- Error de backend caído.
- Error de stream en vivo.
- Estado vacío cuando no hay llamadas, agentes o extensiones.
- Indicadores visuales por estado de llamada, agente y extensión.

Las suscripciones HTTP usan `takeUntilDestroyed`. El `EventSource` se cierra con `DestroyRef` para evitar fugas de memoria.

## Buenas prácticas cubiertas

- Separación de responsabilidades en backend y frontend.
- Configuración externa para el Mock CTI.
- REST API tipada y documentada.
- Logs de conexión, desconexión, eventos recibidos y errores.
- Manejo de errores HTTP y eventos en vivo malformados.
- Estado thread-safe en backend.
- UI sin datos hardcodeados.
- Componentes Angular standalone.
- Estado Angular con Signals.
- SSE para actualización en tiempo real.
- Tests unitarios base en backend y frontend.
- Dockerfile opcional para backend.

## Limitaciones intencionales

- No hay base de datos porque la prueba pide mantener estado en memoria.
- No hay seguridad porque el Mock CTI no requiere autenticación.
- No hay Kafka ni Kubernetes porque están fuera del alcance solicitado.
- No se implementaron acciones de hold/resume/transfer/end desde la UI porque son opcionales y dependen de endpoints no requeridos por el enunciado.
