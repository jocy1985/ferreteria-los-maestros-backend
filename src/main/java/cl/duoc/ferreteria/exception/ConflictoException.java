package cl.duoc.ferreteria.exception;

// Se responde 409: la solicitud es válida, pero choca con el estado actual de los datos
public class ConflictoException extends RuntimeException {
  public ConflictoException(String mensaje) {
    super(mensaje);
  }

}
