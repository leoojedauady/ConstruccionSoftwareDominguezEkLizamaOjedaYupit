package gestor.prestamos.consola;

import gestor.prestamos.modelo.EstadoPrestamo;
import gestor.prestamos.modelo.Herramienta;
import gestor.prestamos.modelo.Prestamo;
import gestor.prestamos.persistencia.RepositorioCsv;
import gestor.prestamos.servicio.GestorPrestamos;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Menú principal de la aplicación. Muestra las opciones, pide los datos necesarios
 * y delega las operaciones en el servicio. Tras cada cambio, guarda los datos.
 */
public final class MenuConsola {

    private static final String MENU = """

            ========== GESTOR DE PRÉSTAMOS ==========
              1. Ver herramientas y disponibilidad
              2. Añadir herramienta
              3. Prestar herramienta
              4. Registrar devolución
              5. Ver préstamos activos
              6. Ver historial y penalizaciones
              7. Dar de baja una herramienta
              8. Reactivar herramienta
              0. Salir
            =========================================
            """;

    private static final List<Integer> OPCIONES = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8);

    private final GestorPrestamos gestor;
    private final RepositorioCsv repositorio;
    private final Entrada entrada;

    /**
     * Crea el menú con sus dependencias.
     *
     * @param gestor servicio de préstamos
     * @param repositorio repositorio donde se guardan los datos
     * @param entrada lectura de datos desde la consola
     */
    public MenuConsola(GestorPrestamos gestor, RepositorioCsv repositorio, Entrada entrada) {
        this.gestor = gestor;
        this.repositorio = repositorio;
        this.entrada = entrada;
    }

    /**
     * Muestra el menú en bucle hasta que el usuario elige salir. Los errores de
     * negocio se muestran sin cerrar la aplicación.
     */
    public void ejecutar() {
        boolean salir = false;
        while (!salir) {
            System.out.println(MENU);
            int opcion = entrada.pedirOpcion("Elige una opción: ", OPCIONES);
            try {
                salir = ejecutarOpcion(opcion);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("  Error: " + e.getMessage());
            }
        }
        System.out.println("¡Hasta luego!");
    }

    /**
     * Ejecuta la opción elegida.
     *
     * @param opcion número de opción del menú
     * @return {@code true} si el usuario quiere salir de la aplicación
     */
    private boolean ejecutarOpcion(int opcion) {
        switch (opcion) {
            case 1 -> listarHerramientas();
            case 2 -> anadirHerramienta();
            case 3 -> prestar();
            case 4 -> devolver();
            case 5 -> verPrestamosActivos();
            case 6 -> verHistorial();
            case 7 -> darDeBaja();
            case 8 -> reactivar();
            default -> {
                return true;
            }
        }
        return false;
    }

    private void listarHerramientas() {
        List<Herramienta> herramientas = gestor.getHerramientas();
        if (herramientas.isEmpty()) {
            System.out.println("\n  No hay herramientas registradas.");
            return;
        }
        System.out.println("\n  ID  Herramienta             Estado");
        System.out.println("  " + "-".repeat(60));
        for (Herramienta herramienta : herramientas) {
            Optional<Prestamo> activo = gestor.prestamoActivo(herramienta);
            String estado = activo
                    .map(p -> "Prestada a " + p.getPrestatario() + " (hasta " + p.getFechaLimite() + ")")
                    .orElse(herramienta.isDadaDeBaja() ? "Dada de baja" : "Disponible");
            System.out.printf("  %-4d%-24s%s%n", herramienta.getId(), herramienta.getNombre(), estado);
        }
    }

    private void darDeBaja() {
        List<Herramienta> candidatas = gestor.getHerramientas().stream()
                .filter(h -> !h.isDadaDeBaja())
                .toList();
        if (candidatas.isEmpty()) {
            System.out.println("\n  No hay herramientas que dar de baja.");
            return;
        }
        System.out.println("\n  Herramientas en uso:");
        for (Herramienta herramienta : candidatas) {
            System.out.printf("    %d. %s%n", herramienta.getId(), herramienta.getNombre());
        }
        List<Integer> ids = candidatas.stream().map(Herramienta::getId).toList();
        int id = entrada.pedirOpcion("ID de la herramienta a dar de baja: ", ids);
        Herramienta elegida = candidatas.stream()
                .filter(h -> h.getId() == id)
                .findFirst()
                .orElseThrow();
        gestor.darDeBaja(id);
        guardar();
        System.out.printf("  '%s' dada de baja. Su historial se conserva.%n", elegida.getNombre());
    }

    private void reactivar() {
        List<Herramienta> dadas = gestor.getHerramientas().stream()
                .filter(Herramienta::isDadaDeBaja)
                .toList();
        if (dadas.isEmpty()) {
            System.out.println("\n  No hay herramientas dadas de baja.");
            return;
        }
        System.out.println("\n  Herramientas dadas de baja:");
        for (Herramienta herramienta : dadas) {
            System.out.printf("    %d. %s%n", herramienta.getId(), herramienta.getNombre());
        }
        List<Integer> ids = dadas.stream().map(Herramienta::getId).toList();
        int id = entrada.pedirOpcion("ID de la herramienta a reactivar: ", ids);
        Herramienta elegida = dadas.stream()
                .filter(h -> h.getId() == id)
                .findFirst()
                .orElseThrow();
        gestor.reactivar(id);
        guardar();
        System.out.printf("  '%s' reactivada y disponible para préstamo.%n", elegida.getNombre());
    }

    private void anadirHerramienta() {
        String nombre = entrada.pedirTexto("Nombre de la herramienta: ");
        Herramienta nueva = gestor.anadirHerramienta(nombre);
        guardar();
        System.out.printf("  Herramienta '%s' añadida con ID %d.%n", nueva.getNombre(), nueva.getId());
    }

    private void prestar() {
        List<Herramienta> disponibles = gestor.herramientasDisponibles();
        if (disponibles.isEmpty()) {
            System.out.println("\n  No hay herramientas disponibles para prestar.");
            return;
        }
        System.out.println("\n  Herramientas disponibles:");
        for (Herramienta herramienta : disponibles) {
            System.out.printf("    %d. %s%n", herramienta.getId(), herramienta.getNombre());
        }
        List<Integer> ids = disponibles.stream().map(Herramienta::getId).toList();
        int id = entrada.pedirOpcion("ID de la herramienta: ", ids);
        String prestatario = entrada.pedirTexto("Nombre de quien la recibe: ");

        LocalDate hoy = LocalDate.now();
        LocalDate inicio = entrada.pedirFecha("Fecha de préstamo (AAAA-MM-DD, Enter = " + hoy + "): ", hoy);
        LocalDate limitePorDefecto = inicio.plusDays(GestorPrestamos.DIAS_PRESTAMO_DEFECTO);
        LocalDate limite = entrada.pedirFecha(
                "Fecha límite (AAAA-MM-DD, Enter = " + limitePorDefecto + "): ", limitePorDefecto);

        Prestamo nuevo = gestor.prestar(id, prestatario, inicio, limite);
        guardar();
        System.out.printf("  '%s' prestada a %s hasta %s.%n",
                nuevo.getHerramienta().getNombre(), nuevo.getPrestatario(), nuevo.getFechaLimite());
    }

    private void devolver() {
        List<Prestamo> activos = gestor.prestamosActivos();
        if (activos.isEmpty()) {
            System.out.println("\n  No hay préstamos pendientes de devolución.");
            return;
        }
        System.out.println("\n  Préstamos activos:");
        for (Prestamo p : activos) {
            System.out.printf("    %d. %s - %s (límite %s)%n",
                    p.getId(), p.getHerramienta().getNombre(), p.getPrestatario(), p.getFechaLimite());
        }
        List<Integer> ids = activos.stream().map(Prestamo::getId).toList();
        int id = entrada.pedirOpcion("ID del préstamo a devolver: ", ids);

        LocalDate hoy = LocalDate.now();
        LocalDate fecha = entrada.pedirFecha("Fecha de devolución (AAAA-MM-DD, Enter = " + hoy + "): ", hoy);

        Prestamo devuelto = gestor.devolver(id, fecha);
        guardar();
        System.out.println("  Devolución registrada.");
        if (devuelto.getDiasRetraso() > 0) {
            System.out.printf("  Retraso de %d día(s). Penalización: %.2f €%n",
                    devuelto.getDiasRetraso(), devuelto.getPenalizacion());
        } else {
            System.out.println("  Devuelta a tiempo, sin penalización.");
        }
    }

    private void verPrestamosActivos() {
        List<Prestamo> activos = gestor.prestamosActivos();
        if (activos.isEmpty()) {
            System.out.println("\n  No hay préstamos activos.");
            return;
        }
        LocalDate hoy = LocalDate.now();
        System.out.println("\n  Préstamos activos:");
        for (Prestamo p : activos) {
            String situacion;
            if (p.estado(hoy) == EstadoPrestamo.ATRASADO) {
                long dias = p.getDiasRetrasoA(hoy);
                double estimada = p.calcularPenalizacion(hoy, GestorPrestamos.PENALIZACION_POR_DIA);
                situacion = String.format("ATRASADO %d día(s), penalización estimada %.2f €", dias, estimada);
            } else {
                situacion = "Al día, vence " + p.getFechaLimite();
            }
            System.out.printf("    %s - %s | desde %s | %s%n",
                    p.getHerramienta().getNombre(), p.getPrestatario(), p.getFechaPrestamo(), situacion);
        }
    }

    private void verHistorial() {
        List<Prestamo> historial = gestor.historial();
        if (historial.isEmpty()) {
            System.out.println("\n  No hay devoluciones registradas.");
            return;
        }
        System.out.println("\n  Historial de devoluciones:");
        for (Prestamo p : historial) {
            System.out.printf("    %s - %s | %s -> %s | retraso %d día(s) | penalización %.2f €%n",
                    p.getHerramienta().getNombre(), p.getPrestatario(), p.getFechaPrestamo(),
                    p.getFechaDevolucion(), p.getDiasRetraso(), p.getPenalizacion());
        }
        System.out.printf("  Total de penalizaciones cobradas: %.2f €%n", gestor.totalPenalizaciones());
    }

    private void guardar() {
        try {
            repositorio.guardarHerramientas(gestor.getHerramientas());
            repositorio.guardarPrestamos(gestor.getPrestamos());
        } catch (IOException e) {
            System.out.println("  Aviso: no se pudieron guardar los datos: " + e.getMessage());
        }
    }
}
