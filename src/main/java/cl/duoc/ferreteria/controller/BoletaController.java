package cl.duoc.ferreteria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ferreteria.dto.BoletaActualizarRequest;
import cl.duoc.ferreteria.dto.BoletaRequest;
import cl.duoc.ferreteria.dto.BoletaResponse;
import cl.duoc.ferreteria.dto.DetalleBoletaResponse;
import cl.duoc.ferreteria.model.Boleta;
import cl.duoc.ferreteria.service.BoletaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/boletas")
public class BoletaController {
  private final BoletaService service;
  public BoletaController(BoletaService service) { this.service = service; }

  @GetMapping public List<BoletaResponse> listar() {
    return service.listar().stream().map(BoletaResponse::desde).toList();
  }

  // Solo CLIENTE (SecurityConfig): sus propias compras. No recibe id: usa el del token, así un
  // cliente no puede ver las boletas de otro. Va antes de "/{id}" solo por orden de lectura
  @GetMapping("/mias") public List<BoletaResponse> listarMias(Authentication autenticacion) {
    Long usuarioId = (Long) autenticacion.getDetails();
    return service.listarDelUsuario(usuarioId).stream().map(BoletaResponse::desde).toList();
  }

  @GetMapping("/{id}") public BoletaResponse buscar(@PathVariable Long id) {
    return BoletaResponse.desde(service.buscar(id));
  }

  @GetMapping("/{id}/detalles")
  public List<DetalleBoletaResponse> listarDetalles(@PathVariable Long id) {
    return service.listarDetalles(id).stream().map(DetalleBoletaResponse::desde).toList();
  }

  // Público: con token la boleta queda a nombre de quien compra (id del token, que el filtro
  // guardó en getDetails()); sin token, autenticacion es null y la compra queda como invitado
  @PostMapping
  public ResponseEntity<BoletaResponse> crear(@Valid @RequestBody BoletaRequest boleta,
      Authentication autenticacion) {
    Long usuarioId = autenticacion != null ? (Long) autenticacion.getDetails() : null;
    Boleta creada = service.guardar(boleta, usuarioId);
    // Responde con los detalles: así la confirmación de compra no necesita GET /detalles (solo personal)
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(BoletaResponse.desde(creada, service.listarDetalles(creada.getId())));
  }

  @PutMapping("/{id}")
  public BoletaResponse actualizar(@PathVariable Long id,
      @Valid @RequestBody BoletaActualizarRequest boleta) {
    return BoletaResponse.desde(service.actualizar(id, boleta));
  }

  // No borra el registro: anula la boleta y devuelve el stock
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) { service.anular(id); }
}
