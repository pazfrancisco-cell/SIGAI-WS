package ar.edu.wellspring.sigaiws.modules.usuario.repository;

import ar.edu.wellspring.sigaiws.common.enums.RolNombre;
import ar.edu.wellspring.sigaiws.modules.usuario.domain.Usuario;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select distinct u from Usuario u
            left join u.roles r
            where (:texto is null or lower(u.nombreCompleto) like lower(concat('%', :texto, '%'))
                                    or lower(u.email) like lower(concat('%', :texto, '%')))
              and (:soloActivos = false or u.activo = true)
              and (:rol is null or r.nombre = :rol)
            order by u.nombreCompleto
            """)
    Page<Usuario> buscar(@Param("texto") String texto, @Param("soloActivos") boolean soloActivos,
                          @Param("rol") RolNombre rol, Pageable pageable);

    long countByRoles_Nombre(RolNombre rolNombre);
}
