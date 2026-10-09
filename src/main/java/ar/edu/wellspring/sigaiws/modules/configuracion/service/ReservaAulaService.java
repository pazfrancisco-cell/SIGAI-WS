package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.common.exception.BusinessException;
import ar.edu.wellspring.sigaiws.common.exception.NotFoundException;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.ReservaAula;
import ar.edu.wellspring.sigaiws.modules.configuracion.dto.ReservaAulaForm;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.AulaRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CursoDivisionRepository;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.ReservaAulaRepository;
import ar.edu.wellspring.sigaiws.modules.horario.repository.HorarioSlotRepository;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seccion 5.5.1: reserva puntual de un aula por un docente, para un curso/division y fecha concretos. */
@Service
@RequiredArgsConstructor
public class ReservaAulaService {

    private static final String MODULO = "CONFIGURACION";

    private final ReservaAulaRepository reservaAulaRepository;
    private final AulaRepository aulaRepository;
    private final CursoDivisionRepository cursoDivisionRepository;
    private final CicloLectivoRepository cicloLectivoRepository;
    private final HorarioSlotRepository horarioSlotRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ReservaAula> misReservas(Long docenteId) {
        return reservaAulaRepository.findAllByDocente_IdOrderByFechaDesc(docenteId);
    }

    @Transactional
    public ReservaAula reservar(ReservaAulaForm form, Usuario docente) {
        if (form.getFecha().isBefore(LocalDate.now())) {
            throw new BusinessException("No se puede reservar un aula para una fecha pasada.");
        }

        reservaAulaRepository.findByAula_IdAndFechaAndPeriodoOrden(form.getAulaId(), form.getFecha(), form.getPeriodoOrden())
                .ifPresent(r -> {
                    throw new BusinessException("El aula ya esta reservada ese dia y periodo por "
                            + r.getDocente().getNombreCompleto() + ".");
                });

        int diaSemana = form.getFecha().getDayOfWeek().getValue();
        if (diaSemana <= 5) {
            cicloLectivoRepository.findByActivoTrue().ifPresent(ciclo -> {
                if (horarioSlotRepository.existsByAula_IdAndCicloLectivo_IdAndDiaSemanaAndPeriodoOrden(
                        form.getAulaId(), ciclo.getId(), diaSemana, form.getPeriodoOrden())) {
                    throw new BusinessException("El aula ya tiene una clase regular asignada ese dia y periodo.");
                }
            });
        }

        ReservaAula reserva = new ReservaAula();
        reserva.setAula(aulaRepository.findById(form.getAulaId())
                .orElseThrow(() -> NotFoundException.of("Aula", form.getAulaId())));
        reserva.setDocente(docente);
        reserva.setCursoDivision(cursoDivisionRepository.findById(form.getCursoDivisionId())
                .orElseThrow(() -> NotFoundException.of("CursoDivision", form.getCursoDivisionId())));
        reserva.setFecha(form.getFecha());
        reserva.setPeriodoOrden(form.getPeriodoOrden());
        reserva.setMotivo(form.getMotivo());
        reserva = reservaAulaRepository.save(reserva);

        auditService.registrar(MODULO, "RESERVAR_AULA", "ReservaAula", reserva.getId(), null,
                "aula=" + reserva.getAula().getNombre() + ", fecha=" + reserva.getFecha() + ", periodo=" + reserva.getPeriodoOrden());
        return reserva;
    }

    @Transactional
    public void cancelar(Long id, Usuario solicitante) {
        ReservaAula reserva = reservaAulaRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("ReservaAula", id));

        boolean esPropia = reserva.getDocente().getId().equals(solicitante.getId());
        boolean esAdmin = solicitante.tieneRol(RolNombre.SUPERADMIN) || solicitante.tieneRol(RolNombre.DIRECTIVO)
                || solicitante.tieneRol(RolNombre.ADMINISTRATIVO);
        if (!esPropia && !esAdmin) {
            throw new BusinessException("Solo podes cancelar tus propias reservas.");
        }

        reservaAulaRepository.delete(reserva);
        auditService.registrar(MODULO, "CANCELAR_RESERVA_AULA", "ReservaAula", id, reserva.getAula().getNombre(), null);
    }
}
