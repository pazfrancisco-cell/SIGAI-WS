# Cambios y adiciones respecto a la Propuesta Técnica — SIGAI-WS

Este documento junta todo lo que el sistema **hace hoy** y que la Propuesta
Técnica original no contemplaba, o contemplaba de otra forma. Es el
complemento de `docs/DECISIONS.md`: ese archivo resuelve ambigüedades y
contradicciones que ya estaban en la Propuesta (cosas que había que decidir
para poder arrancar a programar); este archivo son cosas que **se agregaron
después**, durante el uso real del sistema, y que la Propuesta debería
actualizar para reflejar lo que efectivamente se construyó.

Está ordenado de lo más simple (ajustes de pantallas ya definidas) a lo más
complejo (funcionalidades nuevas que no existían en el documento original, y
cambios de arquitectura de datos). Cada punto dice: qué decía o asumía la
Propuesta, qué hay ahora, y por qué se hizo el cambio.

---

## 1. Ajustes menores a pantallas ya definidas (filtros, columnas, atajos)

Estos son cambios que no alteran el alcance funcional de un módulo descripto
en la Propuesta — sólo lo hacen más rápido de usar en el día a día real.

| Pantalla | Qué decía la Propuesta | Qué hay ahora |
|---|---|---|
| Alumnos (listado, sección 5.1.3) | Listado con filtros básicos y exportación a PDF/Excel | Se agregó un renglón de **atajos de filtro** ("En condición libre", "Con pase pendiente") que aplican el filtro con un click, sin tener que abrir el desplegable; el nombre y apellido del alumno ahora muestra el **DNI debajo** para desambiguar homónimos; la paginación pasó de un texto informativo ("Página 1 de 6") a **botones reales** (Anterior / números / Siguiente) |
| Calificaciones (carga, sección 5.3.4) | Selector manual de cuatrimestre, sin cuatrimestre por defecto definido | El cuatrimestre se **autocompleta según la fecha actual** contra las fechas de inicio/fin de cada cuatrimestre del ciclo lectivo activo (ver punto 8.1); el promedio de cada alumno se colorea (rojo si está por debajo del umbral de bajo rendimiento, verde si está por arriba de un umbral alto); el indicador de autoguardado pasó de uno por fila a **uno solo, global**, arriba de la tabla ("Todo guardado · 14:32") |
| Educación Física — grupos y pruebas (sección 5.7.2/5.7.4) | Cuatrimestre por defecto fijo (no especificado) | Mismo criterio de cuatrimestre-por-fecha que Calificaciones, aplicado a la creación de grupos y a la carga de resultados |
| Boletín (botón desde el legajo, sección 5.1.8) | Selector de cuatrimestre sin default | Preselecciona el cuatrimestre en curso |
| Materias (catálogo, sección 5.14.2) | Estado "Activa/Inactiva" | Renombrado a "Se dicta / Sin dictar este año", que es lo que el campo realmente representa (no es que la materia esté deshabilitada, es que no se dicta ese año lectivo) |
| Usuarios (listado, sección 5.12.4) | Columnas separadas para nombre y email, último acceso en fecha completa | Nombre + email + avatar con inicial unificados en una sola celda; "Último acceso" en formato relativo ("Hoy, 14:32", "Ayer, 09:10") y sólo cae a fecha completa pasada una semana |

---

## 2. Rediseño visual completo (identidad, tipografía, componentes)

La Propuesta especifica una paleta de colores, tipografía y sistema de
componentes en la sección 4 ("Diseño de Interfaz"). Se mantuvo la paleta
institucional **sin cambios** (azul institucional, verde esmeralda, naranja
de alerta, rojo crítico), pero se revisaron los valores de contraste, tamaño
y densidad porque no cumplían un mínimo de legibilidad cómoda:

- **Tipografía**: la Propuesta pedía Inter cargada con `local("Inter")`, que
  en la gran mayoría de las máquinas no está instalada y caía silenciosamente
  a la fuente por defecto del sistema operativo. Se reemplazó por **Source
  Sans 3**, alojada localmente (`static/fonts/`, formato variable-woff2, sin
  depender de internet en producción ni de Google Fonts como CDN).
- **Tamaño base de texto**: 14px → **16px** en body, 12px → **15px** en texto
  secundario y labels de formulario. A 14px/12px varios textos (fechas,
  ayudas, rótulos de campo) quedaban por debajo de un piso de lectura cómoda.
- **Altura de controles**: inputs 38px → **50px**, botones 42px → **52px**,
  botones de ícono 34px → **44px** (mínimo táctil recomendado).
- **Sidebar**: ancho 240px → **286px** (240px cortaba etiquetas largas como
  "Solicitudes de Corrección"); los 19 emoji usados como íconos se
  reemplazaron por **SVG dibujados a mano** (coherente con la regla de "sin
  librerías externas" del proyecto), y el menú quedó **dividido en 5
  secciones** con encabezado (Alumnos y Seguimiento / Horarios y Materias /
  Comunicación / Reportes y Configuración / Administración) en vez de una
  lista plana de ítems.
- **Grupos del sidebar** (Asistencia, Disciplina, Horarios, Materias): antes
  eran `<details>` colapsados por defecto — es decir, un usuario que no usa
  el sistema todos los días no veía indicio de que ahí adentro había 2 o 3
  opciones más. Ahora quedan **siempre abiertos**.
- **Filas de tabla**: se sacó el sombreado alternado (zebra striping); con
  filas más altas (por el tamaño de fuente nuevo) el sombreado competía
  visualmente con los chips de estado, que es la información que importa.
- **Tema oscuro**: la Propuesta no especifica cuál es el tema por defecto al
  entrar por primera vez; el sistema arrancaba en oscuro salvo elección
  explícita. Se invirtió a **claro por defecto** (un sistema de gestión
  institucional se usa de día, muchas veces con proyector).

Página de login (sección 6, flujo de acceso): se agregó fondo con foto aérea
del colegio, tarjeta más grande (380px → 460px), logo + nombre de la
institución arriba del formulario, botón "Mostrar/Ocultar" en el campo de
contraseña (aplicado también a "Cambiar contraseña" y "Restablecer
contraseña"), y un pie con contacto de soporte.

---

## 3. Reorganización de pantallas existentes (sin cambiar su función)

- **Panel principal / Dashboard** (home tras iniciar sesión): la Propuesta
  define sólo las 3 tarjetas de estadística (Alumnos en Condición Libre,
  Alertas Activas, Solicitudes Pendientes) y el listado de comunicados. Se
  agregaron **dos secciones nuevas arriba**: "Lo que hay que resolver hoy"
  (una lista de pendientes accionables con botón directo, construida con los
  mismos datos que ya calculaba el controller) y "¿Qué querés hacer?" (una
  grilla de 6 accesos directos a las tareas más frecuentes: tomar asistencia,
  cargar notas, buscar un alumno, escribir un comunicado, registrar una
  observación, generar un reporte). Ningún dato nuevo: son links a rutas que
  ya existían.
- **Legajo del alumno** (sección 5.1.4): el encabezado pasó a tarjeta con
  avatar de 96px; las pestañas (12px, subrayado) pasaron a botones de 48px
  dentro de un contenedor blanco; las acciones destructivas (Dar de Baja,
  Registrar Pase) se separaron en su propia tarjeta con borde rojo suave y
  texto explícito de qué se pierde y qué se conserva, en vez de convivir con
  campos de edición normales; se agregó una explicación en texto de por qué
  un alumno está en Condición Libre (cuántas inasistencias y desde cuándo),
  en vez de mostrar sólo el chip naranja sin contexto.
- **Asistencia — Preceptor** (sección 5.2): el cambio de más impacto de todo
  el rediseño. Estaba resuelto con un `<select>` de 6 opciones por alumno
  (95% de los casos es "Presente"); se reemplazó por **botones de estado**
  (P / A / AJ / T + "Otra situación..." para los dos casos raros) más un
  botón "Marcar a todos: Todos presentes" y un contador en vivo
  ("18 presentes · 2 ausentes · 1 tarde"). El campo que llega al servidor es
  exactamente el mismo (`items[i].tipo`); no se tocó el controller.
- **Configuración**: la Propuesta lista 8 pantallas de configuración sueltas
  (Parámetros Académicos, Materias, Workshops, Aulas, Ciclos Lectivos,
  Estructura del Día, SMTP, Plantillas de Email) accesibles sólo desde el
  menú lateral, sin una vista que las agrupe. Se agregó una **landing en
  `/configuracion`** con una tarjeta por pantalla, mostrando el dato vigente
  de cada una (ej. "Tope de faltas: 28 · Nota mínima: 7.0"). Es de sólo
  lectura sobre datos que ya existían — no agrega tablas ni lógica de
  negocio nueva.
- **Portal de Padres** (sección 5.11): comunicados con estilos en línea
  (`style="border-radius:6px;..."`) se pasaron al componente `.item-lista`
  ya usado en el resto del sistema; el botón "Ver detalle" pasa a ocupar el
  ancho completo de la tarjeta (52px de alto, más fácil de tocar).

---

## 4. Traducción completa de la aplicación (i18n)

La Propuesta no especifica si el sistema debe soportar más de un idioma.
Se implementó **español/inglés completo**: las 70 pantallas del sistema,
incluidos textos de validación, confirmaciones, mensajes de error y PDFs
generados, están cubiertas por un total de más de 680 claves en
`messages.properties` / `messages_en.properties`, con un selector de idioma
en la topbar. Se armó además un chequeo automático (`comm` entre las claves
usadas en las plantillas y las definidas en ambos archivos) para garantizar
que ninguna pantalla quede a mitad traducir.

---

## 5. Buscador global (funcionalidad nueva, no contemplada)

No existe en la Propuesta. Se agregó un buscador tipo *command palette*
(`Ctrl+K` o botón en la topbar) que busca en simultáneo alumnos, usuarios y
cursos/divisiones, con hasta 5+5+3 resultados, navegable con flechas y Enter,
y con una sección de "Acciones rápidas" (tomar asistencia, cargar notas,
buscar alumno) cuando todavía no se escribió nada.

**Punto de atención para la Propuesta / la defensa:** cada sección de
resultados se filtra por rol **en el propio endpoint** (`GET /buscar`), no
sólo ocultando en la vista — un Docente nunca recibe resultados de la
sección "Usuarios" aunque manipule la petición directamente, porque el
`BuscadorController` chequea `hasAnyRole(...)` contra cada tipo de dato antes
de ejecutar la búsqueda correspondiente.

---

## 6. Sistema de "Deshacer" (funcionalidad nueva, no contemplada)

Tampoco existe en la Propuesta. Las acciones destructivas usaban un modal de
confirmación genérico ("¿Confirmar acción?" / "Confirmar"). Se cambió el
patrón a **consecuencia + sujeto + qué se conserva** (ej. "¿Dar de baja a
Acosta, Gonzalo? Deja de figurar en las listas de su curso y no se le pueden
cargar notas ni asistencia. El legajo se conserva.", con botón "Sí, dar de
baja"), y se agregó una ventana de **10 segundos para deshacer** la acción
desde el mismo toast de confirmación, antes de que se pierda la posibilidad.

Implementado en:
- Dar de baja a un alumno
- Registrar el pase de un alumno
- Desactivar un usuario

**Deliberadamente no implementado** en "eliminar observación disciplinaria"
ni en "eliminar evaluación": deshacerlas bien implica reconstruir filas hijas
(notas de una evaluación, o el conteo de observaciones que dispara una firma
disciplinaria automática) sin duplicar un efecto secundario ya disparado
(por ejemplo, una firma disciplinaria ya notificada por email a la familia).
Se dejó documentado como alcance pendiente en vez de resuelto a las apuradas.

Detalle técnico relevante para la sección de arquitectura: el estado
pendiente de deshacer se guarda en memoria del proceso (un
`ConcurrentHashMap` en `DeshacerService`, no en sesión ni en base de datos),
con una limpieza programada (`@Scheduled`, cada 60 segundos) que descarta
cualquier entrada de más de 60 segundos, y una validación de que el usuario
que pide deshacer sea el mismo que hizo la acción original.

---

## 7. Educación Física: funcionalidades agregadas sobre el módulo original

La sección 5.7 de la Propuesta cubre la creación de grupos y la carga de
pruebas físicas, pero no contempla:

- **Editar un grupo ya creado** (cambiar el docente, las pruebas asignadas o
  el listado de alumnos por cuatrimestre) — originalmente sólo se podía
  crear.
- **Eliminar una prueba física ya cargada**, para el caso de un error de
  carga o una duplicación — antes no había forma de deshacer una carga
  incorrecta salvo entrar directo a la base de datos.
- **Catálogo de pruebas físicas con fusión de duplicados**: con el uso real
  surgió que la misma actividad física terminaba cargada dos veces bajo
  nombres distintos (ej. "50 Metros" y "Carrera 50 metros"), porque el
  catálogo sólo evitaba duplicados exactos (mismo nombre, sin distinguir
  mayúsculas). Se agregó una pantalla (`/educacion-fisica/catalogo-pruebas`)
  donde un Superadmin/Directivo/Administrativo puede fusionar dos
  definiciones en una sola, reasignando automáticamente todo el historial de
  pruebas y grupos de la definición descartada a la que se conserva — sin
  perder continuidad para los gráficos de evolución del alumno.

---

## 8. Cambios de arquitectura y modelo de datos

### 8.1 — Cuatrimestre "en curso" calculado por fecha (no elegido a mano)

La Propuesta no define cómo se determina el cuatrimestre por defecto al
cargar una nota o una prueba física. La implementación original obligaba a
elegirlo manualmente cada vez (con el defecto siempre fijo en "1°", incluso
en pleno segundo cuatrimestre). Se agregó a la entidad `CicloLectivo` un
método derivado, **no persistido**, que compara la fecha de hoy contra
`fecha_fin_c1` del ciclo lectivo activo para decidir si corresponde 1° o 2°:

```java
public int getCuatrimestreEnCurso() {
    return LocalDate.now().isAfter(fechaFinC1) ? 2 : 1;
}
```

Se usa como valor por defecto en Calificaciones, Educación Física (grupos y
pruebas) y en el selector de Boletín — pero sigue siendo **editable a mano**
en todos los casos, para el caso de una corrección o un cierre de
cuatrimestre atrasado.

Nota para la Propuesta: existe un campo `cuatrimestre_activo` que un
Superadmin cambia manualmente desde "Ciclos Lectivos", pero se comprobó que
**no bloquea ni habilita nada** en el sistema (la única validación real es si
el cuatrimestre está *cerrado*, no cuál está "activo"), así que depender de
él como valor por defecto habría reproducido el mismo problema que se quería
resolver (alguien tiene que acordarse de tocarlo a mano). Quedó sin usar para
este propósito; se sugiere evaluar en la Propuesta si conviene eliminarlo o
si tiene otro uso previsto que no está implementado.

### 8.2 — Catálogo de pruebas físicas reemplaza texto libre + enum fijo

Cambio de modelo de datos, migración `V21__prueba_fisica_definicion.sql`.
La sección 5.7.4 de la Propuesta describe la prueba física con un campo de
texto libre (`tipo_prueba`) y una métrica de un enum cerrado de 4 valores
(`TIEMPO_SEGUNDOS`, `DISTANCIA_CM`, `DISTANCIA_METROS`, `REPETICIONES`). Este
diseño no permite comparar el progreso de un alumno entre pruebas si dos
docentes escriben el nombre de la prueba de forma distinta, y el enum fijo no
alcanza para todas las unidades reales (ej. flexiones por minuto).

Se reemplazó por una tabla catálogo `prueba_fisica_definicion` (nombre único,
unidad como texto libre, dirección de mejora `mayor_es_mejor` como booleano)
y una relación muchos-a-muchos `grupo_ef_prueba_definicion` que define qué
pruebas del catálogo puede usar cada grupo. Los datos existentes se migraron
automáticamente combinando `tipo_prueba` + `metrica`, deduplicando por
nombre. Esto es lo que permite, entre otras cosas, el punto 7 (fusión de
duplicados) y el gráfico de evolución del alumno a través de años y grupos
distintos.

### 8.3 — `/fonts/**` en la lista de rutas públicas de Spring Security

Cambio menor pero real: al mover la tipografía a un archivo local
(`static/fonts/SourceSans3-Variable.woff2`), la ruta no estaba en la lista de
rutas públicas de `SecurityConfig` (que sólo incluía `/css/**`, `/js/**`,
`/img/**`). Esto rompía la tipografía específicamente en la pantalla de
login (la única pantalla pública, sin sesión iniciada). Se agregó `/fonts/**`
a `RUTAS_PUBLICAS`.

### 8.4 — Diseño responsive para celular y tablet

La sección 4 de la Propuesta definió un rediseño "sólo escritorio". Se adaptó
todo el sistema a celular (hasta 640px) y tablet (hasta 1023px), con
prioridad en las pantallas de docentes, preceptores y padres; el escritorio
no cambia. El detalle de cómo está hecho y cómo mantenerlo está en
**ADR-012** (`docs/DECISIONS.md`).

---

## 9. Correcciones sobre supuestos incorrectos (no bugs de programación, sino de especificación)

Estos son casos donde el sistema **funcionaba tal como estaba escrito el
código**, pero el comportamiento no era el esperado por un supuesto de la
Propuesta o de una decisión de diseño anterior:

- **Textos de ayuda con referencias internas filtradas**: 11 textos visibles
  para el usuario final (ej. "Sección 5.14.2. Valores por defecto...")
  incluían el número de sección de la Propuesta como si fuera información
  útil para un Directivo o Preceptor. Se quitaron todas esas referencias
  internas del texto visible (quedan intactas en comentarios de código y de
  migraciones SQL, que sí son para desarrolladores).
- **`configuracion_smtp` y `plantilla_email` definidas dos veces**: el
  historial de migraciones (V3 y V16) crea estas dos tablas con columnas
  incompatibles, sin un `DROP TABLE` entre medio — un script que reproduzca
  las migraciones en orden fallaría al llegar a V16. El sistema en uso
  funciona porque la base real nunca se recreó desde cero con V3 activa, pero
  quedó documentado (y corregido en el script de creación de base
  consolidado) para que no sea una sorpresa si alguna vez se necesita
  levantar la base desde cero.

---

## 10. Explícitamente fuera de alcance (decisiones de no implementar, y por qué)

Para que quede como decisión registrada y no como omisión:

| Ítem | Por qué no se implementó |
|---|---|
| Casilla "Mantener sesión iniciada" en el login | Requiere habilitar `rememberMe()` de Spring Security, que extiende la sesión con una cookie persistente — contradice la decisión ya tomada y documentada en **ADR-001** (`docs/DECISIONS.md`) de usar sesiones de servidor, no autenticación persistente tipo token |
| Contador de intentos de login restantes antes del bloqueo | Requiere un `AuthenticationFailureHandler` a medida que consulte cuántos intentos lleva un email antes de fallar login — abre una superficie de enumeración de cuentas (se puede usar para adivinar qué emails existen) que hay que diseñar con cuidado, no como ajuste visual |
| Columna Año/División en el listado de Alumnos | El listado no trae hoy esa información por alumno sin agregar una consulta por fila (N+1) o una nueva agregación en el controller — implica tocar lógica de negocio, no sólo la plantilla |
| 3 números grandes (Promedio / Faltas / Avisos) por hijo en el Portal de Padres | Ningún lugar del sistema calcula hoy un "promedio general" de un alumno (sólo promedios por materia) ni define qué cuenta como "aviso" por alumno — es una funcionalidad nueva a especificar, no un ajuste visual |
| Deshacer para "eliminar observación" y "eliminar evaluación" | Ver punto 6 |

---

## 11. Documentación complementaria generada (no cambia el sistema, pero acompaña esta entrega)

Por fuera del código, se generó documentación que puede anexarse a la
Propuesta o usarse en la defensa:

- Diagramas UML (casos de uso, actividad, secuencia y clases) de los
  procesos core del sistema.
- Diagrama entidad-relación completo (panorama general + un diagrama por
  módulo), en PDF de una página por módulo.
- Diccionario de datos de las 53 tablas de la base.
- Script SQL consolidado de creación de la base desde cero, probado
  ejecutándolo de punta a punta contra una base vacía.

---

## Cómo usar este documento

Cada sección de arriba está pensada para copiarse o resumirse directamente
en el capítulo correspondiente de la Propuesta Técnica (sección 4 para todo
lo visual, sección 5.x para lo funcional de cada módulo, sección 7 para lo
de seguridad). Los puntos 5, 6 y 8.2 son los que más justifican un párrafo
propio nuevo en la Propuesta, porque son funcionalidad y modelo de datos que
no estaban previstos en absoluto, no un ajuste de algo ya descripto.
