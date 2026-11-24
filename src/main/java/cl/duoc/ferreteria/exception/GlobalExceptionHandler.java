package cl.duoc.ferreteria.exception;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(RecursoNoEncontradoException.class)
  ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(Map.of("error", e.getMessage()));
  }

  @ExceptionHandler(ConflictoException.class)
  ResponseEntity<Map<String, String>> conflicto(ConflictoException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
      .body(Map.of("error", e.getMessage()));
  }

  // Guía 18: la base rechazó el dato por la restricción UNIQUE (código de producto o nombre de
  // categoría). Normalmente lo detecta antes el servicio; esto cubre dos solicitudes simultáneas
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Map<String, String>> datoRepetido(DataIntegrityViolationException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
      .body(Map.of("error", "El dato ya existe o está en uso por otro registro"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, String>> datosInvalidos(MethodArgumentNotValidException e) {
    String mensaje = e.getBindingResult().getFieldErrors().stream()
      .findFirst().map(error -> error.getField() + ": " + error.getDefaultMessage())
      .orElse("Datos inválidos");
    return ResponseEntity.badRequest().body(Map.of("error", mensaje));
  }

  // Reglas que revisa el servicio, por ejemplo la dirección de un despacho
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, String>> reglaInvalida(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
  }

  // JSON mal escrito o con un tipo de dato incorrecto
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Map<String, String>> jsonInvalido(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest().body(Map.of("error", "El cuerpo JSON no es válido"));
  }

  // Id de la URL que no es un número, por ejemplo /api/v1/productos/abc
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<Map<String, String>> parametroInvalido(MethodArgumentTypeMismatchException e) {
    return ResponseEntity.badRequest().body(Map.of("error", e.getName() + ": valor no válido"));
  }
}
