# Decisiones Técnicas (ADR) — SIGAI-WS

Este documento registra cada punto donde la Propuesta Técnica original era
ambigua, contradictoria, o dejaba una decisión de implementación sin
especificar, junto con la resolución adoptada y su justificación. Es la
fuente de verdad secundaria: ante cualquier duda de "por qué se hizo así",
la respuesta está acá.

---

## ADR-001 — Autenticación por sesión de servidor (no JWT)

**Contradicción detectada:** la sección 3.4 describe explícitamente cookies
de sesión seguras (HttpOnly, Secure, SameSite=Strict) y la 7.1 habla de
"control de sesiones activas en el servidor". El cronograma (Fase 0)
menciona "implementación de autenticación JWT", que es un mecanismo
stateless incompatible con sesiones de servidor como mecanismo único.

**Decisión:** Spring Security con sesiones de servidor (`HttpSession` +
cookie `JSESSIONID` con `HttpOnly`/`Secure`/`SameSite=Strict`). Es coherente
con el resto de la arquitectura (MVC monolítico, vistas renderizadas en
servidor, sección 3.3) y con la sección 3.4, que es más específica que la
mención aislada de JWT en el cronograma.

**Confirmado con el usuario:** sí (pregunta explícita, respuesta: sesiones).

---

## ADR-002 — OpenPDF en lugar de TCPDF/iText

**Problema detectado:** la tabla de stack (sección 3.2) pide "TCPDF / iText
(Java)". TCPDF es una librería exclusivamente PHP; no existe una versión
Java. No puede usarse en un backend Spring Boot.

**Decisión:** [OpenPDF](https://github.com/LibrePDF/OpenPDF), fork libre de
iText 4 con licencia LGPL/MPL, sin restricciones para uso comercial/
institucional (a diferencia de iText 5+, que es AGPL o licencia comercial paga).

**Confirmado con el usuario:** sí.

**Impacto:** `PasePdfGenerator` (módulo Alumnos) es la primera implementación
concreta; el mismo patrón se reutiliza en el Módulo de Reportes (Fase 5).

---

## ADR-003 — Alcance por fases, no todo el sistema de una vez

El documento estima 28 semanas (~7 meses) de desarrollo en 10 fases. No es
razonable ni honesto entregar "el sistema completo" en una sola sesión.

**Decisión:** se construyó completo y funcional el equivalente a **Fase 0 +
Fase 1** del roadmap propio del documento:
- Infraestructura (Spring Boot, seguridad, auditoría, cifrado, i18n).
- **Esquema de base de datos completo** para los 14 módulos (todas las
  tablas, para no requerir migraciones destructivas más adelante).
- Módulos con lógica de negocio y UI completas: Usuarios y Roles,
  Configuración Institucional, Alumnos.

Los módulos de Asistencia, Calificaciones, Disciplina, Horarios, Workshops,
Educación Física, Reportes, Dashboards, Mensajería, Portal de Padres y
Alertas tienen **su esquema de base de datos ya creado** (migraciones V5 a
V13) pero **no tienen entidades JPA, servicios ni pantallas todavía**. Se
construyen en el mismo orden que propone la sección 9 del documento.

**Confirmado con el usuario:** sí.

---

## ADR-004 — Cifrado a nivel de aplicación (AES-256-GCM)

La sección 7.2 pide cifrado de columnas sensibles sin especificar mecanismo.

**Decisión:** cifrado en la capa de aplicación vía `AttributeConverter` de
JPA (`EncryptedStringConverter` + `AesGcmEncryptor`), en lugar de Always
Encrypted / TDE nativos de SQL Server. Motivo: portabilidad (no depende de
una edición específica de SQL Server), control total sobre la clave, y
capacidad de rotarla sin depender de herramientas de administración de BD.

Los campos DNI (alumno y tutor) además guardan un **hash SHA-256
determinista** (`dni_hash`) para permitir validar unicidad y buscar sin
descifrar todo el listado — el cifrado por sí solo no permite `WHERE dni = ?`
eficiente porque el IV es aleatorio en cada cifrado.

**Confirmado con el usuario:** sí.

---

## ADR-005 — Interpretación de combinaciones de roles (sección 5.12.3)

El texto original: *"Las combinaciones válidas son: Preceptor + Administrativo,
Administrativo + Preceptor, Directivo + Administrativo, Directivo +
Preceptor. Un docente no puede combinarse con otros roles operativos por
diseño."*

**Ambigüedad:** no se especifica si Superadmin y Padre/Tutor pueden
combinarse con algo, ni si los tres roles combinables (Directivo,
Administrativo, Preceptor) pueden estar los tres juntos a la vez.

**Decisión** (`RolCombinacionValidator`):
- Superadmin, Docente y Padre/Tutor son **roles exclusivos**: nunca se
  combinan con ningún otro (ni entre sí).
- Directivo, Administrativo y Preceptor solo se combinan **de a pares**,
  exactamente en los 3 pares listados. La combinación de los tres juntos
  **se rechaza** por no estar documentada explícitamente.

Cubierto por `RolCombinacionValidatorTest`.

---

## ADR-006 — Alcance de la internacionalización (ES|EN)

La sección 4.4 menciona un selector de idioma ES|EN en el topbar, pero
**ningún contenido del documento está escrito en inglés** — todas las
reglas de negocio, textos de UI y nombres de campos están únicamente en
español.

**Decisión:** el mecanismo de i18n queda completamente funcional
(`LocaleResolver`, `LocaleChangeInterceptor`, `messages_en.properties`),
pero el archivo en inglés solo traduce las etiquetas genéricas de
navegación/UI. Las reglas de negocio, validaciones y contenido específico
del dominio (boletines, notificaciones, etc.) permanecen en español hasta
que la institución provea las traducciones.

---

## ADR-007 — CRUD de Aulas inferido

La sección 5.5.1 lista "Administración de aulas" como funcionalidad y el
modal de horarios (5.5.3) usa un selector de aula, pero **no hay una
pantalla propia especificada** con URL, campos o permisos (a diferencia de
Alumnos, que sí tiene esa especificación completa en 5.1.3-5.1.8).

**Decisión:** se infirió un CRUD mínimo (`Aula`: nombre, capacidad, activa)
con el mismo nivel de acceso que Materias y Workshops (Superadmin/Directivo/
Administrativo), por ser estructura académica base del mismo tipo.

---

## ADR-008 — Lógica condicional del umbral de Condición Libre

La sección 5.2.3 especifica una regla más matizada que la tabla resumen de
alertas (5.13.2): al llegar a 20 faltas, el sistema evalúa si ≥50% están
justificadas. Si sí, se habilitan 8 faltas adicionales (Condición Libre a
las 28). Si no, **la próxima falta (21) activa la Condición Libre
directamente** — no hay que esperar a la número 28 en ese caso.

**Decisión:** se modela `alumno_ciclo_lectivo.condicion_libre` como un flag
booleano actualizado por el motor de reglas del módulo de Asistencia
(pendiente, Fase 2), implementando la lógica condicional de 5.2.3 como
fuente de verdad (es la sección más específica). La tabla 5.13.2 se toma
como resumen simplificado, no como regla literal alternativa.

**Nota:** el modelo de datos y el catálogo de umbrales configurables
(`parametro_academico`) ya están listos; la lógica de cálculo se implementa
junto con el módulo de Asistencia.

---

## ADR-009 — Vista de Alumnos filtrada por Docente (gap documentado)

La tabla de permisos 5.1.2 exige que el rol Docente vea el listado de
alumnos "filtrado a sus divisiones", lo cual depende de la tabla
`asignacion_docente` (módulo Horarios, Fase 2, no implementado en esta
entrega).

**Decisión:** en lugar de otorgar acceso completo sin filtrar (lo que
violaría el principio de mínimo privilegio implícito en la tabla 5.1.2), el
rol Docente **no tiene acceso** al listado/legajo de Alumnos hasta que el
módulo de Asignación Docente esté implementado. Se documenta como
pendiente en `docs/DEV_GUIDE.md` en lugar de improvisar una regla de
autorización no especificada.

---

## ADR-010 — Asistente de promoción masiva (gap documentado)

La sección 5.14.2 menciona que activar un nuevo ciclo lectivo dispara una
"solicitud de confirmación de continuidad de alumnos (cuáles repiten,
cuáles pasan de año)", pero no especifica pantalla, flujo ni reglas para el
caso general de promoción (sí para el caso específico de repetidor, sección
5.1.7, que **está** implementado en `AlumnoService.registrarRepeticion`).

**Decisión:** no se inventó una automatización no descripta. El modelo de
datos (`alumno_ciclo_lectivo`, un registro por alumno y ciclo) ya soporta la
funcionalidad; falta construir la pantalla de asistente de promoción masiva
como tarea explícita de una fase posterior.

**Actualización:** implementado en `PromocionService` (`/configuracion/promocion`).
Al activar un ciclo se abre el asistente: propone para cada alumno activo del
ciclo anterior "pasa" (año + 1, misma letra), "repite" o "egresa" (6° año,
nuevo estado `EGRESADO`). Todo es editable; al confirmar se crea su
`alumno_ciclo_lectivo` del ciclo nuevo. La regla de "repite" depende de las
mesas de examen: ver ADR-011.

---

## ADR-011 — Materias adeudadas y mesas de diciembre/febrero

**Regla del colegio:** una materia con promedio anual desaprobado no hace
repetir por sí sola. El alumno la rinde en la mesa de diciembre; si le quedan
3 o más, tiene otra instancia en febrero. Después de febrero, si debe hasta 2
materias pasa de año debiéndolas ("previas", que rinde en las mesas de julio,
diciembre y febrero siguientes); si debe más de 2, repite. Las previas de
años anteriores cuentan para ese límite.

**Decisión:**
- `materia_adeudada` (V34): una fila por alumno, materia y ciclo, con el curso
  en que se cursó (la misma materia se dicta de 1° a 6°). Se generan al
  cerrar el 2° cuatrimestre y, por si no se cerró, al activar el ciclo
  siguiente (`MesaExamenService.sincronizarCiclo`, idempotente).
- `nota_mesa`: resultado por turno (DICIEMBRE, FEBRERO, JULIO) y año. Se
  aprueba con la misma nota mínima del año (parámetro académico). La carga el
  docente que dicta esa materia en ese año, o gestión (`/mesas`).
- La promoción sugiere "repite" si después de febrero quedan más de 2
  materias adeudadas (o quedó en Condición Libre). Al confirmar "repite", las
  adeudadas de ese año pasan a `RECURSADA`: las vuelve a cursar.
- La mesa de febrero queda abierta a cualquier alumno que deba materias (no
  solo a los que deben 3 o más): la regla de pasa/repite se aplica igual.

---

## ADR-012 — Diseño responsive (celular y tablet)

La sección 4 de la Propuesta definió un diseño de escritorio. Se adaptó todo
el sistema a celular y tablet sin cambiar de framework (CSS propio, sin
Bootstrap/Tailwind), en la sección "Responsive" al final de `main.css`:

- **Tres anchos:** celular hasta 640px, tablet 641–1023px, escritorio desde
  1024px (sin cambios respecto del diseño original).
- **Celular y tablet:** barra superior en un renglón (tema e idioma pasan al
  menú del avatar) y menú lateral deslizable, que se cierra tocando afuera.
- **Tablas:** en celular, los listados se ven como tarjetas usando el
  `data-label` de cada celda. Las grillas (planilla de notas, horarios,
  calendarios) llevan `class="tabla-grilla"` y siguen siendo tabla con
  scroll; con `con-columna-fija` la primera columna queda fija. En tablet,
  las tablas muy anchas llevan `tarjetas-en-tablet`.
- **Componentes puntuales:** horario de a un día con pestañas
  (`horario-semanal`), toma de asistencia compacta (`tabla-asistencia`),
  mensajes con lista y conversación en pantallas separadas, listas largas
  recortadas con "Ver todas" (`data-mostrar-primeros`).
- **Táctil:** controles de al menos 44px y campos con letra de 16px (evita el
  zoom automático del iPhone).

**Cómo mantenerlo:** una tabla nueva sale como tarjetas en celular sin hacer
nada, siempre que cada `<td>` tenga `data-label`. Si es una grilla (columnas
dinámicas o días), agregarle `tabla-grilla`.

---

## Otras observaciones menores

- **Numeración rota en el documento original:** salta de "9. Cronograma" a
  "10.2 Priorización de Módulos Críticos" sin que exista un "10." ni
  "10.1". Posible contenido faltante (¿sección de presupuesto?) en el PDF
  fuente. No bloqueó ninguna decisión de implementación.
- **Mención de "Sesiones PHP/Java"** en la tabla de stack (sección 3.2): se
  interpretó como error de redacción/copy-paste, dado que el resto del
  documento es explícito sobre un backend 100% Java/Spring Boot.
