package ar.edu.wellspring.sigaiws.modules.disciplina.dto;

import ar.edu.wellspring.sigaiws.modules.disciplina.domain.ActaDisciplinaria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActaForm {
    @NotNull
    private Long alumnoId;
    @NotNull
    private ActaDisciplinaria.Tipo tipo;
    @NotBlank(message = "Describa la situacion y las acciones acordadas")
    private String contenido;
}
