## keycloak-spi

**Descripción:** Base de SPIs personalizables para Keycloak, diseñados para facilitar la extensión de funcionalidades clave.<br>
Puedes adaptar o ampliar estos puntos de integración para ajustarlos a los flujos de tu negocio.<br>

### 🔹 Authentication
Nuevo flujo "Reset Credential Email" con pasos personalizables:
- Paso 1: Guardar evento del usuario
- Paso 2: Validar usuario
- Paso 3: Generar token
- Paso 4: Enviar correo

### 🔹 Required Actions
- Nueva acción "Update Password" personalizada:
  - Puedes integrarla fácilmente a tus propios flujos de seguridad

### 🔹 Endpoints Personalizados
- Endpoints base para administración:
  - Punto de partida para construir tus propios endpoints admin
