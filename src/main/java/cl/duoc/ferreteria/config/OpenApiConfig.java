package cl.duoc.ferreteria.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

// Igual al backend de clases: agrega el esquema Bearer y el botón Authorize en Swagger.
// El token se obtiene en el Swagger de Auth (http://localhost:8081/swagger-ui/index.html)
@Configuration
@OpenAPIDefinition(
  info = @Info(
    title = "Ferretería Los Maestros - API de la tienda",
    version = "1.0",
    description = "Productos, categorías, boletas y detalles. Valida el JWT emitido por Auth"),
  security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
  name = "bearerAuth",
  type = SecuritySchemeType.HTTP,
  scheme = "bearer",
  bearerFormat = "JWT"
)
public class OpenApiConfig {}
