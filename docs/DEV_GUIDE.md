# Guía para Desarrolladores — SIGAI-WS

## Prerrequisitos

- **JDK 21** (el proyecto usa `java.version=21` en `pom.xml`).
- **Maven 3.9+**.
- **Docker** (para SQL Server y MailHog en desarrollo local vía
  `docker-compose.yml`; también lo usan los tests de integración `*IT.java`).

> **Actualización:** el proyecto ya fue compilado y ejecutado exitosamente
> (JDK 21 + Maven 3.9.9 instalados manualmente, ver más abajo), incluyendo
> arranque completo contra una instancia real de SQL Server con las 13
> migraciones de Flyway aplicadas. `mvn clean verify` es el primer paso
> recomendado en cualquier entorno nuevo para confirmar que sigue compilando.

## Puesta en marcha local

### Opción A — Docker (recomendada si tenés Docker Desktop)

```bash
docker compose up -d          # SQL Server + MailHog
cp .env.example .env          # completar SIGAI_AES_KEY, credenciales del admin inicial
mvn spring-boot:run           # o ./run.ps1 en Windows (ver mas abajo)
```

### Opción B — SQL Server local ya instalado (sin Docker)

Así se corrió este proyecto la primera vez, contra una instancia SQL Server
Express **2008 R2** preexistente. Si tu SQL Server es más nuevo (2016+) la
mayoría de estos pasos no hacen falta, pero quedan documentados porque son
comunes en instalaciones de SQL Server Express por defecto:

1. **Habilitar TCP/IP** (deshabilitado por defecto en SQL Server Express):
   `SQLServerManager<N>.msc` (`SQLServerManager10.msc` para 2008 R2, buscar la
   versión correcta según tu instalación) → *SQL Server Network Configuration*
   → *Protocols for \<instancia\>* → habilitar **TCP/IP** → pestaña
   *IP Addresses* → sección **IPAll** → fijar un **TCP Port** (ej. `14330`,
   evitando 1433 si ya lo usa otra instancia) → reiniciar el servicio de SQL
   Server. Esto evita depender del servicio *SQL Server Browser* (suele estar
   detenido y requiere admin para iniciarlo).
2. **Autenticación de Windows** (`integratedSecurity=true`): requiere la DLL
   nativa `mssql-jdbc_auth`. Ya está resuelta en `pom.xml` (dependencia con
   `<type>dll</type>`, versión debe coincidir con `mssql-jdbc.version`) +
   `maven-dependency-plugin` la copia a `./native/` en `generate-resources` +
   `spring-boot-maven-plugin` pasa `-Djava.library.path="${project.basedir}/native"`.
   Alternativa más simple si da problemas: crear un login SQL con
   usuario/contraseña y usar `DB_USER`/`DB_PASSWORD` en vez de integrated security.
3. **SQL Server viejo (≤2008 R2) + JDK moderno = handshake TLS roto**: el JDK
   deshabilita TLS 1.0/1.1 por defecto (`jdk.tls.disabledAlgorithms` en
   `java.security`) y SQL Server 2008 R2 no habla nada más nuevo. Se resuelve
   con un archivo de override (`native/allow-tls10.security`, ya incluido)
   pasado vía `-Djava.security.properties=` en el mismo plugin. **No hace
   falta contra SQL Server 2016+** (ya soporta TLS 1.2).
4. **`INFORMATION_SCHEMA.SEQUENCES` no existe en SQL Server ≤2008 R2**
   (se agregó en 2012): Hibernate la consulta al validar el esquema en el
   arranque si `ddl-auto=validate`. Setear `JPA_DDL_AUTO=none` en `.env` para
   desactivar esa validación extra (Flyway ya garantiza el esquema correcto).
   **No hace falta contra SQL Server 2012+.**
5. **La base de datos debe existir y estar VACÍA** antes del primer arranque
   (Flyway crea las tablas, pero no la base). Si la base ya tiene tablas de
   otro proyecto, Flyway las toma como "baseline" y salta la migración V1,
   rompiendo todo lo que depende de ella — hay que usar una base nueva y vacía.
6. Configurar `DB_URL` en `.env` con el resultado (ver `.env.example`,
   sección "OPCION B").

### Instalar JDK 21 y Maven en Windows sin admin (si hace falta)

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e --accept-package-agreements --accept-source-agreements
# Apache Maven no esta en winget: descargar el zip y extraer
Invoke-WebRequest "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" -OutFile "$env:TEMP\maven.zip"
Expand-Archive "$env:TEMP\maven.zip" -DestinationPath "C:\tools" -Force
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot", "User")
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\tools\apache-maven-3.9.9\bin", "User")
```

### Arrancar la app

`run.ps1` (en la raíz del proyecto) carga `.env` y ejecuta `mvn spring-boot:run`
en un solo paso — usarlo en vez de repetir la carga manual de variables:

```powershell
.\run.ps1
```

La app queda en `http://localhost:8443` (perfil `dev`: `COOKIE_SECURE=false`,
funciona por HTTP sin certificado). Al arrancar sin ningún Superadmin existente,
`BootstrapDataRunner` crea uno usando `SIGAI_BOOTSTRAP_ADMIN_EMAIL` /
`SIGAI_BOOTSTRAP_ADMIN_PASSWORD`.

Emails de prueba: abrir `http://localhost:8025` (UI de MailHog) para ver los
correos de bienvenida / recuperación de contraseña sin necesidad de un SMTP
real.

## Tests

```bash
# Tests unitarios (Mockito, sin Docker, rápidos) — se ejecutan con "mvn test"
mvn test

# Tests de integración (*IT.java, requieren Docker corriendo — Testcontainers
# levanta un SQL Server real). El sufijo "IT" los excluye automáticamente del
# patrón por defecto de Surefire ("mvn test" NO los ejecuta).
# Para correrlos manualmente hoy (sin el plugin Failsafe configurado):
mvn test -Dtest=AlumnoPersistenciaIT
```

Si en una fase posterior se agregan más tests de integración, conviene sumar
el plugin `maven-failsafe-plugin` al `pom.xml` para que corran en `mvn
verify` de forma estándar.

## Estructura del proyecto

Ver `docs/ARCHITECTURE.md`.

## Flujo para agregar un módulo nuevo

Cada módulo de negocio de la Propuesta Técnica (Asistencia, Calificaciones,
Disciplina, etc.) sigue el mismo patrón que `modules/alumno`:

1. **El esquema de base de datos ya existe** (migraciones V5-V13). Verificar
   que coincide con lo que se va a mapear; si falta algo, agregar una
   migración `V14__...sql` nueva (nunca editar una migración ya aplicada).
2. Crear `domain/` (entidades JPA), `repository/`, `dto/` (formularios,
   nunca bindear HTML directo a una entidad).
3. Crear `service/` con la lógica de negocio, citando en comentarios la
   sección exacta del documento que se está implementando.
4. Crear `web/` (`@Controller`) con `@PreAuthorize` replicando la tabla de
   permisos por rol de esa sección del documento.
5. Crear las vistas Thymeleaf reutilizando `fragments/head`, `fragments/topbar`,
   `fragments/sidebar`, `fragments/footer`, `fragments/toasts` y las clases
   CSS de `static/css/main.css` (no crear un sistema de diseño nuevo).
6. Agregar el link correspondiente en `fragments/sidebar.html` con el
   `sec:authorize` adecuado.
7. Tests unitarios de las reglas de negocio nuevas (Mockito, sin DB).
8. Actualizar `docs/DECISIONS.md` si aparece alguna ambigüedad nueva del
   documento — **no resolverla en silencio**.

## Trabajo pendiente conocido (no inventar soluciones no documentadas)

| Pendiente | Sección del documento | Por qué no se hizo ahora |
|---|---|---|
| Motor de reglas de Asistencia (umbrales, Condición Libre) | 5.2 | Depende de Horarios para asistencia por clase; es el siguiente módulo en el roadmap (Fase 2). |
| Vista de Alumnos filtrada por división para Docente | 5.1.2 | Depende de `asignacion_docente` (módulo Horarios). Ver ADR-009. |
| Asistente de promoción masiva de alumnos | 5.14.2 | No tiene pantalla especificada en el documento más allá del caso "repetidor" (sí implementado). Ver ADR-010. |
| Calificaciones, Disciplina, Workshops (asignación), Educación Física, Reportes, Dashboards, Mensajería, Portal de Padres, Alertas, motor de Configuración de SMTP/plantillas de email | 5.3, 5.4, 5.6, 5.7, 5.8, 5.9, 5.10, 5.11, 5.13, 5.14.2 (SMTP) | Fases 2-9 del roadmap propio del documento (sección 9). Esquema de BD ya existe. |
| Maven Wrapper (`mvnw`) | — | El wrapper requiere un binario `.jar` que no puede generarse como texto; agregarlo desde un entorno con Maven instalado (`mvn -N wrapper:wrapper`). |

## Convenciones de código

- Español para nombres de dominio (entidades, campos, mensajes de error),
  inglés para términos técnicos de infraestructura estándar (`Service`,
  `Repository`, `Controller`). Sigue la terminología literal del documento
  siempre que sea posible (ej. `condicionLibre`, no `freeStatus`).
- Comentarios solo cuando explican el *por qué* (una regla de negocio no
  obvia, una decisión que resuelve una ambigüedad del documento). No se
  documenta el *qué* cuando el código ya es autoexplicativo.
- Nunca bindear un `@ModelAttribute` de formulario directamente sobre una
  entidad JPA — siempre a través de un DTO en `dto/`.
- Toda operación de escritura relevante para trazabilidad institucional
  (alta, baja, edición, cambios de estado) debe llamar a
  `AuditService.registrar(...)`.
