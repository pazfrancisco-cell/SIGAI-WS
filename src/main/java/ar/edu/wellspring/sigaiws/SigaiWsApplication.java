package ar.edu.wellspring.sigaiws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de SIGAI-WS (Sistema Integral de Gestion Academica Institucional).
 * Ver docs/ARCHITECTURE.md para el detalle de capas y docs/DECISIONS.md para las
 * decisiones tecnicas que resuelven ambiguedades de la Propuesta Tecnica original.
 */
@SpringBootApplication
@EnableScheduling // usado por el motor de alertas y recordatorios (modulo 5.13 / 5.14)
@EnableAsync // usado por EmailService para no bloquear el hilo de request
public class SigaiWsApplication {

    public static void main(String[] args) {
        // Si el IDE reporta "Could not initialize class lombok.javac.Javac",
        // verificar que Lombok y el JDK configurado en el entorno sean compatibles.
        SpringApplication.run(SigaiWsApplication.class, args);
    }
}
