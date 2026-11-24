package cl.duoc.ferreteria;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

// Restricciones por rol (Guía 17). Ninguna solicitud llega a modificar la base: todas se
// rechazan en SecurityConfig antes del controlador. Los tokens se firman aquí con la misma
// clave que usa Auth, porque este backend no genera tokens. Necesita MySQL (levanta la aplicación)
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadTests {

    @Autowired
    private MockMvc mvc;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.issuer}")
    private String issuer;

    private String token(String rol, long id) {
        Date ahora = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .subject(rol.toLowerCase() + "@losmaestros.cl")
                .claim("rol", rol)
                .claim("id", id)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + 60000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();
    }

    private static final String COMPRA = """
            {"nombreCliente":"Prueba","correoCliente":"p@p.cl","telefonoCliente":"912345678",
             "entrega":"retiro","formaPago":"transferencia","items":[{"productoId":1,"cantidad":1}]}""";

    @Test
    void elVendedorNoPuedeComprar() throws Exception {
        mvc.perform(post("/api/v1/boletas").header("Authorization", "Bearer " + token("VENDEDOR", 2))
                .contentType(MediaType.APPLICATION_JSON).content(COMPRA))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("No tienes permisos para esta operación"));
    }

    @Test
    void elClienteNoVeBoletasNiDetalles() throws Exception {
        String cliente = "Bearer " + token("CLIENTE", 1);
        mvc.perform(get("/api/v1/boletas").header("Authorization", cliente)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/boletas/1/detalles").header("Authorization", cliente)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/detalles-boleta").header("Authorization", cliente)).andExpect(status().isForbidden());
    }

    @Test
    void sinTokenNoSeVenBoletas() throws Exception {
        mvc.perform(get("/api/v1/boletas"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Debes iniciar sesión"));
    }

    @Test
    void elVendedorNoModificaProductos() throws Exception {
        mvc.perform(post("/api/v1/productos").header("Authorization", "Bearer " + token("VENDEDOR", 2))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    // GET /boletas/mias: solo CLIENTE, filtrado por el id del token (solo lectura, no modifica la base)
    @Test
    void elClienteVeSoloSusPropiasCompras() throws Exception {
        mvc.perform(get("/api/v1/boletas/mias").header("Authorization", "Bearer " + token("CLIENTE", 1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].usuarioId", everyItem(is(1))));
        // Un cliente sin compras recibe una lista vacía, no las boletas de otros
        mvc.perform(get("/api/v1/boletas/mias").header("Authorization", "Bearer " + token("CLIENTE", 999)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void misComprasEsSoloParaClientes() throws Exception {
        mvc.perform(get("/api/v1/boletas/mias").header("Authorization", "Bearer " + token("VENDEDOR", 2)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/boletas/mias").header("Authorization", "Bearer " + token("ADMINISTRADOR", 3)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/boletas/mias")).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenAlteradoRecibe401AunqueLaRutaSeaPublica() throws Exception {
        mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer " + token("CLIENTE", 1) + "x"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Token inválido o expirado"));
    }
}
