package cl.duoc.ferreteria.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ferreteria.dto.DetalleBoletaResponse;
import cl.duoc.ferreteria.service.DetalleBoletaService;

// Solo lectura: los detalles se crean dentro del POST de boleta
@RestController
@RequestMapping("/api/v1/detalles-boleta")
public class DetalleBoletaController {
  private final DetalleBoletaService service;
  public DetalleBoletaController(DetalleBoletaService service) { this.service = service; }

  @GetMapping public List<DetalleBoletaResponse> listar() {
    return service.listar().stream().map(DetalleBoletaResponse::desde).toList();
  }

  @GetMapping("/{id}") public DetalleBoletaResponse buscar(@PathVariable Long id) {
    return DetalleBoletaResponse.desde(service.buscar(id));
  }
}
