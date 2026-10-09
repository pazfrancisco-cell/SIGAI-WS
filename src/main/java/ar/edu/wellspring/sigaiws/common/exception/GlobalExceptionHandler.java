package ar.edu.wellspring.sigaiws.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Manejo centralizado de errores. SIGAI-WS es, en su mayoria, un monolito
 * MVC que responde HTML (seccion 3.1), pero algunas pantallas usan llamadas
 * AJAX puntuales (autosave de notas 5.3.4, toasts). Por eso este handler
 * responde distinto segun el tipo de request:
 *  - Peticiones AJAX/JSON: ProblemDetail (RFC 7807) para que el JS muestre un toast.
 *  - Peticiones de navegacion normal: redirect al origen con mensaje flash,
 *    o una vista de error dedicada (403/404/500) cuando no hay a donde volver.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public Object handleNotFound(NotFoundException ex, HttpServletRequest request) {
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(problem(HttpStatus.NOT_FOUND, ex.getMessage(), request));
        }
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        mav.addObject("mensaje", ex.getMessage());
        return mav;
    }

    @ExceptionHandler({BusinessException.class, DuplicateResourceException.class})
    public Object handleBusiness(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (isAjax(request)) {
            return ResponseEntity.unprocessableEntity()
                    .body(problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request));
        }
        String referer = request.getHeader(HttpHeaders.REFERER);
        redirectAttributes.addFlashAttribute("errorMensaje", ex.getMessage());
        if (referer != null) {
            return "redirect:" + referer;
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("mensaje", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ModelAndView handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        ModelAndView mav = new ModelAndView("error/403");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        if (isAjax(request)) {
            return ResponseEntity.internalServerError()
                    .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado.", request));
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return "XMLHttpRequest".equals(requestedWith) || (accept != null && accept.contains("application/json"));
    }

    private ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }
}
