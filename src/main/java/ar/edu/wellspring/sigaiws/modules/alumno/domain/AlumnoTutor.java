package ar.edu.wellspring.sigaiws.modules.alumno.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vinculo alumno-tutor (maximo 2 tutores por alumno, validado en AlumnoService). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alumno_tutor")
public class AlumnoTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Enumerated(EnumType.STRING)
    @Column(name = "vinculo", nullable = false, length = 20)
    private VinculoTutor vinculo;

    @Column(name = "recibe_notificaciones", nullable = false)
    private boolean recibeNotificaciones = true;
}
