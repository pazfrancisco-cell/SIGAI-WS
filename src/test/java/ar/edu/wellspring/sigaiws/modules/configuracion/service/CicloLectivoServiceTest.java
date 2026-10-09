package ar.edu.wellspring.sigaiws.modules.configuracion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.wellspring.sigaiws.common.audit.AuditService;
import ar.edu.wellspring.sigaiws.modules.configuracion.domain.CicloLectivo;
import ar.edu.wellspring.sigaiws.modules.configuracion.event.CicloLectivoActivadoEvent;
import ar.edu.wellspring.sigaiws.modules.configuracion.repository.CicloLectivoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Seccion 5.14.2: "activar el ciclo lectivo actual (solo puede haber uno activo)".
 * El indice unico filtrado de la base de datos (ux_ciclo_lectivo_activo) es la
 * garantia definitiva; este test cubre la logica de orquestacion del service
 * (archivar el anterior, publicar el evento de reinicio de contadores).
 */
@ExtendWith(MockitoExtension.class)
class CicloLectivoServiceTest {

    @Mock private CicloLectivoRepository cicloLectivoRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CicloLectivoService service;

    @BeforeEach
    void setUp() {
        service = new CicloLectivoService(cicloLectivoRepository, auditService, eventPublisher);
    }

    @Test
    void alActivarUnCicloSeArchivaElAnteriorYSePublicaElEvento() {
        CicloLectivo anterior = cicloLectivo(1L, 2025, true);
        CicloLectivo nuevo = cicloLectivo(2L, 2026, false);

        when(cicloLectivoRepository.findById(2L)).thenReturn(Optional.of(nuevo));
        when(cicloLectivoRepository.findByActivoTrue()).thenReturn(Optional.of(anterior));
        when(cicloLectivoRepository.save(any(CicloLectivo.class))).thenAnswer(inv -> inv.getArgument(0));

        service.activar(2L);

        assertThat(anterior.isActivo()).isFalse();
        assertThat(anterior.getEstado()).isEqualTo(CicloLectivo.Estado.ARCHIVADO);
        assertThat(nuevo.isActivo()).isTrue();
        assertThat(nuevo.getEstado()).isEqualTo(CicloLectivo.Estado.ACTIVO);
        assertThat(nuevo.getCuatrimestreActivo()).isEqualTo(1);

        ArgumentCaptor<CicloLectivoActivadoEvent> captor = ArgumentCaptor.forClass(CicloLectivoActivadoEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().cicloLectivoAnteriorId()).isEqualTo(1L);
        assertThat(captor.getValue().cicloLectivoNuevoId()).isEqualTo(2L);
    }

    @Test
    void activarElCicloYaActivoNoHaceNada() {
        CicloLectivo yaActivo = cicloLectivo(1L, 2026, true);
        when(cicloLectivoRepository.findById(1L)).thenReturn(Optional.of(yaActivo));

        service.activar(1L);

        verify(eventPublisher, times(0)).publishEvent(any());
    }

    private CicloLectivo cicloLectivo(Long id, int anio, boolean activo) {
        CicloLectivo ciclo = new CicloLectivo();
        ciclo.setId(id);
        ciclo.setAnio(anio);
        ciclo.setActivo(activo);
        ciclo.setEstado(activo ? CicloLectivo.Estado.ACTIVO : CicloLectivo.Estado.PLANIFICADO);
        return ciclo;
    }
}
