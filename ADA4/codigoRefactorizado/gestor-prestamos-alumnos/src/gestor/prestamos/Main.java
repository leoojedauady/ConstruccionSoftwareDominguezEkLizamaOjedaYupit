package gestor.prestamos;

import gestor.prestamos.consola.Entrada;
import gestor.prestamos.consola.MenuConsola;
import gestor.prestamos.modelo.Herramienta;
import gestor.prestamos.modelo.Prestamo;
import gestor.prestamos.persistencia.RepositorioCsv;
import gestor.prestamos.servicio.GestorPrestamos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Clase principal de la aplicación. Carga los datos desde los archivos CSV,
 * crea el servicio de negocio y arranca el menú de consola.
 */
public final class Main {

    /**
     * Constructor privado: la clase solo expone el método {@code main}.
     */
    private Main() {
    }

    /**
     * Arranca la aplicación.
     *
     * @param args argumento opcional con la carpeta donde están los archivos CSV;
     *             si no se indica, se usa la carpeta actual
     */
    public static void main(String[] args) {
        Path directorio = Paths.get(args.length > 0 ? args[0] : ".");
        RepositorioCsv repositorio = new RepositorioCsv(directorio);

        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            List<Herramienta> herramientas = repositorio.cargarHerramientas();
            List<Prestamo> prestamos = repositorio.cargarPrestamos(herramientas);
            GestorPrestamos gestor = new GestorPrestamos(herramientas, prestamos);
            new MenuConsola(gestor, repositorio, new Entrada(scanner)).ejecutar();
        } catch (IOException e) {
            System.err.println("Error al leer los datos: " + e.getMessage());
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada.");
        }
    }
}
