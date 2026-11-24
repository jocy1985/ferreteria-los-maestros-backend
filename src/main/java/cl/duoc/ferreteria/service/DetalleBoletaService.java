package cl.duoc.ferreteria.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.duoc.ferreteria.exception.RecursoNoEncontradoException;
import cl.duoc.ferreteria.model.DetalleBoleta;
import cl.duoc.ferreteria.repository.DetalleBoletaRepository;

// Solo lectura: los detalles se crean dentro del POST de boleta
@Service
public class DetalleBoletaService {

    @Autowired
    private DetalleBoletaRepository repository;

    public List<DetalleBoleta> listar() {
        return repository.findAll();
    }

    public DetalleBoleta buscar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Detalle de boleta no encontrado: " + id));
    }
}
