# 💻 keycloak-spi

Colección de SPIs (Service Provider Interfaces) modulares, independientes y personalizables para **Keycloak 26.8.1** sobre **Java 21 LTS** (Quarkus runtime).

Cada SPI cuenta con su propia fábrica (`Factory`), identificador único (`Provider ID`), clases de contexto aisladas y registro independiente en `META-INF/services/`.

---

## 🚀 Guía de Despliegue Rápido

### 1. Compilación
```bash
mvn clean package
```

### 2. Instalación en Docker
```bash
# Copiar el JAR al directorio de providers de Keycloak
docker cp target/keycloak-spi.jar <contenedor_keycloak>:/opt/keycloak/providers/

# Reiniciar para que Keycloak compile y registre los SPIs
docker restart <contenedor_keycloak>

# Inspeccionar logs en tiempo real
docker logs -f <contenedor_keycloak>
```

---

## 🧊 SPI: Reset Credential Email (User)

SPI de autenticación independiente enfocado exclusivamente en la recuperación de contraseñas de usuarios en realms de aplicación/negocio.

### Especificación Técnica:
* **Tipo de SPI:** `Authenticator`
* **Factory Class:** `UserResetCredentialEmailFactory`
* **Provider ID:** `keycloak-spi-send-email`
* **Nombre en UI:** `NEW RESET CREDENTIAL EMAIL`
* **Paquete Java:** `org.keycloak.authentication.authenticators.resetcred.user`
* **Realm Destino:** Realms de aplicación

### Pipeline de Pasos:
1. **`UserEventSave`:** Registra el evento de inicio de reseteo en Keycloak (`SEND_RESET_PASSWORD`).
2. **`UserValidateUser`:** Valida que el usuario exista y tenga email configurado. Si no cuenta con email, interrumpe el flujo con error `400`.
3. **`UserGenerateToken`:** Genera el token de acción (`ExecuteActionsActionToken`) asociado al cliente con la acción `UPDATE_PASSWORD`.
4. **`UserSendEmail`:** Capa de salida para entrega del enlace.

> 💡 **Adaptabilidad de `UserSendEmail`:**  
> Esta clase está diseñada como una **capa desacoplada**. Actualmente emite el enlace en `stdout` para propósitos de prueba y laboratorio, pero está concebida para ser reemplazada o adaptada al proveedor final de entrega requerido (SMTP nativo, AWS SES, SendGrid, API REST o colas de mensajería).

### Propiedades Configurables:
* `RedirectUri` *(Opcional)*: URL de redirección final tras completar el cambio de contraseña.
* `LifeSecondsToken` *(Default: 86400)*: Tiempo de validez del enlace en segundos (24 horas).

### Salida Actual en Consola:
```text
=========================================================================================
>>> [KEYCLOAK SPI - USER REALM PASSWORD RESET]
>>> User      : carlos.perez
>>> ProcessID : e3b0c442-98fc-1c14-9afb-f4c8996fb924
>>> RESET URL : http://localhost:8080/realms/mi-realm/login-actions/action-token?key=...
=========================================================================================
```

---

## 🧊 SPI: Reset Credential Email (Master Admin)

SPI de autenticación independiente y dedicado para la recuperación de acceso de administradores del realm **`master`**. Posee su propia fábrica, pipeline y reglas de validación separadas del SPI de usuarios.

### Especificación Técnica:
* **Tipo de SPI:** `Authenticator`
* **Factory Class:** `MasterResetCredentialEmailFactory`
* **Provider ID:** `keycloak-spi-send-email-master`
* **Nombre en UI:** `MASTER RESET CREDENTIAL EMAIL (LOG ONLY)`
* **Paquete Java:** `org.keycloak.authentication.authenticators.resetcred.master`
* **Realm Destino:** Exclusivo para realm `master`

### Pipeline de Pasos:
1. **`MasterEventSave`:** Registra el evento administrativo en `master` (`SEND_RESET_PASSWORD`).
2. **`MasterValidateUser`:** Valida que la ejecución ocurra en `master` y que el usuario exista. **Permite continuar incluso si el usuario administrador no tiene correo configurado**.
3. **`MasterGenerateToken`:** Genera el token asociándolo al cliente `security-admin-console` y redirigiendo a la consola administrativa (`/admin/master/console/`).
4. **`MasterSendEmail`:** Capa de salida visual para los logs del servidor.

> 💡 **Adaptabilidad de `MasterSendEmail`:**  
> Funciona como una **capa desacoplada**. Por defecto imprime un banner visual destacado en consola para copiar el enlace directamente desde los logs de Docker, pero puede adaptarse si se requiere despachar por canales de alerta administrativa (Slack, webhook, correo seguro, etc.).

### Propiedades Configurables:
* `RedirectUri` *(Default: /admin/master/console/)*: Redirección automática a la consola de administración.
* `LifeSecondsToken` *(Default: 86400)*: Tiempo de validez del enlace en segundos (24 horas).

### Salida Actual en Consola:
```text
=========================================================================================
>>> [KEYCLOAK SPI - MASTER REALM ADMIN PASSWORD RESET]
>>> User      : admin
>>> ProcessID : 7a8f3b21-4d1e-4567-9abc-1234567890ab
>>> RESET URL : http://localhost:8080/realms/master/login-actions/action-token?key=...
=========================================================================================
```

---

## 🧊 SPI: Auditoría de Eventos (Admin REST Extension)

SPI de extensión administrativa REST (`AdminRealmResourceProvider`) para registrar eventos de auditoría donde un **Actor (Usuario A)** realiza una acción sobre un **Objetivo (Usuario B)**.

### Especificación Técnica:
* **Tipo de SPI:** `AdminRealmResourceProvider`
* **Factory Class:** `BootstrapFactory`
* **Provider ID:** `audit`
* **Ruta REST:** `POST /admin/realms/{realm}/audit/events`
* **Paquete Java:** `org.keycloak.endpoints.audit.event`
* **Seguridad Requerida:** Token Bearer con rol administrativo `manage-users`

### Estructura Interna:
* **`handler/EventHandler.java`:** Controlador REST delgado que evalúa permisos y delega a la aplicación.
* **`application/EventSaveUseCase.java`:** Orquesta la resolución del actor, búsqueda de usuario en base de datos y persistencia en Keycloak.
* **`domain/`:** Modelos de datos inmutables (`EventRecord`, `EventRequest`, `EventResponse`).

### Contrato de la API

#### Petición (`POST /admin/realms/{realm}/audit/events`):
```json
{
  "targetUser": "carlos.perez",
  "actor": "admin_maria",
  "clientId": "security-admin-console",
  "action": "RESET_PASSWORD_FORCED",
  "description": "Reseteo solicitado por ticket de soporte",
  "details": {
    "ticket": "SEC-1049",
    "ip": "192.168.1.50"
  }
}
```
> *Nota: `"actor"` es opcional; si no se especifica, se toma automáticamente del administrador autenticado en el token Bearer.*

#### Respuesta (`200 OK`):
```json
{
  "processId": "c4b183a2-581d-44a6-96cb-8d147bb9a302",
  "actor": "admin_maria",
  "targetUser": "carlos.perez",
  "action": "RESET_PASSWORD_FORCED",
  "clientId": "security-admin-console",
  "status": "SUCCESS",
  "message": "Audit event recorded: User [admin_maria] performed [RESET_PASSWORD_FORCED] on User [carlos.perez]",
  "timestamp": "2026-10-30T21:07:00"
}
```

---

## 🧊 SPI: Security Event Listener (Detección de Fuerza Bruta y Spraying con Infinispan)

SPI de escucha de eventos (`EventListenerProvider`) con **motor de detección de anomalías respaldado nativamente en Infinispan**. Opera automáticamente tanto en entornos de un solo nodo (lab/desarrollo) como en clústeres distribuidos de Kubernetes con HPA, sincronizando el estado entre pods y gestionando la expiración de ventanas con TTL nativo.

### Especificación Técnica:
* **Tipo de SPI:** `EventListenerProvider`
* **Factory Class:** `SecurityEventListenerFactory`
* **Provider ID:** `security-event-listener`
* **Paquete Java:** `org.keycloak.events.security`
* **Caché Utilizado:** Infinispan (`securityAnomalyCache` registrado dinámicamente con aislamiento y fallback en memoria)
* **Activación en Keycloak:** Realm Settings $\to$ Events $\to$ Event Listeners $\to$ Añadir `security-event-listener`

### Estructura Interna:
* **`SecurityEventListenerFactory.java` & `SecurityEventListener.java`:** Adaptadores SPI de Keycloak (Factory y Provider) que capturan el evento `LOGIN_ERROR` protegidos con manejo de excepciones resiliente para nunca interrumpir el flujo de autenticación de Keycloak.
* **`anomaly/SecurityAlertUseCase.java`:** Orquesta la resolución de usuario y realm, invoca el análisis del detector en Infinispan y coordina el despacho de la alerta.
* **`anomaly/SecurityAnomalyDetector.java`:** Motor de detección con registro dinámico por código de la caché aislada `securityAnomalyCache` en el `EmbeddedCacheManager` de Infinispan. Maneja TTL nativo, bloqueo atómico distribuido (`putIfAbsent`) y fallback concurrente ante entornos restringidos.
* **`anomaly/SecurityAlertSender.java`:** Capa de salida desacoplada que emite el banner en consola (`stdout`).
* **`anomaly/SecurityAlertPayload.java`:** Modelo de datos inmutable para el payload de incidentes.

### Detección Inteligente de Seguridad (Respaldada en Infinispan):
* **1. Detección de Fuerza Bruta / Password Spraying por IP:**  
  Rastrea en Infinispan con expiración automática (TTL) los intentos fallidos (`LOGIN_ERROR`) emitidos desde una misma dirección IP. Si supera el umbral configurado (ej: 5 fallos en cuentas distintas), dispara una alerta crítica con el conteo de cuentas afectadas.
* **2. Detección de Picos Globales de Fallos (Anomalía de Realm):**  
  Calcula la tasa global de fallos de login en todo el realm sincronizado en el clúster. Si ocurre un pico inusual (ej: 20 fallos en 60s), alerta de inmediato sobre un posible ataque distribuido o incidente generalizado.
* **3. Filtro Cero Ruido y Enfriamiento Distribuido (Antispam):**  
  Utiliza `putIfAbsent` atómico con TTL en Infinispan para garantizar que, incluso en clústeres con múltiples réplicas (HPA), solo un pod emita la alerta y se respete el periodo de enfriamiento sin duplicar logs.

> 💡 **Adaptabilidad de `SecurityAlertSender`:**  
> Al igual que las capas de envío de correo (`UserSendEmail` y `MasterSendEmail`), esta clase está concebida como una **capa de salida desacoplada**. Actualmente emite el incidente estructurado en `stdout` para visualización inmediata en entornos de laboratorio o recolectores de logs de Docker, pero está diseñada para que cualquier desarrollador pueda adaptarla al canal final que requiera (Webhook HTTP, Slack, Discord, PagerDuty, AWS SNS, Kafka, etc.).

### Variables de Entorno Configurables:
* **`KC_ALERT_IP_FAILURES`** (Defecto: `5`)  
  Umbral de fallos desde una misma dirección IP antes de alertar ataque de fuerza bruta / spraying.
* **`KC_ALERT_REALM_FAILURES`** (Defecto: `20`)  
  Umbral de fallos globales en el realm dentro de la ventana de tiempo para alertar pico masivo.
* **`KC_ALERT_WINDOW_SECONDS`** (Defecto: `60`)  
  Duración de la ventana deslizante de análisis temporal en segundos.
* **`KC_ALERT_COOLDOWN_SECONDS`** (Defecto: `120`)  
  Segundos de espera mínima antes de repetir una misma alerta (protección antispam).

### Salida en Consola (stdout):
```text
=========================================================================================
>>> [KEYCLOAK SPI - SECURITY ANOMALY ALERT]
>>> Alert Type : IP_BRUTE_FORCE
>>> Realm      : master
>>> Client ID  : security-admin-console
>>> Source IP  : 192.168.1.150
>>> Last User  : admin
>>> Message    : La IP 192.168.1.150 ha acumulado 5 intentos fallidos en los últimos 60 segundos afectando a 4 cuenta(s) distinta(s).
=========================================================================================
```
