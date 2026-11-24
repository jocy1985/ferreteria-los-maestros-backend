package cl.duoc.ferreteria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.ferreteria.model.Boleta;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    // Compras de un cliente (GET /api/v1/boletas/mias), de la más nueva a la más antigua
    List<Boleta> findByUsuarioIdOrderByIdDesc(Long usuarioId);
}
