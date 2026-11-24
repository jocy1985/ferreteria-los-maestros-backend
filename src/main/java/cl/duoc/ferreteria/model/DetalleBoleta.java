package cl.duoc.ferreteria.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="detalle_boleta")
public class DetalleBoleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Min(1)
    private Integer cantidad;

    // Precio cobrado al vender: no cambia si después cambia el precio del producto
    @NotNull
    @Min(0)
    private Integer precioUnitario;

    @NotNull
    @Min(0)
    private Integer subtotal;

    // Boleta 1-N DetalleBoleta
    @NotNull
    @ManyToOne
    @JoinColumn(name = "boleta_id")
    private Boleta boleta;

    // Producto 1-N DetalleBoleta
    @NotNull
    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;
}
