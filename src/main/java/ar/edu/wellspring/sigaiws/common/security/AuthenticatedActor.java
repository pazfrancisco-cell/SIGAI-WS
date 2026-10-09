package ar.edu.wellspring.sigaiws.common.security;

/**
 * Contrato minimo que debe cumplir el principal autenticado (implementado por
 * {@code SigaiUserPrincipal} en el modulo auth) para que capas de bajo nivel
 * como {@code common.audit.AuditService} puedan identificar "quien" ejecuta
 * una accion sin depender del modulo de usuarios/autenticacion (evita
 * dependencias circulares entre capas).
 */
public interface AuthenticatedActor {
    Long getId();
    String getUsername();
}
