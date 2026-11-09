package gestor.prestamos.persistencia;

import gestor.prestamos.modelo.Herramienta;
import gestor.prestamos.modelo.Prestamo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Repositorio que guarda y recupera herramientas y préstamos en los archivos
 * {@code herramientas.csv} y {@code prestamos.csv} de una carpeta.
 */
public final class RepositorioCsv {

    /** Nombre del archivo de herramientas. */
    static final String ARCHIVO_HERRAMIENTAS = "herramientas.csv";

    /** Nombre del archivo de préstamos. */
    static final String ARCHIVO_PRESTAMOS = "prestamos.csv";

    private static final String CABECERA_HERRAMIENTAS = "id,nombre,dada_de_baja";
    private static final String CABECERA_PRESTAMOS =
            "id,herramienta_id,prestatario,fecha_prestamo,fecha_limite,fecha_devolucion,dias_retraso,penalizacion";
    private static final int CAMPOS_HERRAMIENTA = 2;
    private static final int CAMPOS_PRESTAMO = 8;

    private final Path directorio;

    /**
     * Crea un repositorio que trabaja en la carpeta indicada.
     *
     * @param directorio carpeta donde están o se crearán los archivos CSV; no puede ser nula
     */
    public RepositorioCsv(Path directorio) {
        this.directorio = Objects.requireNonNull(directorio, "directorio");
    }

    /**
     * Lee las herramientas del archivo {@code herramientas.csv}. Si el archivo no existe,
     * devuelve una lista vacía.
     *
     * @return herramientas leídas
     * @throws IOException si el archivo no se puede leer o alguna línea no es válida
     */
    public List<Herramienta> cargarHerramientas() throws IOException {
        List<String> lineas = leerLineas(ARCHIVO_HERRAMIENTAS);
        List<Herramienta> herramientas = new ArrayList<>();
        for (int i = 1; i < lineas.size(); i++) {
            if (!lineas.get(i).isBlank()) {
                herramientas.add(parsearHerramienta(lineas.get(i), i + 1));
            }
        }
        return herramientas;
    }

    /**
     * Lee los préstamos del archivo {@code prestamos.csv}, asociándolos a las herramientas indicadas.
     * Si el archivo no existe, devuelve una lista vacía.
     *
     * @param herramientas herramientas existentes, usadas para resolver cada préstamo
     * @return préstamos leídos
     * @throws IOException si el archivo no se puede leer, alguna línea no es válida
     *                     o un préstamo hace referencia a una herramienta inexistente
     */
    public List<Prestamo> cargarPrestamos(List<Herramienta> herramientas) throws IOException {
        Map<Integer, Herramienta> porId = new HashMap<>();
        for (Herramienta herramienta : herramientas) {
            porId.put(herramienta.getId(), herramienta);
        }
        List<String> lineas = leerLineas(ARCHIVO_PRESTAMOS);
        List<Prestamo> prestamos = new ArrayList<>();
        for (int i = 1; i < lineas.size(); i++) {
            if (!lineas.get(i).isBlank()) {
                prestamos.add(parsearPrestamo(lineas.get(i), i + 1, porId));
            }
        }
        return prestamos;
    }

    /**
     * Guarda las herramientas en {@code herramientas.csv}, sustituyendo el contenido anterior.
     *
     * @param herramientas herramientas a guardar
     * @throws IOException si no se puede escribir el archivo
     */
    public void guardarHerramientas(List<Herramienta> herramientas) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add(CABECERA_HERRAMIENTAS);
        for (Herramienta herramienta : herramientas) {
            lineas.add(herramienta.getId() + "," + CsvUtil.escapar(herramienta.getNombre()) + "," + herramienta.isDadaDeBaja());
        }
        escribir(ARCHIVO_HERRAMIENTAS, lineas);
    }

    /**
     * Guarda los préstamos en {@code prestamos.csv}, sustituyendo el contenido anterior.
     *
     * @param prestamos préstamos a guardar
     * @throws IOException si no se puede escribir el archivo
     */
    public void guardarPrestamos(List<Prestamo> prestamos) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add(CABECERA_PRESTAMOS);
        for (Prestamo p : prestamos) {
            String devolucion = p.getFechaDevolucion() == null ? "" : p.getFechaDevolucion().toString();
            lineas.add(p.getId() + ","
                    + p.getHerramienta().getId() + ","
                    + CsvUtil.escapar(p.getPrestatario()) + ","
                    + p.getFechaPrestamo() + ","
                    + p.getFechaLimite() + ","
                    + devolucion + ","
                    + p.getDiasRetraso() + ","
                    + p.getPenalizacion());
        }
        escribir(ARCHIVO_PRESTAMOS, lineas);
    }

    private Herramienta parsearHerramienta(String linea, int numeroLinea) throws IOException {
        List<String> campos = CsvUtil.dividir(linea);
        if (campos.size() < CAMPOS_HERRAMIENTA || campos.size() > CAMPOS_HERRAMIENTA + 1) {
            throw error(ARCHIVO_HERRAMIENTAS, numeroLinea, "se esperaban " + CAMPOS_HERRAMIENTA + " campos");
        }
        try {
            int id = Integer.parseInt(campos.get(0).trim());
            return new Herramienta(id, campos.get(1),
                    campos.size() > CAMPOS_HERRAMIENTA && Boolean.parseBoolean(campos.get(2).trim()));
        } catch (IllegalArgumentException e) {
            throw error(ARCHIVO_HERRAMIENTAS, numeroLinea, e.getMessage());
        }
    }

    private Prestamo parsearPrestamo(String linea, int numeroLinea, Map<Integer, Herramienta> porId)
            throws IOException {
        List<String> campos = CsvUtil.dividir(linea);
        if (campos.size() != CAMPOS_PRESTAMO) {
            throw error(ARCHIVO_PRESTAMOS, numeroLinea, "se esperaban " + CAMPOS_PRESTAMO + " campos");
        }
        try {
            int id = Integer.parseInt(campos.get(0).trim());
            int herramientaId = Integer.parseInt(campos.get(1).trim());
            Herramienta herramienta = porId.get(herramientaId);
            if (herramienta == null) {
                throw error(ARCHIVO_PRESTAMOS, numeroLinea, "no existe la herramienta " + herramientaId);
            }
            LocalDate inicio = LocalDate.parse(campos.get(3).trim());
            LocalDate limite = LocalDate.parse(campos.get(4).trim());
            String textoDevolucion = campos.get(5).trim();
            LocalDate devolucion = textoDevolucion.isEmpty() ? null : LocalDate.parse(textoDevolucion);
            int diasRetraso = Integer.parseInt(campos.get(6).trim());
            double penalizacion = Double.parseDouble(campos.get(7).trim());
            return new Prestamo(id, herramienta, campos.get(2), inicio, limite, devolucion, diasRetraso, penalizacion);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw error(ARCHIVO_PRESTAMOS, numeroLinea, e.getMessage());
        }
    }

    private List<String> leerLineas(String nombreArchivo) throws IOException {
        Path ruta = directorio.resolve(nombreArchivo);
        if (!Files.exists(ruta)) {
            return List.of();
        }
        return Files.readAllLines(ruta, StandardCharsets.UTF_8);
    }

    private void escribir(String nombreArchivo, List<String> lineas) throws IOException {
        Files.createDirectories(directorio);
        Files.write(directorio.resolve(nombreArchivo), lineas, StandardCharsets.UTF_8);
    }

    private static IOException error(String archivo, int numeroLinea, String detalle) {
        return new IOException("Línea " + numeroLinea + " de " + archivo + " no válida: " + detalle);
    }
}
