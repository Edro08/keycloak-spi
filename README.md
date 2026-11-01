# keycloak-spi

[![Keycloak](https://img.shields.io/badge/Keycloak-26.x%2B-blue.svg?logo=keycloak&logoColor=white)](https://www.keycloak.org/)
[![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.x-4695EB.svg?logo=quarkus&logoColor=white)](https://quarkus.io/)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Implementación de SPIs (Service Provider Interfaces) para **Keycloak 26.8.1** sobre **Java 21 LTS** (Quarkus runtime).

El proyecto contiene proveedores desacoplados con sus correspondientes fábricas (`Factory`), identificadores (`Provider ID`) y declaraciones de servicio en `META-INF/services/`.

---

## 🧊 SPI: Reset Credential Email (User)

Proveedor de autenticación (`Authenticator`) para la recuperación de contraseñas de usuarios en realms de aplicación.

### Especificación Técnica
* **Tipo de SPI:** `Authenticator`
* **Factory Class:** `UserResetCredentialEmailFactory`
* **Provider ID:** `keycloak-spi-send-email`
* **Nombre en UI:** `NEW RESET CREDENTIAL EMAIL`
* **Paquete Java:** `org.keycloak.authentication.authenticators.resetcred.user`
* **Realm Destino:** Realms de aplicación

### Pipeline de Ejecución
1. **`UserEventSave`:** Registra el evento de inicio de reseteo en Keycloak (`SEND_RESET_PASSWORD`).
2. **`UserValidateUser`:** Valida la existencia del usuario y la presencia de dirección de correo electrónico (retorna error HTTP 400 si no existe correo registrado).
3. **`UserGenerateToken`:** Emite un token de acción (`ExecuteActionsActionToken`) asociado al cliente con la acción `UPDATE_PASSWORD`.
4. **`UserSendEmail`:** Capa de salida encargada de procesar el enlace generado.

> 💡 **Consideraciones de Integración:**  
> La clase `UserSendEmail` actúa como interfaz de salida desacoplada. Actualmente emite el enlace generado a `stdout` para entornos de pruebas, permitiendo su sustitución por implementaciones SMTP, APIs de mensajería (SendGrid, AWS SES) o colas asíncronas.

### Parámetros Configurables
* `RedirectUri` *(Opcional)*: URL de retorno tras completar el cambio de credencial.
* `LifeSecondsToken` *(Default: 86400)*: Tiempo de validez del enlace en segundos.

### Formato de Salida en Consola
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

Proveedor de autenticación (`Authenticator`) dedicado a la recuperación de acceso administrativo en el realm **`master`**.

### Especificación Técnica
* **Tipo de SPI:** `Authenticator`
* **Factory Class:** `MasterResetCredentialEmailFactory`
* **Provider ID:** `keycloak-spi-send-email-master`
* **Nombre en UI:** `MASTER RESET CREDENTIAL EMAIL (LOG ONLY)`
* **Paquete Java:** `org.keycloak.authentication.authenticators.resetcred.master`
* **Realm Destino:** Realm `master`

### Pipeline de Ejecución
1. **`MasterEventSave`:** Registra el evento administrativo en `master` (`SEND_RESET_PASSWORD`).
2. **`MasterValidateUser`:** Valida que la ejecución pertenezca al realm `master` y que el usuario exista. Permite la ejecución incluso si la cuenta administrativa carece de correo registrado.
3. **`MasterGenerateToken`:** Genera el token asociándolo al cliente `security-admin-console` con redirección a `/admin/master/console/`.
4. **`MasterSendEmail`:** Capa de salida para trazabilidad administrativa.

> 💡 **Consideraciones de Integración:**  
> Por defecto emite el enlace directamente a la consola (`stdout`) para su uso directo desde los logs del servidor, pero puede adaptarse a canales de alerta administrativa (Slack, webhook, correo seguro, etc.).

### Parámetros Configurables
* `RedirectUri` *(Default: /admin/master/console/)*: Redirección automática a la consola de administración.
* `LifeSecondsToken` *(Default: 86400)*: Tiempo de validez del token en segundos.

### Formato de Salida en Consola
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

Extensión de la API administrativa (`AdminRealmResourceProvider`) para registrar eventos de auditoría donde un actor realiza operaciones sobre un usuario objetivo.

### Especificación Técnica
* **Tipo de SPI:** `AdminRealmResourceProvider`
* **Factory Class:** `BootstrapFactory`
* **Provider ID:** `audit`
* **Endpoint:** `POST /admin/realms/{realm}/audit/events`
* **Paquete Java:** `org.keycloak.endpoints.audit.event`
* **Autorización:** Token Bearer con rol `manage-users`

### Estructura
* **`handler/EventHandler.java`:** Endpoint JAX-RS que valida el contexto de seguridad y delega al caso de uso.
* **`application/EventSaveUseCase.java`:** Orquestador de resolución de identidades y persistencia de eventos en el store de Keycloak.
* **`domain/`:** Modelos de datos inmutables (`EventRecord`, `EventRequest`, `EventResponse`).

### Contrato de la API

#### Solicitud (`POST /admin/realms/{realm}/audit/events`)
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
> *El campo `"actor"` es opcional; si se omite, se infiere del subject presente en el token de autenticación.*

#### Respuesta (`200 OK`)
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

## 🧊 SPI: Security Event Listener (Detección de Intentos Fallidos)

Receptor de eventos (`EventListenerProvider`) que procesa fallos de autenticación (`LOGIN_ERROR`) y gestiona umbrales de alerta mediante una caché aislada de Infinispan.

### Especificación Técnica
* **Tipo de SPI:** `EventListenerProvider`
* **Factory Class:** `SecurityEventListenerFactory`
* **Provider ID:** `security-event-listener`
* **Paquete Java:** `org.keycloak.events.security`
* **Almacenamiento:** Caché `securityAnomalyCache` en Infinispan (definida dinámicamente con fallback en memoria)
* **Habilitación:** Realm Settings $\to$ Events $\to$ Event Listeners $\to$ Agregar `security-event-listener`

### Estructura
* **`SecurityEventListenerFactory.java` & `SecurityEventListener.java`:** Proveedor de eventos que captura `LOGIN_ERROR` encapsulado para aislar excepciones de la transacción principal.
* **`anomaly/SecurityAlertUseCase.java`:** Extrae metadatos del evento (realm, IP, usuario) y evalúa los umbrales configurados.
* **`anomaly/SecurityAnomalyDetector.java`:** Gestión de contadores temporales con expiración TTL en Infinispan y deduplicación atómica (`putIfAbsent`).
* **`anomaly/SecurityAlertSender.java`:** Capa de salida desacoplada que formatea el reporte de alerta a `stdout`.
* **`anomaly/SecurityAlertPayload.java`:** Estructura del evento de alerta.

> 💡 **Consideraciones de Integración:**  
> `SecurityAlertSender` emite el reporte de alerta a `stdout`. Al estar desacoplada, puede extenderse para enviar eventos a Webhooks, Slack, Discord, PagerDuty, AWS SNS o Kafka.

### Lógica de Detección
1. **Tasa de Fallos por IP (Fuerza Bruta / Spraying):**  
   Registra los intentos fallidos asociados a una dirección IP en una ventana de tiempo móvil (`KC_ALERT_WINDOW_SECONDS`). Si se alcanza el umbral de intentos (`KC_ALERT_IP_FAILURES`), se emite un reporte indicando el número de intentos y las cuentas objetivo distintas.
2. **Tasa Global de Fallos en el Realm:**  
   Registra el volumen acumulado de fallos en todo el realm. Si se supera el límite global (`KC_ALERT_REALM_FAILURES`), se dispara una notificación de incremento anómalo.
3. **Periodo de Enfriamiento (Cooldown Distribuido):**  
   Aplica una clave atómica con TTL (`KC_ALERT_COOLDOWN_SECONDS`) para evitar alertas reiteradas por el mismo origen durante el intervalo configurado.

### Variables de Entorno
| Variable | Tipo | Defecto | Descripción |
| :--- | :---: | :---: | :--- |
| `KC_ALERT_IP_FAILURES` | Entero | `5` | Intentos fallidos por IP antes de disparar alerta |
| `KC_ALERT_REALM_FAILURES` | Entero | `20` | Intentos fallidos acumulados en el realm antes de alertar |
| `KC_ALERT_WINDOW_SECONDS` | Entero | `60` | Ventana de tiempo de análisis en segundos |
| `KC_ALERT_COOLDOWN_SECONDS` | Entero | `120` | Intervalo de silencio posterior a una alerta generada |

### Formato de Salida en Consola
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

---

## 🧊 SPI: Metrics Event Listener (Telemetría Prometheus)

Receptor de eventos (`EventListenerProvider`) que transforma las operaciones de autenticación de usuarios y acciones administrativas en métricas dimensionales para Prometheus vía Micrometer.

### Especificación Técnica
* **Tipo de SPI:** `EventListenerProvider`
* **Factory Class:** `MetricsEventListenerFactory`
* **Provider ID:** `metrics-listener`
* **Paquete Java:** `org.keycloak.events.metrics`
* **Almacenamiento:** En memoria RAM (Micrometer Core) — no utiliza Infinispan ni base de datos.
* **Habilitación:** Realm Settings $\to$ Events $\to$ Event Listeners $\to$ Agregar `metrics-listener`

### Métricas Exportadas en `/metrics`
* **`keycloak_user_events_total`**: Contador de eventos de autenticación de usuarios.
  * Etiquetas (`labels`): `realm`, `client_id`, `type` (e.g. `LOGIN`, `LOGIN_ERROR`, `REGISTER`, `UPDATE_PASSWORD_ERROR`), `error` (e.g. `invalid_user_credentials`, `user_not_found`, `none`).
* **`keycloak_admin_events_total`**: Contador de operaciones en la consola de administración.
  * Etiquetas (`labels`): `realm`, `resource_type`, `operation_type`, `error`.

---

## ⚡ Inicio Rápido

### 1. Compilación
```bash
mvn clean package
```

### 2. Instalación en Docker
```bash
# Copiar el artefacto al directorio de providers de Keycloak
docker cp target/keycloak-spi.jar <contenedor_keycloak>:/opt/keycloak/providers/

# Reiniciar el contenedor para procesar los providers
docker restart <contenedor_keycloak>

# Verificar logs de inicio
docker logs -f <contenedor_keycloak>
```

---

## 📄 Licencia

Distribuido bajo la licencia [Apache 2.0](LICENSE). Consulta los archivos [LICENSE](LICENSE) y [NOTICE](NOTICE) para más información.
