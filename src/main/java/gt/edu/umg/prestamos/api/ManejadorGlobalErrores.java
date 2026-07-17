package gt.edu.umg.prestamos.api;

import gt.edu.umg.prestamos.api.dto.ErrorDTO;
import gt.edu.umg.prestamos.dominio.estado.TransicionInvalidaException;
import gt.edu.umg.prestamos.servicio.RecursoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Traducción centralizada de excepciones a respuestas HTTP uniformes:
 * 404 recurso inexistente, 400 datos inválidos, 409 transición de estado ilegal.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ErrorDTO.de(404, "Not Found", ex.getMessage(), List.of());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDTO manejarTransicionInvalida(TransicionInvalidaException ex) {
        return ErrorDTO.de(409, "Conflict", ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ErrorDTO.de(400, "Bad Request", "Datos de la solicitud inválidos", detalles);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO manejarArgumentoInvalido(Exception ex) {
        return ErrorDTO.de(400, "Bad Request", ex.getMessage(), List.of());
    }
}
