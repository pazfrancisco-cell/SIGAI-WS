# Arquitectura — SIGAI-WS

Referencia: sección 3 ("Arquitectura General del Sistema") de la Propuesta
Técnica. Este documento explica cómo se tradujo esa sección a código real.

## 1. Visión general

Monolito modular en Java 21 / Spring Boot 3, arquitectura MVC clásica:
el servidor renderiza HTML (Thymeleaf) y lo envía ya construido al
navegador (sección 3.3). No es una SPA ni expone una API pública separada;
el JavaScript del cliente es puntual (toasts, tabs, modales de confirmación,
formularios dinámicos), tal como exige la sección 4.

```
Navegador (HTML + CSS + JS puntual)
        │  HTTPS, cookies de sesión HttpOnly/Secure/SameSite=Strict
        ▼
┌─────────────────────────────────────────────────────────┐
│ Spring MVC Controllers (capa de presentación/orquestación)│
├─────────────────────────────────────────────────────────┤
│ Services (lógica de negocio — reglas de la Propuesta)     │
├─────────────────────────────────────────────────────────┤
│ Spring Data JPA Repositories (acceso a datos)              │
├─────────────────────────────────────────────────────────┤
│ Microsoft SQL Server (Flyway gestiona el esquema)          │
└─────────────────────────────────────────────────────────┘
```

## 2. Estructura de paquetes

```
ar.edu.wellspring.sigaiws
├── SigaiWsApplication.java
├── config/                 # Composition root: Security, Web, JPA Auditing, Encryption
├── common/                 # Código transversal, sin conocimiento de ningún módulo de negocio
│   ├── domain/              BaseEntity (id + auditoría técnica)
│   ├── enums/                RolNombre, Genero (usados por >1 módulo)
│   ├── exception/             BusinessException, NotFoundException, DuplicateResourceException,
│   │                          GlobalExceptionHandler (@ControllerAdvice)
│   ├── crypto/                AesGcmEncryptor, EncryptedStringConverter (JPA @Converter)
│   ├── audit/                 AuditLog, AuditLogRepository, AuditService (tabla audit_log, sección 3.5)
│   ├── security/               AuthenticatedActor (contrato mínimo, evita dependencia circular), PasswordPolicy
│   ├── notification/           EmailService (SMTP interno, sección 3.2)
│   ├── storage/                 AlmacenamientoService (archivos en filesystem, sección 7.2)
│   └── util/                    HashUtil (SHA-256 para campos cifrados-pero-buscables)
├── modules/
│   ├── auth/                 Login, cambio/recuperación de contraseña, bloqueo de cuenta
│   ├── usuario/               Usuarios y Roles (sección 5.12)
│   ├── configuracion/         Ciclo Lectivo, Materias, Cursos/Divisiones, Aulas, Workshops (catálogo),
│   │                          Parámetros Académicos (sección 5.14)
│   └── alumno/                 Gestión de Alumnos (sección 5.1) — módulo de referencia de esta fase
└── web/                      Rutas transversales sin módulo propio (Home, descarga de archivos)
```

Cada módulo de negocio sigue el mismo patrón interno (Clean
Architecture "ligera", apropiada para un monolito Spring — no se introdujo
un framework de capas adicional tipo hexagonal completo porque el propio
documento (sección 3.3) especifica MVC en capas, no arquitectura hexagonal):

```
modules/<modulo>/
├── domain/       Entidades JPA. Solo conocen JPA + otras entidades. Cero lógica de framework web.
├── repository/   Interfaces Spring Data JPA. Sin lógica, solo consultas.
├── service/      Lógica de negocio y orquestación transaccional. Aquí viven las reglas de la Propuesta.
├── web/          @Controller. Traduce HTTP <-> DTOs <-> llamadas a service. Sin lógica de negocio.
└── dto/          Formularios de entrada (nunca se bindea un formulario HTML directo a una entidad).
```

**Regla de dependencia:** los módulos de negocio pueden depender de
`common` y de `configuracion` (catálogos base), nunca al revés. Cuando un
módulo de menor nivel necesita reaccionar a algo de un módulo de negocio
específico (ej. Configuración activa un ciclo lectivo nuevo y Alumnos debe
reaccionar), se usa un **evento de dominio** de Spring
(`CicloLectivoActivadoEvent`) en lugar de una llamada directa, para no
invertir la dirección de dependencias.

## 3. Seguridad (sección 3.4, 7)

- Autenticación: Spring Security, sesión de servidor (ver `docs/DECISIONS.md`
  ADR-001). `CustomUserDetailsService` + `SigaiUserPrincipal` adaptan la
  entidad `Usuario` al contrato de Spring Security.
- Autorización: `@PreAuthorize("hasRole('...')")` / `hasAnyRole(...)` a nivel
  de método de controller, replicando exactamente las tablas de permisos por
  rol de cada sección del documento (ej. 5.1.2 para Alumnos, 5.12.2 para
  Usuarios).
- Bloqueo de cuenta: `LoginAttemptListener` escucha los eventos de
  autenticación de Spring Security (no hay lógica de bloqueo manual en el
  controller de login).
- Contraseña obligatoria a cambiar en el primer ingreso:
  `ForcePasswordChangeInterceptor` (Spring MVC `HandlerInterceptor`)
  redirige cualquier request a `/cambiar-password` mientras
  `Usuario.debeCambiarPassword` sea `true`.
- Auditoría: `AuditService.registrar(...)` se invoca explícitamente desde
  los servicios en los puntos que la Propuesta marca como auditables. Se
  prefirió invocación explícita a un aspecto AOP genérico porque cada
  operación tiene una noción distinta de "antes/después" (ver comentario en
  `AuditService`).

## 4. Modelo de datos

El esquema completo (35 tablas para los 14 módulos) vive en
`src/main/resources/db/migration/V1__...sql` a `V13__...sql`, versionado con
Flyway. Se construyó completo desde el inicio (no solo las tablas de los
módulos ya implementados) para evitar migraciones destructivas más
adelante — ver ADR-003.

Cada migración está comentada con la sección del documento que la
justifica. Puntos de diseño no obvios:

- **`alumno_ciclo_lectivo`**: un registro por alumno y ciclo lectivo. Es a
  la vez la "trayectoria" (tab del legajo, sección 5.1.4) y el mecanismo
  que garantiza el reinicio anual de contadores de asistencia/disciplina
  (sección 5.14.2) — un año nuevo simplemente crea un registro nuevo con
  contadores en cero, sin necesidad de un job de "reseteo".
- **Columnas cifradas**: `*_cifrado`/`*_cifrada` en la base de datos,
  expuestas como texto plano en el objeto Java (el converter JPA cifra/
  descifra de forma transparente). Ver ADR-004.
- **`ux_ciclo_lectivo_activo`**: índice único filtrado (`WHERE activo = 1`)
  que garantiza a nivel de base de datos que solo puede haber un ciclo
  lectivo activo, sin depender únicamente de la lógica de aplicación.

## 5. Frontend

Thymeleaf + CSS/JS propios, sin framework de frontend (React, Vue, etc.),
tal como especifica la sección 3.2 ("las plantillas de servidor simplifican
el desarrollo al generar HTML directamente desde el backend"). El sistema
de diseño (`static/css/main.css`) implementa literalmente los tokens de la
sección 4: paleta de colores (4.2), tipografía (4.3), layout de 4 zonas
(4.4), botones (4.5), formularios (4.6), notificaciones (4.7) y reglas
responsive (4.8). Al final del archivo, la sección "Responsive" adapta todo a
tablet (hasta 1023px) y celular (hasta 640px): ver ADR-012 en
`docs/DECISIONS.md`.

## 6. Qué falta (y dónde está el hueco)

Ver la tabla de fases en `docs/DEV_GUIDE.md`. En resumen: el esquema de base
de datos de Asistencia, Calificaciones, Disciplina, Horarios, Workshops
(lógica de asignación), Educación Física, Reportes, Dashboards, Mensajería,
Portal de Padres y Alertas **ya existe**; sus entidades JPA, servicios,
controladores y vistas se construyen módulo por módulo en el mismo orden
que propone la sección 9 del documento.
