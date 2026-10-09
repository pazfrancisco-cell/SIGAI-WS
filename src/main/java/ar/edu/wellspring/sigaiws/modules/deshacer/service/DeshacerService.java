package ar.edu.wellspring.sigaiws.modules.deshacer.service;

import ar.edu.wellspring.sigaiws.modules.alumno.service.AlumnoService;
import ar.edu.wellspring.sigaiws.modules.usuario.service.UsuarioService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Deshacer de 10 segundos para acciones destructivas (IMPLEMENTACION.md 4.2).
 * Guarda en memoria, por 60 segundos como maximo, el suficiente contexto para
 * revertir la accion llamando al mismo metodo de servicio que ya existe para
 * el caso contrario (ej. reactivar/activar). No persiste nada en base de
 * datos: si el proceso se reinicia, las entradas pendientes se pierden (igual
 * que perder la ventana de 10 segundos del toast).
 *
 * Alcance actual: baja de alumno, pase de alumno y desactivar usuario. Quedan
 * afuera "eliminar observacion" y "eliminar evaluacion" porque revertirlas
 * bien implica recrear filas hijas (notas, firmas) y recalcular contadores
 * acumulados sin duplicar efectos secundarios (como una firma disciplinaria)
 * - un alcance mayor al de esta pasada, documentado a proposito en vez de
 * resuelto a las apuradas.
 */
@Service
@RequiredArgsConstructor
public class DeshacerService {

    public enum Tipo { BAJA_O_PASE_ALUMNO, DESACTIVAR_USUARIO }

    private record Entrada(Tipo tipo, Long entidadId, Long usuarioId, Instant creadoEn) {}

    private static final long VENTANA_SEGUNDOS = 60;

    private final Map<String, Entrada> pendientes = new ConcurrentHashMap<>();
    private final AlumnoService alumnoService;
    private final UsuarioService usuarioService;

    public String registrar(Tipo tipo, Long entidadId, Long usuarioId) {
        String uuid = UUID.randomUUID().toString();
        pendientes.put(uuid, new Entrada(tipo, entidadId, usuarioId, Instant.now()));
        return uuid;
    }

    /** @return true si se deshizo; false si el uuid no existe, ya expiro, o no pertenece al usuario. */
    public boolean deshacer(String uuid, Long usuarioId) {
        Entrada entrada = pendientes.remove(uuid);
        if (entrada == null || !entrada.usuarioId().equals(usuarioId)) {
            return false;
        }
        switch (entrada.tipo()) {
            case BAJA_O_PASE_ALUMNO -> alumnoService.reactivar(entrada.entidadId());
            case DESACTIVAR_USUARIO -> usuarioService.activar(entrada.entidadId());
        }
        return true;
    }

    @Scheduled(fixedRate = 60000)
    void limpiarVencidas() {
        Instant limite = Instant.now().minusSeconds(VENTANA_SEGUNDOS);
        pendientes.values().removeIf(entrada -> entrada.creadoEn().isBefore(limite));
    }
}
