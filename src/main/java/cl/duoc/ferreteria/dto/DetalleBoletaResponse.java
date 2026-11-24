package cl.duoc.ferreteria.dto;

import cl.duoc.ferreteria.model.DetalleBoleta;
import cl.duoc.ferreteria.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Detalle que devuelve la API: la boleta va como BoletaResponse
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleBoletaResponse {

    private Long id;
    private Integer cantidad;
    private Integer precioUnitario;
    private Integer subtotal;
    private BoletaResponse boleta;
    private Producto producto;

    public static DetalleBoletaResponse desde(DetalleBoleta detalle) {
        return new DetalleBoletaResponse(detalle.getId(), detalle.getCantidad(), detalle.getPrecioUnitario(),
                detalle.getSubtotal(), BoletaResponse.desde(detalle.getBoleta()), detalle.getProducto());
    }
}
