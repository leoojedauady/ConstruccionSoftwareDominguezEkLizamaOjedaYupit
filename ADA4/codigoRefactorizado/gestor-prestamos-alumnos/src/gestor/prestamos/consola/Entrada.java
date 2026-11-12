package gestor.prestamos.consola;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lectura de datos desde la consola con validación. Repite la petición hasta
 * obtener un valor correcto.
 */
public final class Entrada {

    /**
     * Forma de una fecha ISO-8601 ({@code AAAA-MM-DD}) tal como la acepta
     * {@link LocalDate#parse(CharSequence)}: año de 4 cifras (o con signo si tiene más
     * cifras o es negativo), mes de 2 cifras y día de 2 cifras.
     */
    private static final Pattern FORMATO_FECHA =
            Pattern.compile("(\\d{4}|\\+\\d{5,10}|-\\d{4,10})-(\\d{2})-(\\d{2})");

    private final Scanner scanner;

    /**
     * Crea una entrada que lee del escáner indicado.
     *
     * @param scanner fuente de las líneas introducidas por el usuario; no puede ser nula
     */
    public Entrada(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    /**
     * Pide un texto no vacío.
     *
     * @param mensaje texto que se muestra antes de leer
     * @return el texto introducido, sin espacios sobrantes
     */
    public String pedirTexto(String mensaje) {
        while (true) {
            String valor = leer(mensaje).trim();
            if (!valor.isEmpty()) {
                return valor;
            }
            System.out.println("  El campo no puede estar vacío.");
        }
    }

    /**
     * Pide un número entero que debe pertenecer a un conjunto de opciones válidas.
     *
     * @param mensaje texto que se muestra antes de leer
     * @param validas números aceptados
     * @return el número elegido
     */
    public int pedirOpcion(String mensaje, Collection<Integer> validas) {
        while (true) {
            String texto = leer(mensaje).trim();
            try {
                int numero = Integer.parseInt(texto);
                if (validas.contains(numero)) {
                    return numero;
                }
            } catch (NumberFormatException e) {
                // El texto no es un número: se informa abajo y se vuelve a pedir.
            }
            System.out.println("  Opción no válida.");
        }
    }

    /**
     * Pide una fecha en formato {@code AAAA-MM-DD}. Si el usuario pulsa Enter y hay un valor
     * por defecto, se devuelve ese valor.
     *
     * @param mensaje texto que se muestra antes de leer
     * @param valorPorDefecto fecha que se usa al pulsar Enter, o {@code null} si no hay
     * @return la fecha introducida o el valor por defecto
     */
    public LocalDate pedirFecha(String mensaje, LocalDate valorPorDefecto) {
        while (true) {
            String texto = leer(mensaje).trim();
            if (texto.isEmpty() && valorPorDefecto != null) {
                return valorPorDefecto;
            }
            // Técnica 7 - Replace Exception with Test: antes se intentaba LocalDate.parse()
            // y se capturaba DateTimeParseException para saber si la fecha era válida, es
            // decir, se usaba una excepción como control de flujo. Ahora se pregunta primero
            // con esFechaValida() y solo se convierte el texto cuando se sabe que es correcto.
            if (esFechaValida(texto)) {
                return LocalDate.parse(texto);
            }
            System.out.println("  Formato inválido. Usa AAAA-MM-DD.");
        }
    }

    /**
     * Técnica 7 - Replace Exception with Test: prueba que sustituye a la captura de la
     * excepción. Acepta exactamente los mismos textos que {@link LocalDate#parse(CharSequence)}:
     * el formato {@code AAAA-MM-DD}, un mes entre 1 y 12 y un día que exista en ese mes
     * (por ejemplo, rechaza 2026-02-29 y 2026-04-31).
     *
     * @param texto texto introducido por el usuario
     * @return {@code true} si el texto es una fecha válida
     */
    static boolean esFechaValida(String texto) {
        Matcher partes = FORMATO_FECHA.matcher(texto);
        if (!partes.matches()) {
            return false;
        }
        long anio = Long.parseLong(partes.group(1));
        int mes = Integer.parseInt(partes.group(2));
        int dia = Integer.parseInt(partes.group(3));
        boolean anioEnRango = anio >= Year.MIN_VALUE && anio <= Year.MAX_VALUE;
        boolean mesEnRango = mes >= 1 && mes <= 12;
        return anioEnRango && mesEnRango
                && dia >= 1 && dia <= YearMonth.of((int) anio, mes).lengthOfMonth();
    }

    private String leer(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine();
    }
}
