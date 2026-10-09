package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.alerta.domain.Alerta;
import ar.edu.wellspring.sigaiws.modules.alerta.service.AlertaService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import ar.edu.wellspring.sigaiws.modules.mensajeria.service.NotificacionService;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import ar.edu.wellspring.sigaiws.modules.usuario.repository.UsuarioRepository;
import ar.edu.wellspring.sigaiws.web.DashboardService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seccion 5.13.2 "Cierre de cuatrimestre proximo". Chequeo diario: a 7 dias
 * del cierre avisa a gestion (alerta) y a cada docente (notificacion, con sus
 * evaluaciones que todavia tienen notas sin cargar); a 1 dia, un ultimo
 * recordatorio a los docentes. Cada aviso se genera una sola vez.
 */
@Service
@RequiredArgsConstructor
public class AvisoCierreCuatrimestreService {

    static final String TIPO_ALERTA = "Cierre de cuatrimestre proximo";
    private static final String NOTIF_PRIMER_AVISO = "CIERRE_CUATRIMESTRE_PROXIMO";
    private static final String NOTIF_ULTIMO_AVISO = "CIERRE_CUATRIMESTRE_ULTIMO_AVISO";
    private static final int DIAS_PRIMER_AVISO = 7;
    private static final int DIAS_ULTIMO_AVISO = 1;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CicloLectivoRepository cicloLectivoRepository;
    private final AlertaService alertaService;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;
    private final DashboardService dashboardService;

    public record Resultado(boolean enVentana, long diasRestantes, int cuatrimestre, boolean alertaCreada, int docentesAvisados) {}

    @Scheduled(cron = "0 5 7 * * *")
    @Transactional
    public Resultado verificar() {
        CicloLectivo ciclo = cicloLectivoRepository.findByActivoTrue().orElse(null);
        if (ciclo == null) {
            return new Resultado(false, -1, 0, false, 0);
        }
        int cuatrimestre = ciclo.getCuatrimestreEnCurso();
        boolean cerrado = cuatrimestre == 1 ? ciclo.isCuatrimestre1Cerrado() : ciclo.isCuatrimestre2Cerrado();
        LocalDate cierre = cuatrimestre == 1 ? ciclo.getFechaFinC1() : ciclo.getFechaFinC2();
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), cierre);
        if (cerrado || dias < 0 || dias > DIAS_PRIMER_AVISO) {
            return new Resultado(false, dias, cuatrimestre, false, 0);
        }

        // Clave de idempotencia: el cuatrimestre de este ciclo lectivo.
        String entidad = "CUATRIMESTRE_" + cuatrimestre;
        String cuando = cuandoCierra(dias);

        boolean alertaCreada = false;
        if (!alertaService.yaGenerada(TIPO_ALERTA, entidad, ciclo.getId())) {
            alertaService.crear(TIPO_ALERTA, Alerta.Prioridad.ALTA, null,
                    "El cuatrimestre %d cierra el %s (%s). Despues de esa fecha los docentes no podran cargar ni editar notas."
                            .formatted(cuatrimestre, cierre.format(FECHA), cuando),
                    entidad, ciclo.getId());
            alertaCreada = true;
        }

        String tipoAviso = dias <= DIAS_ULTIMO_AVISO ? NOTIF_ULTIMO_AVISO : NOTIF_PRIMER_AVISO;
        int avisados = 0;
        for (Usuario docente : usuarioRepository.buscar(null, true, null, PageRequest.of(0, 1000)).getContent()) {
            if (!docente.tieneRol(RolNombre.DOCENTE)
                    || notificacionService.yaNotificado(docente.getId(), tipoAviso, entidad, ciclo.getId())) {
                continue;
            }
            var pendientes = dashboardService.evaluacionesSinCerrar(docente.getId(), ciclo);
            String detalle = pendientes.isEmpty()
                    ? "Tenes todas tus evaluaciones del cuatrimestre con notas completas."
                    : "Evaluaciones con notas sin cargar: " + pendientes.stream()
                            .map(e -> "%s - %s (%d/%d)".formatted(e.evaluacion().getMateria().getNombre(),
                                    e.evaluacion().getTitulo(), e.notasCargadas(), e.totalAlumnos()))
                            .collect(Collectors.joining("; ")) + ".";
            notificacionService.notificar(docente.getId(), tipoAviso,
                    "El cuatrimestre %d cierra %s".formatted(cuatrimestre, cuando),
                    "El %s cierra el cuatrimestre %d; despues no vas a poder cargar ni editar notas. %s"
                            .formatted(cierre.format(FECHA), cuatrimestre, detalle),
                    entidad, ciclo.getId(), "/calificaciones/cargar");
            avisados++;
        }
        return new Resultado(true, dias, cuatrimestre, alertaCreada, avisados);
    }

    private static String cuandoCierra(long dias) {
        if (dias == 0) {
            return "hoy";
        }
        return dias == 1 ? "mañana" : "en " + dias + " días";
    }
}
