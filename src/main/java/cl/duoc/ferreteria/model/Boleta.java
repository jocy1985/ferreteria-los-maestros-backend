package cl.duoc.ferreteria.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="boleta")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String numero;

    @NotNull
    private LocalDateTime fecha;

    // Datos de contacto: se guardan aquí porque el checkout admite compras sin sesión
    @NotBlank
    private String nombreCliente;

    @NotBlank
    private String correoCliente;

    @NotBlank
    private String telefonoCliente;

    @NotBlank
    private String entrega;

    private String calle;

    private String numeroDireccion;

    private String comuna;

    private String indicaciones;

    @NotBlank
    private String formaPago;

    @NotNull
    @Min(0)
    private Integer total;

    // RECIBIDA, CONFIRMADA, ENTREGADA o ANULADA
    @NotBlank
    private String estado;

    // Usuario 1-N Boleta, como relación lógica: los usuarios viven en la base de Auth, así que aquí
    // solo se guarda su id (sin llave foránea). Sale del claim "id" del token; null = compra sin sesión
    @Column(name = "usuario_id")
    private Long usuarioId;
}
