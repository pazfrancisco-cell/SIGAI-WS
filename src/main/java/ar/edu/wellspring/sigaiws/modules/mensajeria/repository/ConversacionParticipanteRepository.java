package ar.edu.wellspring.sigaiws.modules.mensajeria.repository;

import ar.edu.wellspring.sigaiws.modules.mensajeria.domain.ConversacionParticipante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversacionParticipanteRepository extends JpaRepository<ConversacionParticipante, Long> {
    List<ConversacionParticipante> findAllByUsuario_Id(Long usuarioId);
    List<ConversacionParticipante> findAllByConversacion_Id(Long conversacionId);
}
