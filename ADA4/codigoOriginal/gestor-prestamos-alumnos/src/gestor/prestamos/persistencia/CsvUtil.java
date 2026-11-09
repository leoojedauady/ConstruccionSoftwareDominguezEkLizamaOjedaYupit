package gestor.prestamos.persistencia;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades para generar y leer líneas CSV. Los campos que contienen comas,
 * comillas o saltos de línea se encierran entre comillas dobles, y las comillas
 * internas se duplican.
 */
public final class CsvUtil {

    /**
     * Constructor privado: la clase solo contiene métodos estáticos.
     */
    private CsvUtil() {
    }

    /**
     * Prepara un valor para escribirlo en un campo CSV.
     *
     * @param valor texto a escribir; si es nulo se devuelve una cadena vacía
     * @return el valor, entrecomillado si hace falta
     */
    public static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        boolean necesitaComillas = valor.contains(",") || valor.contains("\"") || valor.contains("\n");
        if (!necesitaComillas) {
            return valor;
        }
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    /**
     * Divide una línea CSV en sus campos, respetando los campos entrecomillados.
     *
     * @param linea línea a dividir
     * @return lista de campos, sin las comillas que los envuelven
     */
    public static List<String> dividir(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean dentroDeComillas = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (dentroDeComillas) {
                if (c == '"') {
                    if (i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                        actual.append('"');
                        i++;
                    } else {
                        dentroDeComillas = false;
                    }
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                dentroDeComillas = true;
            } else if (c == ',') {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }
}
