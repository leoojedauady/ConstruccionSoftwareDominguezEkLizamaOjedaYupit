package gestor.prestamos.modelo;

import java.time.LocalDate;

/**
 * Periodo de un préstamo: la fecha en que se presta la herramienta y la fecha
 * máxima en que debe devolverse.
 *
 * <p><b>Técnica 8 - Introduce Parameter Object.</b> En el código original las fechas
 * de préstamo y límite viajaban siempre juntas como dos parámetros sueltos
 * ({@code GestorPrestamos.prestar(id, prestatario, fechaPrestamo, fechaLimite)} y el
 * constructor de {@link Prestamo} con ocho parámetros). Se agrupan en este objeto,
 * que además concentra la regla que las relaciona (el límite no puede ser anterior
 * al inicio). Así las firmas son más cortas y la regla vive en un solo lugar.</p>
 *
 * @param inicio fecha en que se presta la herramienta
 * @param limite fecha máxima de devolución
 */
public record PeriodoPrestamo(LocalDate inicio, LocalDate limite) {

    /**
     * Comprueba que el periodo es válido. Lo invoca {@link Prestamo} en el mismo punto
     * en el que el código original validaba las dos fechas, para que el orden de los
     * mensajes de error no cambie.
     *
     * @throws IllegalArgumentException si falta alguna fecha o si el límite es anterior al inicio
     */
    public void validar() {
        if (inicio == null || limite == null) {
            throw new IllegalArgumentException("Las fechas de préstamo y límite son obligatorias");
        }
        if (limite.isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha límite no puede ser anterior al préstamo");
        }
    }
}
