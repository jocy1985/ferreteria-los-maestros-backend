package cl.duoc.ferreteria.dto;

import cl.duoc.ferreteria.model.DetalleBoleta;
import cl.duoc.ferreteria.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Línea de una boleta dentro de la respuesta del POST. Igual a DetalleBoletaResponse,
// pero sin repetir la boleta (ya es el objeto que la contiene)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraResponse {

    private Long id;
    private Integer cantidad;
    private Integer precioUnitario;
    private Integer subtotal;
    private Producto producto;

    public static DetalleCompraResponse desde(DetalleBoleta detalle) {
        return new DetalleCompraResponse(detalle.getId(), detalle.getCantidad(), detalle.getPrecioUnitario(),
                detalle.getSubtotal(), detalle.getProducto());
    }
}
