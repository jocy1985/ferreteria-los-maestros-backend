package cl.duoc.ferreteria.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.duoc.ferreteria.exception.ConflictoException;
import cl.duoc.ferreteria.exception.RecursoNoEncontradoException;
import cl.duoc.ferreteria.model.Categoria;
import cl.duoc.ferreteria.model.Producto;
import cl.duoc.ferreteria.repository.DetalleBoletaRepository;
import cl.duoc.ferreteria.repository.ProductoRepository;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository repository;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private DetalleBoletaRepository detalleRepository;

    public List<Producto> listar() {
        return repository.findAll();
    }

    public Producto buscar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Producto no encontrado: " + id));
    }

    public Producto guardar(Producto producto) {
        // Misma normalización del frontend: el código se guarda en mayúsculas
        String codigo = producto.getCodigo().trim().toUpperCase();
        if (repository.existsByCodigo(codigo)) {
            throw new ConflictoException("Ya existe un producto con ese código: " + codigo);
        }
        producto.setId(null);
        producto.setCodigo(codigo);
        producto.setCategoria(resolverCategoria(producto.getCategoria()));
        return repository.save(producto);
    }

    public Producto actualizar(Long id, Producto datos) {
        Producto actual = buscar(id);
        String codigo = datos.getCodigo().trim().toUpperCase();
        if (repository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ConflictoException("Ya existe un producto con ese código: " + codigo);
        }
        actual.setCodigo(codigo);
        actual.setNombre(datos.getNombre());
        actual.setMarca(datos.getMarca());
        actual.setUnidad(datos.getUnidad());
        actual.setPrecio(datos.getPrecio());
        actual.setPrecioOferta(datos.getPrecioOferta());
        actual.setStock(datos.getStock());
        actual.setStockMinimo(datos.getStockMinimo());
        actual.setImagen(datos.getImagen());
        actual.setDescripcion(datos.getDescripcion());
        actual.setCategoria(resolverCategoria(datos.getCategoria()));
        return repository.save(actual);
    }

    public void eliminar(Long id) {
        Producto producto = buscar(id);
        if (detalleRepository.existsByProductoId(id)) {
            throw new ConflictoException("No se puede eliminar: el producto aparece en boletas");
        }
        repository.delete(producto);
    }

    // El JSON solo trae el id de la categoría: se busca la categoría real (404 si no existe)
    private Categoria resolverCategoria(Categoria categoria) {
        if (categoria.getId() == null) {
            throw new IllegalArgumentException("categoria.id: es obligatorio");
        }
        return categoriaService.buscar(categoria.getId());
    }
}
