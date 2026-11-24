package cl.duoc.ferreteria.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Una línea de la compra: el precio no se recibe, lo calcula el servicio
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemBoletaRequest {

    @NotNull
    private Long productoId;

    @NotNull
    @Min(1)
    private Integer cantidad;
}
