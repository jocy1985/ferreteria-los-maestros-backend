package cl.duoc.ferreteria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name="producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guía 18: UNIQUE también en la base (además de la revisión del servicio), así dos
    // solicitudes simultáneas no pueden dejar el mismo código repetido
    @NotBlank
    @Column(unique = true)
    private String codigo;

    @NotBlank
    private String nombre;

    @NotBlank
    private String marca;

    @NotBlank
    private String unidad;

    @NotNull
    @Min(0)
    private Integer precio;

    // Opcional: si viene, es el precio que se cobra en la boleta
    @Min(0)
    private Integer precioOferta;

    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    @Min(0)
    private Integer stockMinimo;

    private String imagen;

    private String descripcion;

    // Categoria 1-N Producto. En el JSON basta enviar { "categoria": { "id": 1 } }
    @NotNull
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
