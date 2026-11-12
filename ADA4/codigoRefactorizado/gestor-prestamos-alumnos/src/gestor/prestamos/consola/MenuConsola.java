package gestor.prestamos.consola;

import gestor.prestamos.modelo.EstadoPrestamo;
import gestor.prestamos.modelo.Herramienta;
import gestor.prestamos.modelo.PeriodoPrestamo;
import gestor.prestamos.modelo.Prestamo;
import gestor.prestamos.persistencia.RepositorioCsv;
import gestor.prestamos.servicio.GestorPrestamos;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final GestorPrestamos gestor;
    private final RepositorioCsv repositorio;
    private final Entrada entrada;

    /**
     * Técnica 10 - Replace Conditional with Polymorphism: cada número del menú se asocia
     * a un objeto {@link OpcionMenu}. Sustituye al switch de ejecutarOpcion(). Las claves
     * del mapa son además las opciones válidas que acepta el menú (antes, la constante
     * OPCIONES repetía esa misma lista por separado).
     */
    private final Map<Integer, OpcionMenu> opciones;

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
        this.opciones = crearOpciones();
    }

    /**
     * Técnica 10 - Replace Conditional with Polymorphism: construye la tabla de opciones.
     * Cada opción es una implementación distinta de {@link OpcionMenu}: las opciones 1 a 8
     * se implementan con referencias a los métodos de este menú y la opción 0 con
     * {@link OpcionSalir}, que redefine {@link OpcionMenu#terminaElPrograma()}.
     * Para añadir una opción nueva basta con registrarla aquí, sin tocar ningún switch.
     *
     * @return opciones del menú indexadas por su número
     */
    private Map<Integer, OpcionMenu> crearOpciones() {
        Map<Integer, OpcionMenu> mapa = new LinkedHashMap<>();
        mapa.put(1, this::listarHerramientas);
        mapa.put(2, this::anadirHerramienta);
        mapa.put(3, this::prestar);
        mapa.put(4, this::devolver);
        mapa.put(5, this::verPrestamosActivos);
        mapa.put(6, this::verHistorial);
        mapa.put(7, this::darDeBaja);
        mapa.put(8, this::reactivar);
        mapa.put(0, new OpcionSalir());
        return Collections.unmodifiableMap(mapa);
    }

    /**
     * Muestra el menú en bucle hasta que el usuario elige salir. Los errores de
     * negocio se muestran sin cerrar la aplicación.
     */
    public void ejecutar() {
        boolean salir = false;
        while (!salir) {
            System.out.println(MENU);
            int opcion = entrada.pedirOpcion("Elige una opción: ", opciones.keySet());
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
     * <p><b>Técnica 10 - Replace Conditional with Polymorphism.</b> Antes este método
     * tenía un switch que decidía qué hacer según el número de opción. Ahora se busca el
     * objeto {@link OpcionMenu} correspondiente y se le pide que se ejecute; cada opción
     * decide su comportamiento (y si termina el programa) por polimorfismo.</p>
     *
     * @param opcion número de opción del menú
     * @return {@code true} si el usuario quiere salir de la aplicación
     */
    private boolean ejecutarOpcion(int opcion) {
        OpcionMenu elegida = opciones.get(opcion);
        elegida.ejecutar();
        return elegida.terminaElPrograma();
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

    /**
     * Técnica 1 - Extract Method: el método original hacía todo en un solo bloque largo
     * (mostrar y elegir la herramienta, pedir las fechas y registrar el préstamo). Ahora
     * solo coordina los pasos; cada paso está en su propio método con un nombre que
     * explica qué hace. Las entradas se piden en el mismo orden que antes.
     */
    private void prestar() {
        List<Herramienta> disponibles = gestor.herramientasDisponibles();
        if (disponibles.isEmpty()) {
            System.out.println("\n  No hay herramientas disponibles para prestar.");
            return;
        }
        int herramientaId = seleccionarHerramientaDisponible(disponibles);
        String prestatario = entrada.pedirTexto("Nombre de quien la recibe: ");
        PeriodoPrestamo periodo = pedirPeriodoPrestamo();
        registrarPrestamo(herramientaId, prestatario, periodo);
    }

    /**
     * Técnica 1 - Extract Method: selección de la herramienta, extraída de prestar().
     * Muestra las herramientas disponibles y pide el ID de una de ellas.
     *
     * @param disponibles herramientas que pueden prestarse
     * @return ID de la herramienta elegida
     */
    private int seleccionarHerramientaDisponible(List<Herramienta> disponibles) {
        System.out.println("\n  Herramientas disponibles:");
        for (Herramienta herramienta : disponibles) {
            System.out.printf("    %d. %s%n", herramienta.getId(), herramienta.getNombre());
        }
        List<Integer> ids = disponibles.stream().map(Herramienta::getId).toList();
        return entrada.pedirOpcion("ID de la herramienta: ", ids);
    }

    /**
     * Técnica 1 - Extract Method: petición de fechas, extraída de prestar().
     * Pide la fecha de préstamo (por defecto hoy) y la fecha límite (por defecto,
     * la de préstamo más los días de préstamo por defecto).
     *
     * <p>Técnica 8 - Introduce Parameter Object: devuelve las dos fechas juntas
     * en un {@link PeriodoPrestamo}.</p>
     *
     * @return periodo con las fechas introducidas
     */
    private PeriodoPrestamo pedirPeriodoPrestamo() {
        LocalDate hoy = fechaActual();
        LocalDate inicio = entrada.pedirFecha("Fecha de préstamo (AAAA-MM-DD, Enter = " + hoy + "): ", hoy);
        LocalDate limitePorDefecto = inicio.plusDays(GestorPrestamos.DIAS_PRESTAMO_DEFECTO);
        LocalDate limite = entrada.pedirFecha(
                "Fecha límite (AAAA-MM-DD, Enter = " + limitePorDefecto + "): ", limitePorDefecto);
        return new PeriodoPrestamo(inicio, limite);
    }

    /**
     * Técnica 1 - Extract Method: registro del préstamo, extraído de prestar().
     * Crea el préstamo en el servicio, guarda los datos y muestra la confirmación.
     *
     * @param herramientaId ID de la herramienta a prestar
     * @param prestatario persona que recibe la herramienta
     * @param periodo fechas de préstamo y límite
     */
    private void registrarPrestamo(int herramientaId, String prestatario, PeriodoPrestamo periodo) {
        Prestamo nuevo = gestor.prestar(herramientaId, prestatario, periodo);
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

        LocalDate hoy = fechaActual();
        LocalDate fecha = entrada.pedirFecha("Fecha de devolución (AAAA-MM-DD, Enter = " + hoy + "): ", hoy);

        // Técnica 5 - Separate Query from Modifier: primero se modifica (devolver) y
        // después se consulta el préstamo ya actualizado (buscarPrestamo).
        gestor.devolver(id, fecha);
        Prestamo devuelto = gestor.buscarPrestamo(id);
        guardar();
        System.out.println("  Devolución registrada.");
        if (devuelto.getDiasRetraso() > 0) {
            System.out.printf("  Retraso de %d día(s). Penalización: %.2f €%n",
                    devuelto.getDiasRetraso(), devuelto.getPenalizacion());
        } else {
            System.out.println("  Devuelta a tiempo, sin penalización.");
        }
    }

    /**
     * Muestra los préstamos activos y su situación respecto a la fecha actual.
     *
     * <p><b>Técnica 2 - Replace Temp with Query.</b> La variable temporal {@code hoy}
     * ({@code LocalDate hoy = LocalDate.now()}) se sustituye por el método de consulta
     * {@link #fechaActual()}, que pueden usar también los métodos extraídos.</p>
     *
     * <p><b>Técnica 9 - Decompose Conditional.</b> El if/else que estaba dentro del bucle
     * mezclaba el cálculo del retraso con el texto que se muestra. Se descompone en
     * {@link #describirSituacion}, {@link #estaAtrasado}, {@link #describirAtraso} y
     * {@link #describirAlDia}.</p>
     */
    private void verPrestamosActivos() {
        List<Prestamo> activos = gestor.prestamosActivos();
        if (activos.isEmpty()) {
            System.out.println("\n  No hay préstamos activos.");
            return;
        }
        System.out.println("\n  Préstamos activos:");
        for (Prestamo p : activos) {
            System.out.printf("    %s - %s | desde %s | %s%n",
                    p.getHerramienta().getNombre(), p.getPrestatario(), p.getFechaPrestamo(),
                    describirSituacion(p));
        }
    }

    /**
     * Técnica 2 - Replace Temp with Query: consulta que sustituye a la variable
     * temporal {@code hoy}. Devuelve la fecha del sistema.
     *
     * @return la fecha de hoy
     */
    private LocalDate fechaActual() {
        return LocalDate.now();
    }

    /**
     * Técnica 9 - Decompose Conditional: el condicional original queda reducido a
     * «si está atrasado, describe el atraso; si no, describe que está al día».
     *
     * @param prestamo préstamo activo
     * @return texto con la situación del préstamo
     */
    private String describirSituacion(Prestamo prestamo) {
        if (estaAtrasado(prestamo)) {
            return describirAtraso(prestamo);
        }
        return describirAlDia(prestamo);
    }

    /**
     * Técnica 9 - Decompose Conditional: la condición, extraída con un nombre propio.
     *
     * @param prestamo préstamo activo
     * @return {@code true} si la fecha límite ya pasó
     */
    private boolean estaAtrasado(Prestamo prestamo) {
        return prestamo.estado(fechaActual()) == EstadoPrestamo.ATRASADO;
    }

    /**
     * Técnica 9 - Decompose Conditional: rama «atrasado». Calcula los días de retraso
     * y la penalización estimada a fecha de hoy y los presenta como texto.
     *
     * @param prestamo préstamo activo y atrasado
     * @return texto con los días de retraso y la penalización estimada
     */
    private String describirAtraso(Prestamo prestamo) {
        long dias = prestamo.getDiasRetrasoA(fechaActual());
        double estimada = prestamo.calcularPenalizacion(fechaActual(), Prestamo.PENALIZACION_POR_DIA);
        return String.format("ATRASADO %d día(s), penalización estimada %.2f €", dias, estimada);
    }

    /**
     * Técnica 9 - Decompose Conditional: rama «al día».
     *
     * @param prestamo préstamo activo dentro de plazo
     * @return texto con la fecha de vencimiento
     */
    private String describirAlDia(Prestamo prestamo) {
        return "Al día, vence " + prestamo.getFechaLimite();
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
