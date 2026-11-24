package cl.duoc.ferreteria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Campos que permite cambiar el PUT de boleta. ANULADA no se acepta aquí: se anula con DELETE
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoletaActualizarRequest {

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
    @Pattern(regexp = "RECIBIDA|CONFIRMADA|ENTREGADA", message = "debe ser RECIBIDA, CONFIRMADA o ENTREGADA")
    private String estado;
}
