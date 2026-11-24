package cl.duoc.ferreteria.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Datos que envía el cliente en el POST de boleta. El total y el stock se resuelven en el servicio
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoletaRequest {

    // No trae usuarioId: el backend lo toma del token (claim "id"), así nadie compra a nombre de otro

    @NotBlank
    private String nombreCliente;

    @NotBlank
    @Email
    private String correoCliente;

    @NotBlank
    private String telefonoCliente;

    @NotBlank
    @Pattern(regexp = "retiro|despacho", message = "debe ser retiro o despacho")
    private String entrega;

    private String calle;

    private String numeroDireccion;

    private String comuna;

    private String indicaciones;

    @NotBlank
    private String formaPago;

    // @Valid aplica las reglas de ItemBoletaRequest a cada elemento de la lista
    @NotEmpty
    @Valid
    private List<ItemBoletaRequest> items;
}
