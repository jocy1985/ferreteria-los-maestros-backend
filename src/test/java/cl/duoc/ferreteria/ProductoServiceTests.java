package cl.duoc.ferreteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import cl.duoc.ferreteria.dto.BoletaRequest;
import cl.duoc.ferreteria.dto.ItemBoletaRequest;
import cl.duoc.ferreteria.exception.ConflictoException;
import cl.duoc.ferreteria.model.Categoria;
import cl.duoc.ferreteria.model.Producto;
import cl.duoc.ferreteria.repository.BoletaRepository;
import cl.duoc.ferreteria.repository.CategoriaRepository;
import cl.duoc.ferreteria.repository.DetalleBoletaRepository;
import cl.duoc.ferreteria.repository.ProductoRepository;
import cl.duoc.ferreteria.service.BoletaService;
import cl.duoc.ferreteria.service.CategoriaService;
import cl.duoc.ferreteria.service.ProductoService;

// Pruebas de integración de las reglas de Producto y Categoría (Guía 18, pendiente de la Guía 15).
// Usan una base aparte, ferreteria_test, que se crea sola y se borra al terminar (create-drop):
// nunca tocan ferreteria_los_maestros. Necesitan MySQL de XAMPP encendido.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/ferreteria_test?createDatabaseIfNotExist=true",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ProductoServiceTests {

    @Autowired private ProductoService productoService;
    @Autowired private CategoriaService categoriaService;
    @Autowired private BoletaService boletaService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private BoletaRepository boletaRepository;
    @Autowired private DetalleBoletaRepository detalleRepository;

    private Categoria categoria;

    // Antes de cada prueba se vacían las tablas (los detalles primero, por las llaves foráneas)
    @BeforeEach
    void limpiarBase() {
        detalleRepository.deleteAll();
        boletaRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        categoria = categoriaRepository.save(new Categoria(null, "Herramientas"));
    }

    private Producto producto(String codigo) {
        return new Producto(null, codigo, "Martillo", "Stanley", "unidad", 1000, null, 10, 2,
                null, null, categoria);
    }

    @Test
    void guardaElCodigoEnMayusculasYSinEspacios() {
        Producto creado = productoService.guardar(producto("  hm001 "));
        assertEquals("HM001", creado.getCodigo());
    }

    @Test
    void codigoRepetidoDa409AunqueVengaEnMinusculas() {
        productoService.guardar(producto("HM001"));
        assertThrows(ConflictoException.class, () -> productoService.guardar(producto("hm001")));
        assertEquals(1, productoRepository.count());
    }

    @Test
    void actualizarConElCodigoDeOtroProductoDa409() {
        productoService.guardar(producto("HM001"));
        Producto segundo = productoService.guardar(producto("HM002"));
        assertThrows(ConflictoException.class,
                () -> productoService.actualizar(segundo.getId(), producto("HM001")));
    }

    @Test
    void noSePuedeEliminarUnProductoQueEstaEnUnaBoleta() {
        Producto vendido = productoService.guardar(producto("HM001"));
        BoletaRequest compra = new BoletaRequest("Cliente", "c@c.cl", "912345678", "retiro",
                null, null, null, null, "transferencia",
                List.of(new ItemBoletaRequest(vendido.getId(), 1)));
        boletaService.guardar(compra, null);

        assertThrows(ConflictoException.class, () -> productoService.eliminar(vendido.getId()));
    }

    // La restricción UNIQUE de la base rechaza el duplicado aunque no pase por el servicio
    @Test
    void laBaseRechazaUnCodigoRepetido() {
        productoRepository.saveAndFlush(producto("HM001"));
        assertThrows(DataIntegrityViolationException.class,
                () -> productoRepository.saveAndFlush(producto("HM001")));
    }

    @Test
    void nombreDeCategoriaRepetidoDa409SinImportarMayusculas() {
        assertThrows(ConflictoException.class,
                () -> categoriaService.guardar(new Categoria(null, "HERRAMIENTAS")));
        assertThrows(DataIntegrityViolationException.class,
                () -> categoriaRepository.saveAndFlush(new Categoria(null, "herramientas")));
    }
}
