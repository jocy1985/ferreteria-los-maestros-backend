package cl.duoc.ferreteria.dto;

import java.time.LocalDateTime;
import java.util.List;

import cl.duoc.ferreteria.model.Boleta;
import cl.duoc.ferreteria.model.DetalleBoleta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Boleta que devuelve la API. Del usuario solo se conoce su id (los datos viven en Auth)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoletaResponse {

    private Long id;
    private String numero;
    private LocalDateTime fecha;
    private String nombreCliente;
    private String correoCliente;
    private String telefonoCliente;
    private String entrega;
    private String calle;
    private String numeroDireccion;
    private String comuna;
    private String indicaciones;
    private String formaPago;
    private Integer total;
    private String estado;
    // null en las compras sin sesión
    private Long usuarioId;
    // Solo en la respuesta del POST: el comprador (cliente o invitado) recibe su detalle sin
    // llamar a GET /boletas/{id}/detalles, que es solo para VENDEDOR y ADMINISTRADOR.
    // En los GET va null: ahí el detalle se pide aparte
    private List<DetalleCompraResponse> detalles;

    public static BoletaResponse desde(Boleta boleta) {
        return new BoletaResponse(boleta.getId(), boleta.getNumero(), boleta.getFecha(), boleta.getNombreCliente(),
                boleta.getCorreoCliente(), boleta.getTelefonoCliente(), boleta.getEntrega(), boleta.getCalle(),
                boleta.getNumeroDireccion(), boleta.getComuna(), boleta.getIndicaciones(), boleta.getFormaPago(),
                boleta.getTotal(), boleta.getEstado(), boleta.getUsuarioId(), null);
    }

    // Respuesta del POST: la boleta recién creada con sus líneas
    public static BoletaResponse desde(Boleta boleta, List<DetalleBoleta> detalles) {
        BoletaResponse respuesta = desde(boleta);
        respuesta.setDetalles(detalles.stream().map(DetalleCompraResponse::desde).toList());
        return respuesta;
    }
}
