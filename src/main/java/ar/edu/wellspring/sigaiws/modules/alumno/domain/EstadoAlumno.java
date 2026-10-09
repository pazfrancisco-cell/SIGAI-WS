package ar.edu.wellspring.sigaiws.modules.alumno.domain;

/**
 * Estado global del alumno en la institucion (seccion 5.1). La "Condicion Libre"
 * NO es un estado global: es un atributo del ciclo lectivo en curso (ver
 * AlumnoCicloLectivo.condicionLibre), porque un alumno puede estar en condicion
 * libre un año y regularizar al siguiente sin dejar de estar "Activo" como alumno.
 */
public enum EstadoAlumno {
    ACTIVO,
    INACTIVO_PASE,
    INACTIVO_BAJA,
    EGRESADO
}
