package cl.duoc.ferreteria.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Filtro de clases con dos cambios aprobados por el equipo:
// 1) un Bearer que no valida (alterado o expirado) recibe 401 de inmediato, en vez de seguir como
//    anónimo: así una compra con la sesión vencida no queda registrada como invitado;
// 2) guarda el claim "id" en getDetails() para que BoletaController sepa quién compra.
// No usa UsuarioRepository: los usuarios viven en Auth.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  public JwtAuthenticationFilter(JwtService jwt) { this.jwt = jwt; }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        Claims claims = jwt.validar(header.substring(7));
        String correo = claims.getSubject();
        String rol = claims.get("rol", String.class);
        // ROLE_: convierte VENDEDOR en ROLE_VENDEDOR, lo que espera hasRole
        var auth = new UsernamePasswordAuthenticationToken(correo, null,
          List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        auth.setDetails(claims.get("id", Long.class));
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (JwtException | IllegalArgumentException e) {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Token inválido o expirado\"}");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
