package gestor.prestamos.servicio;

import gestor.prestamos.modelo.Herramienta;
import gestor.prestamos.modelo.Prestamo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio que aplica las reglas de negocio del sistema. Mantiene las listas de
 * herramientas y préstamos en memoria; la persistencia se delega en otra capa.
 */
public final class GestorPrestamos {

    /** Días de préstamo que se proponen por defecto. */
    public static final int DIAS_PRESTAMO_DEFECTO = 7;

    /** Penalización en euros por cada día de retraso. */
    public static final double PENALIZACION_POR_DIA = 2.0;

    private final List<Herramienta> herramientas;
    private final List<Prestamo> prestamos;

    /**
     * Crea el servicio con los datos ya cargados. Las listas recibidas se copian.
     *
     * @param herramientas herramientas existentes; no puede ser nula
     * @param prestamos préstamos existentes, activos y cerrados; no puede ser nula
     */
    public GestorPrestamos(List<Herramienta> herramientas, List<Prestamo> prestamos) {
        this.herramientas = new ArrayList<>(Objects.requireNonNull(herramientas, "herramientas"));
        this.prestamos = new ArrayList<>(Objects.requireNonNull(prestamos, "prestamos"));
    }

    /**
     * Devuelve todas las herramientas registradas.
     *
     * @return vista de solo lectura de las herramientas
     */
    public List<Herramienta> getHerramientas() {
        return Collections.unmodifiableList(herramientas);
    }

    /**
     * Devuelve todos los préstamos, activos y cerrados.
     *
     * @return vista de solo lectura de los préstamos
     */
    public List<Prestamo> getPrestamos() {
        return Collections.unmodifiableList(prestamos);
    }

    /**
     * Registra una nueva herramienta con el siguiente identificador disponible.
     *
     * @param nombre nombre de la herramienta
     * @return la herramienta creada
     * @throws IllegalArgumentException si el nombre no es válido
     */
    public Herramienta anadirHerramienta(String nombre) {
        int siguienteId = herramientas.stream().mapToInt(Herramienta::getId).max().orElse(0) + 1;
        Herramienta nueva = new Herramienta(siguienteId, nombre);
        herramientas.add(nueva);
        return nueva;
    }

    /**
     * Da de baja una herramienta. No se borra: deja de estar disponible para préstamos,
     * pero su historial se conserva.
     *
     * @param herramientaId identificador de la herramienta
     * @throws IllegalArgumentException si la herramienta no existe
     * @throws IllegalStateException si ya estaba dada de baja o si está prestada
     */
    public void darDeBaja(int herramientaId) {
        Herramienta herramienta = buscarHerramienta(herramientaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe ninguna herramienta con ID " + herramientaId));
        if (herramienta.isDadaDeBaja()) {
            throw new IllegalStateException("La herramienta '" + herramienta.getNombre() + "' ya está dada de baja");
        }
        if (prestamoActivo(herramienta).isPresent()) {
            throw new IllegalStateException("No se puede dar de baja: la herramienta está prestada");
        }
        herramienta.darDeBaja();
    }

    /**
     * Reactiva una herramienta dada de baja. El historial de préstamos anteriores se conserva.
     *
     * @param herramientaId identificador de la herramienta
     * @throws IllegalArgumentException si la herramienta no existe
     * @throws IllegalStateException si la herramienta no estaba dada de baja
     */
    public void reactivar(int herramientaId) {
        Herramienta herramienta = buscarHerramienta(herramientaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe ninguna herramienta con ID " + herramientaId));
        if (!herramienta.isDadaDeBaja()) {
            throw new IllegalStateException("La herramienta '" + herramienta.getNombre() + "' no está dada de baja");
        }
        herramienta.reactivar();
    }

    /**
     * Busca una herramienta por su identificador.
     *
     * @param id identificador de la herramienta
     * @return la herramienta si existe, o un Optional vacío
     */
    public Optional<Herramienta> buscarHerramienta(int id) {
        return herramientas.stream().filter(h -> h.getId() == id).findFirst();
    }

    /**
     * Busca el préstamo abierto de una herramienta, si lo tiene.
     *
     * @param herramienta herramienta a consultar
     * @return el préstamo activo, o un Optional vacío si la herramienta está disponible
     */
    public Optional<Prestamo> prestamoActivo(Herramienta herramienta) {
        return prestamos.stream()
                .filter(p -> p.estaActivo() && p.getHerramienta().equals(herramienta))
                .findFirst();
    }

    /**
     * Devuelve las herramientas que pueden prestarse en este momento.
     *
     * @return lista de herramientas sin préstamo activo
     */
    public List<Herramienta> herramientasDisponibles() {
        return herramientas.stream()
                .filter(h -> !h.isDadaDeBaja() && prestamoActivo(h).isEmpty())
                .toList();
    }

    /**
     * Registra el préstamo de una herramienta disponible.
     *
     * @param herramientaId identificador de la herramienta a prestar
     * @param prestatario persona que recibe la herramienta
     * @param fechaPrestamo fecha del préstamo
     * @param fechaLimite fecha máxima de devolución
     * @return el préstamo creado
     * @throws IllegalArgumentException si la herramienta no existe o los datos no son válidos
     * @throws IllegalStateException si la herramienta ya está prestada
     */
    public Prestamo prestar(int herramientaId, String prestatario, LocalDate fechaPrestamo, LocalDate fechaLimite) {
        Herramienta herramienta = buscarHerramienta(herramientaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe ninguna herramienta con ID " + herramientaId));
        if (herramienta.isDadaDeBaja()) {
            throw new IllegalStateException("La herramienta '" + herramienta.getNombre() + "' está dada de baja");
        }
        if (prestamoActivo(herramienta).isPresent()) {
            throw new IllegalStateException("La herramienta '" + herramienta.getNombre() + "' ya está prestada");
        }
        int siguienteId = prestamos.stream().mapToInt(Prestamo::getId).max().orElse(0) + 1;
        Prestamo nuevo = new Prestamo(siguienteId, herramienta, prestatario, fechaPrestamo, fechaLimite, null, 0, 0.0);
        prestamos.add(nuevo);
        return nuevo;
    }

    /**
     * Registra la devolución de un préstamo activo y calcula su penalización.
     *
     * @param prestamoId identificador del préstamo
     * @param fecha fecha real de devolución
     * @return el préstamo actualizado
     * @throws IllegalArgumentException si el préstamo no existe o la fecha no es válida
     * @throws IllegalStateException si el préstamo ya estaba devuelto
     */
    public Prestamo devolver(int prestamoId, LocalDate fecha) {
        Prestamo prestamo = prestamos.stream()
                .filter(p -> p.getId() == prestamoId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe el préstamo con ID " + prestamoId));
        prestamo.registrarDevolucion(fecha, PENALIZACION_POR_DIA);
        long retraso = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(prestamo.getFechaLimite(), fecha));
        prestamo.setDiasRetraso((int) retraso);
        prestamo.setPenalizacion(Math.round(retraso * PENALIZACION_POR_DIA * 100.0) / 100.0);
        return prestamo;
    }

    /**
     * Devuelve los préstamos que aún no se han devuelto.
     *
     * @return lista de préstamos activos
     */
    public List<Prestamo> prestamosActivos() {
        return prestamos.stream().filter(Prestamo::estaActivo).toList();
    }

    /**
     * Devuelve los préstamos ya devueltos.
     *
     * @return lista de préstamos cerrados
     */
    public List<Prestamo> historial() {
        return prestamos.stream().filter(p -> !p.estaActivo()).toList();
    }

    /**
     * Suma las penalizaciones cobradas en todas las devoluciones.
     *
     * @return total en euros
     */
    public double totalPenalizaciones() {
        return historial().stream().mapToDouble(Prestamo::getPenalizacion).sum();
    }
}
