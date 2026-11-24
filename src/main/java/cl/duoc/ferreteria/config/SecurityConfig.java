package cl.duoc.ferreteria.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import cl.duoc.ferreteria.security.JwtAuthenticationFilter;

// Seguridad del backend de la tienda (Guía 17). Reemplaza al WebConfig de la Guía 16:
// el CORS ahora se declara aquí, como en el proyecto de clases.
@Configuration
public class SecurityConfig {
  @Bean SecurityFilterChain filterChain(HttpSecurity http,
      JwtAuthenticationFilter jwtFilter) throws Exception {
    return http.csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
          "/v3/api-docs/**").permitAll()
        // /error: ruta interna donde Spring reenvía los errores; si cae en denyAll sin identidad,
        // el error llega convertido en 401
        .requestMatchers("/error").permitAll()
        // Públicos (decisión del equipo): la tienda se ve sin sesión
        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/categorias/**").permitAll()
        // Comprar: invitado (sin token), CLIENTE y ADMINISTRADOR. El VENDEDOR no compra (Anexo 1:
        // solo ve productos y órdenes), así que con su token responde 403
        .requestMatchers(HttpMethod.POST, "/api/v1/boletas").not().hasRole("VENDEDOR")
        // CLIENTE: solo sus propias compras (filtradas por el id del token). Debe ir antes que
        // la regla de /boletas/**, porque Spring usa la primera regla que coincide
        .requestMatchers(HttpMethod.GET, "/api/v1/boletas/mias").hasRole("CLIENTE")
        // VENDEDOR: solo lectura de boletas y sus detalles (productos ya son públicos)
        .requestMatchers(HttpMethod.GET, "/api/v1/boletas/**", "/api/v1/detalles-boleta/**")
          .hasAnyRole("VENDEDOR", "ADMINISTRADOR")
        // Todo lo demás de la API (crear, editar, borrar, anular): solo ADMINISTRADOR
        .requestMatchers("/api/v1/**").hasRole("ADMINISTRADOR")
        .anyRequest().denyAll())
      .exceptionHandling(e -> e
        // 401: falta el token. En clases era HttpStatusEntryPoint, sin cuerpo
        .authenticationEntryPoint((request, response, excepcion) -> {
          response.setStatus(HttpStatus.UNAUTHORIZED.value());
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().write("{\"error\":\"Debes iniciar sesión\"}");
        })
        // 403: token válido pero el rol no alcanza
        .accessDeniedHandler((request, response, excepcion) -> {
          response.setStatus(HttpStatus.FORBIDDEN.value());
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().write("{\"error\":\"No tienes permisos para esta operación\"}");
        }))
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
      .build();
  }

  // CORS: solo el frontend de Vite, y ahora acepta el encabezado Authorization (Bearer)
  // Guía 18: el origen permitido sale de app.cors.allowed-origin (localhost:5173 en desarrollo,
  // http://IP_PUBLICA en producción) en vez de ir escrito en el código
  @Bean CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origin}") String allowedOrigin) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(List.of(allowedOrigin));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", c);
    return source;
  }
}
