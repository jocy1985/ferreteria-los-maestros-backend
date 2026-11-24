package cl.duoc.ferreteria.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.ferreteria.dto.BoletaActualizarRequest;
import cl.duoc.ferreteria.dto.BoletaRequest;
import cl.duoc.ferreteria.dto.ItemBoletaRequest;
import cl.duoc.ferreteria.exception.ConflictoException;
import cl.duoc.ferreteria.exception.RecursoNoEncontradoException;
import cl.duoc.ferreteria.model.Boleta;
import cl.duoc.ferreteria.model.DetalleBoleta;
import cl.duoc.ferreteria.model.Producto;
import cl.duoc.ferreteria.repository.BoletaRepository;
import cl.duoc.ferreteria.repository.DetalleBoletaRepository;
import cl.duoc.ferreteria.repository.ProductoRepository;

@Service
public class BoletaService {

    private static final String ESTADO_RECIBIDA = "RECIBIDA";
    private static final String ESTADO_ANULADA = "ANULADA";

    @Autowired
    private BoletaRepository repository;

    @Autowired
    private DetalleBoletaRepository detalleRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoService productoService;

    public List<Boleta> listar() {
        return repository.findAll();
    }

    public Boleta buscar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Boleta no encontrada: " + id));
    }

    // Compras del cliente que hizo la solicitud: el id viene del token, nunca de un parámetro
    public List<Boleta> listarDelUsuario(Long usuarioId) {
        return repository.findByUsuarioIdOrderByIdDesc(usuarioId);
    }

    public List<DetalleBoleta> listarDetalles(Long id) {
        buscar(id);
        return detalleRepository.findByBoletaId(id);
    }

    // @Transactional: si falla cualquier línea se revierte todo (boleta, detalles y stock)
    // usuarioId llega desde el token (BoletaController); null = compra como invitado
    @Transactional
    public Boleta guardar(BoletaRequest datos, Long usuarioId) {
        validarDireccion(datos.getEntrega(), datos.getCalle(), datos.getNumeroDireccion(), datos.getComuna());

        Boleta boleta = new Boleta();
        boleta.setNumero("LM-" + String.valueOf(System.currentTimeMillis()).substring(5));
        boleta.setFecha(LocalDateTime.now());
        boleta.setNombreCliente(datos.getNombreCliente());
        boleta.setCorreoCliente(datos.getCorreoCliente());
        boleta.setTelefonoCliente(datos.getTelefonoCliente());
        boleta.setEntrega(datos.getEntrega());
        boleta.setCalle(datos.getCalle());
        boleta.setNumeroDireccion(datos.getNumeroDireccion());
        boleta.setComuna(datos.getComuna());
        boleta.setIndicaciones(datos.getIndicaciones());
        boleta.setFormaPago(datos.getFormaPago());
        boleta.setEstado(ESTADO_RECIBIDA);
        boleta.setTotal(0);
        boleta.setUsuarioId(usuarioId);
        boleta = repository.save(boleta);

        int total = 0;
        for (ItemBoletaRequest item : datos.getItems()) {
            Producto producto = productoService.buscar(item.getProductoId());
            if (producto.getStock() < item.getCantidad()) {
                throw new ConflictoException("Stock insuficiente para " + producto.getCodigo()
                        + ": disponible " + producto.getStock() + ", solicitado " + item.getCantidad());
            }
            producto.setStock(producto.getStock() - item.getCantidad());
            productoRepository.save(producto);

            // Se cobra el precio de oferta cuando existe
            int precioUnitario = producto.getPrecioOferta() != null ? producto.getPrecioOferta() : producto.getPrecio();
            int subtotal = precioUnitario * item.getCantidad();
            total += subtotal;

            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setBoleta(boleta);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotal);
            detalleRepository.save(detalle);
        }

        boleta.setTotal(total);
        return repository.save(boleta);
    }

    // Solo contacto, entrega y estado: productos, totales y forma de pago no cambian
    public Boleta actualizar(Long id, BoletaActualizarRequest datos) {
        Boleta actual = buscar(id);
        validarNoAnulada(actual);
        validarDireccion(datos.getEntrega(), datos.getCalle(), datos.getNumeroDireccion(), datos.getComuna());
        actual.setNombreCliente(datos.getNombreCliente());
        actual.setCorreoCliente(datos.getCorreoCliente());
        actual.setTelefonoCliente(datos.getTelefonoCliente());
        actual.setEntrega(datos.getEntrega());
        actual.setCalle(datos.getCalle());
        actual.setNumeroDireccion(datos.getNumeroDireccion());
        actual.setComuna(datos.getComuna());
        actual.setIndicaciones(datos.getIndicaciones());
        actual.setEstado(datos.getEstado());
        return repository.save(actual);
    }

    // No borra el registro: la marca ANULADA y devuelve el stock de sus detalles
    @Transactional
    public void anular(Long id) {
        Boleta boleta = buscar(id);
        validarNoAnulada(boleta);
        for (DetalleBoleta detalle : detalleRepository.findByBoletaId(id)) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());
            productoRepository.save(producto);
        }
        boleta.setEstado(ESTADO_ANULADA);
        repository.save(boleta);
    }

    private void validarNoAnulada(Boleta boleta) {
        if (ESTADO_ANULADA.equals(boleta.getEstado())) {
            throw new ConflictoException("La boleta " + boleta.getNumero() + " está anulada y no se puede modificar");
        }
    }

    // Misma regla del checkout: el despacho exige calle, número y comuna
    private void validarDireccion(String entrega, String calle, String numero, String comuna) {
        if ("despacho".equals(entrega) && (estaVacio(calle) || estaVacio(numero) || estaVacio(comuna))) {
            throw new IllegalArgumentException("El despacho requiere calle, número y comuna");
        }
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
