# SIGAI-WS

**Sistema Integral de Gestión Académica Institucional — Wellspring School**

Implementación en construcción a partir de la Propuesta Técnica del
10/05/2026. Backend Java 21 / Spring Boot 3 (MVC + Thymeleaf, sesiones de
servidor), base de datos Microsoft SQL Server, arquitectura monolítica
modular en capas.

## Estado actual

Fase 0 + Fase 1 del roadmap propio del documento (sección 9), completas y
funcionales:

- ✅ Infraestructura: seguridad (login, bloqueo de cuenta, recuperación de
  contraseña), auditoría institucional, cifrado de datos sensibles,
  internacionalización ES/EN.
- ✅ **Esquema de base de datos completo** para los 14 módulos del sistema
  (35 tablas, Flyway).
- ✅ Módulo **Usuarios y Roles** (sección 5.12).
- ✅ Módulo **Configuración Institucional** (sección 5.14): Ciclos
  Lectivos, Materias, Cursos/Divisiones, Aulas, Workshops (catálogo),
  Parámetros Académicos.
- ✅ Módulo **Gestión de Alumnos** completo (sección 5.1): legajo digital,
  tutores, documentos, pase de alumno (con generación de PDF), repetición
  de año.

Pendiente (esquema de base de datos ya existe, falta lógica/UI):
Asistencia, Calificaciones, Disciplina, Horarios, Workshops (asignación),
Educación Física, Reportes, Dashboards, Mensajería, Portal de Padres,
Alertas.

## Documentación

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — cómo está organizado el
  código y por qué.
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — cada ambigüedad/contradicción
  encontrada en la Propuesta Técnica y cómo se resolvió (léelo antes de
  cuestionar por qué algo se hizo de una forma específica).
- [`docs/DEV_GUIDE.md`](docs/DEV_GUIDE.md) — cómo levantar el entorno,
  correr tests, y el patrón a seguir para agregar el próximo módulo.

## Quickstart

```bash
docker compose up -d
cp .env.example .env   # completar SIGAI_AES_KEY y credenciales del admin inicial
mvn spring-boot:run
```

En Windows con `.env` ya configurado también podés usar `.\run.ps1`.

Compilado y ejecutado exitosamente end-to-end (JDK 21, Maven, migraciones de
Flyway aplicadas contra SQL Server real). Ver
[`docs/DEV_GUIDE.md`](docs/DEV_GUIDE.md) para el detalle completo, incluyendo
la sección de compatibilidad con SQL Server local/legacy sin Docker.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring MVC, Spring Security, Spring Data JPA |
| Base de datos | Microsoft SQL Server 2022, Flyway |
| Frontend | Thymeleaf (server-side), CSS/JS propios (sin framework SPA) |
| PDF | OpenPDF |
| Email | Spring Mail (SMTP interno) |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers |
