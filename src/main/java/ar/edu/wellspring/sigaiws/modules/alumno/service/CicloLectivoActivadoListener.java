package ar.edu.wellspring.sigaiws.modules.alumno.service;

import ar.edu.wellspring.sigaiws.modules.calificacion.service.MesaExamenService;
import ar.edu.wellspring.sigaiws.modules.configuracion.event.CicloLectivoActivadoEvent;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacciona a la activacion de un nuevo ciclo lectivo (seccion 5.14.2). Con el
 * modelo de datos elegido (un AlumnoCicloLectivo por alumno y ciclo, con
 * contadores en cero por defecto - ver AlumnoCicloLectivo), el "reinicio de
 * contadores" queda satisfecho automaticamente en cuanto se crea el registro
 * del nuevo año: no hay nada que resetear en los registros existentes.
 *
 * La "solicitud de confirmacion de continuidad de alumnos (cuales repiten, cuales
 * pasan de año)" la resuelve el asistente de promocion (PromocionService): al
 * activar un ciclo, CicloLectivoController redirige a esa pantalla. Antes, se
 * asegura que las materias adeudadas del ciclo que termina esten generadas (por
 * si no se cerro el 2° cuatrimestre), porque la promocion se decide con ellas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CicloLectivoActivadoListener {

    private final CicloLectivoRepository cicloLectivoRepository;
    private final MesaExamenService mesaExamenService;

    @EventListener
    public void onCicloLectivoActivado(CicloLectivoActivadoEvent event) {
        log.info("Ciclo lectivo activado (anterior={}, nuevo={}). La inscripcion de los alumnos en el ciclo nuevo "
                        + "se hace desde el asistente de promocion (PromocionService).",
                event.cicloLectivoAnteriorId(), event.cicloLectivoNuevoId());
        if (event.cicloLectivoAnteriorId() != null) {
            cicloLectivoRepository.findById(event.cicloLectivoAnteriorId()).ifPresent(anterior -> {
                var r = mesaExamenService.sincronizarCiclo(anterior);
                log.info("Materias adeudadas del ciclo {}: {} nuevas, {} actualizadas, {} quitadas.",
                        anterior.getAnio(), r.nuevas(), r.actualizadas(), r.quitadas());
            });
        }
    }
}
