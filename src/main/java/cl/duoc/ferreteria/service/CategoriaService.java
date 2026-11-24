package cl.duoc.ferreteria.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.duoc.ferreteria.exception.ConflictoException;
import cl.duoc.ferreteria.exception.RecursoNoEncontradoException;
import cl.duoc.ferreteria.model.Categoria;
import cl.duoc.ferreteria.repository.CategoriaRepository;
import cl.duoc.ferreteria.repository.ProductoRepository;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository repository;

    @Autowired
    private ProductoRepository productoRepository;

    public List<Categoria> listar() {
        return repository.findAll();
    }

    public Categoria buscar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Categoría no encontrada: " + id));
    }

    public Categoria guardar(Categoria categoria) {
        if (repository.existsByNombreIgnoreCase(categoria.getNombre())) {
            throw new ConflictoException("Ya existe una categoría con ese nombre: " + categoria.getNombre());
        }
        // El id lo genera la base: se ignora si viene en el JSON (Swagger envía 0 de ejemplo)
        categoria.setId(null);
        return repository.save(categoria);
    }

    public Categoria actualizar(Long id, Categoria datos) {
        Categoria actual = buscar(id);
        if (repository.existsByNombreIgnoreCaseAndIdNot(datos.getNombre(), id)) {
            throw new ConflictoException("Ya existe una categoría con ese nombre: " + datos.getNombre());
        }
        actual.setNombre(datos.getNombre());
        return repository.save(actual);
    }

    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede eliminar: la categoría tiene productos asociados");
        }
        repository.delete(categoria);
    }
}
