package cl.duoc.ferreteria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ferreteria.model.Producto;
import cl.duoc.ferreteria.service.ProductoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {
  private final ProductoService service;
  public ProductoController(ProductoService service) { this.service = service; }

  @GetMapping public List<Producto> listar() { return service.listar(); }
  @GetMapping("/{id}") public Producto buscar(@PathVariable Long id) { return service.buscar(id); }

  @PostMapping
  public ResponseEntity<Producto> crear(@Valid @RequestBody Producto producto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.guardar(producto));
  }

  @PutMapping("/{id}")
  public Producto actualizar(@PathVariable Long id,
      @Valid @RequestBody Producto producto) {
    return service.actualizar(id, producto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}
