package ar.edu.wellspring.sigaiws.modules.configuracion.event;

/**
 * Publicado cuando se activa un nuevo ciclo lectivo (seccion 5.14.2). El modulo
 * Alumnos escucha este evento para crear los registros anuales de trayectoria
 * (alumno_ciclo_lectivo) con contadores de asistencia/disciplina en cero.
 * Se modela como evento de dominio (en vez de una llamada directa
 * configuracion -> alumno) para mantener la direccion de dependencias: los
 * modulos academicos dependen de Configuracion, nunca al reves.
 */
public record CicloLectivoActivadoEvent(Long cicloLectivoAnteriorId, Long cicloLectivoNuevoId) {
}
