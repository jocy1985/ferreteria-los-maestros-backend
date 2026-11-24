package cl.duoc.ferreteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.duoc.ferreteria.dto.BoletaActualizarRequest;
import cl.duoc.ferreteria.dto.BoletaRequest;
import cl.duoc.ferreteria.dto.ItemBoletaRequest;
import cl.duoc.ferreteria.exception.ConflictoException;
import cl.duoc.ferreteria.model.Boleta;
import cl.duoc.ferreteria.model.Categoria;
import cl.duoc.ferreteria.model.DetalleBoleta;
import cl.duoc.ferreteria.model.Producto;
import cl.duoc.ferreteria.repository.BoletaRepository;
import cl.duoc.ferreteria.repository.CategoriaRepository;
import cl.duoc.ferreteria.repository.DetalleBoletaRepository;
import cl.duoc.ferreteria.repository.ProductoRepository;
import cl.duoc.ferreteria.service.BoletaService;

// Pruebas de integración de las reglas de Boleta: stock, total con oferta y anulación.
// Misma base aparte que ProductoServiceTests (ferreteria_test). Necesitan MySQL de XAMPP encendido
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/ferreteria_test?createDatabaseIfNotExist=true",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class BoletaServiceTests {

    @Autowired private BoletaService boletaService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private BoletaRepository boletaRepository;
    @Autowired private DetalleBoletaRepository detalleRepository;

    private Producto conOferta;   // precio 1000, oferta 800, stock 10
    private Producto sinOferta;   // precio 500, stock 5

    @BeforeEach
    void prepararDatos() {
        detalleRepository.deleteAll();
        boletaRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        Categoria categoria = categoriaRepository.save(new Categoria(null, "Herramientas"));
        conOferta = productoRepository.save(new Producto(null, "HM001", "Martillo", "Stanley",
                "unidad", 1000, 800, 10, 2, null, null, categoria));
        sinOferta = productoRepository.save(new Producto(null, "HM002", "Alicate", "Bahco",
                "unidad", 500, null, 5, 1, null, null, categoria));
    }

    private BoletaRequest compra(String entrega, ItemBoletaRequest... items) {
        return new BoletaRequest("Cliente", "c@c.cl", "912345678", entrega, null, null, null,
                null, "transferencia", List.of(items));
    }

    private int stockActual(Producto producto) {
        return productoRepository.findById(producto.getId()).orElseThrow().getStock();
    }

    @Test
    void laCompraDescuentaStockYCobraElPrecioDeOferta() {
        Boleta boleta = boletaService.guardar(compra("retiro",
                new ItemBoletaRequest(conOferta.getId(), 2),
                new ItemBoletaRequest(sinOferta.getId(), 3)), 7L);

        // 2 x 800 (oferta) + 3 x 500 (precio normal)
        assertEquals(3100, boleta.getTotal());
        assertEquals("RECIBIDA", boleta.getEstado());
        assertEquals(7L, boleta.getUsuarioId());
        assertEquals(8, stockActual(conOferta));
        assertEquals(2, stockActual(sinOferta));

        DetalleBoleta lineaOferta = detalleRepository.findByBoletaId(boleta.getId()).stream()
                .filter(d -> d.getProducto().getId().equals(conOferta.getId())).findFirst().orElseThrow();
        assertEquals(800, lineaOferta.getPrecioUnitario());
        assertEquals(1600, lineaOferta.getSubtotal());
    }

    @Test
    void sinSesionLaBoletaQuedaSinUsuario() {
        Boleta boleta = boletaService.guardar(compra("retiro", new ItemBoletaRequest(sinOferta.getId(), 1)), null);
        assertNull(boleta.getUsuarioId());
    }

    // @Transactional de guardar: la segunda línea falla y se revierte también la primera
    @Test
    void stockInsuficienteDa409YNoGuardaNada() {
        assertThrows(ConflictoException.class, () -> boletaService.guardar(compra("retiro",
                new ItemBoletaRequest(conOferta.getId(), 2),
                new ItemBoletaRequest(sinOferta.getId(), 6)), null));

        assertEquals(10, stockActual(conOferta));
        assertEquals(5, stockActual(sinOferta));
        assertEquals(0, boletaRepository.count());
        assertEquals(0, detalleRepository.count());
    }

    @Test
    void despachoSinDireccionEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> boletaService.guardar(
                compra("despacho", new ItemBoletaRequest(sinOferta.getId(), 1)), null));
        assertEquals(5, stockActual(sinOferta));
    }

    @Test
    void anularDevuelveElStockYMarcaAnulada() {
        Boleta boleta = boletaService.guardar(compra("retiro",
                new ItemBoletaRequest(conOferta.getId(), 4),
                new ItemBoletaRequest(sinOferta.getId(), 5)), null);

        boletaService.anular(boleta.getId());

        assertEquals(10, stockActual(conOferta));
        assertEquals(5, stockActual(sinOferta));
        assertEquals("ANULADA", boletaService.buscar(boleta.getId()).getEstado());
    }

    @Test
    void unaBoletaAnuladaNoSeVuelveAAnularNiSeModifica() {
        Boleta boleta = boletaService.guardar(compra("retiro", new ItemBoletaRequest(conOferta.getId(), 1)), null);
        boletaService.anular(boleta.getId());

        assertThrows(ConflictoException.class, () -> boletaService.anular(boleta.getId()));
        // El segundo intento no devolvió stock de nuevo
        assertEquals(10, stockActual(conOferta));

        BoletaActualizarRequest cambio = new BoletaActualizarRequest("Otro", "o@o.cl", "911111111",
                "retiro", null, null, null, null, "CONFIRMADA");
        assertThrows(ConflictoException.class, () -> boletaService.actualizar(boleta.getId(), cambio));
    }
}
