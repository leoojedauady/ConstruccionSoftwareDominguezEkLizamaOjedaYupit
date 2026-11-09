package gestor.prestamos.modelo;

/**
 * Representa una herramienta que puede prestarse. Una herramienta puede darse de baja:
 * en ese caso deja de estar disponible para préstamos, pero se conserva su historial.
 * Dos herramientas son iguales si tienen el mismo identificador.
 */
public final class Herramienta {

    private final int id;
    private final String nombre;
    private boolean dadaDeBaja;

    /**
     * Crea una herramienta activa, es decir, que no está dada de baja.
     *
     * @param id identificador único, mayor que cero
     * @param nombre nombre descriptivo; no puede ser nulo ni estar vacío
     * @throws IllegalArgumentException si el id no es positivo o el nombre no es válido
     */
    public Herramienta(int id, String nombre) {
        this(id, nombre, false);
    }

    /**
     * Crea una herramienta indicando su estado de baja. Se usa al restaurar datos guardados.
     *
     * @param id identificador único, mayor que cero
     * @param nombre nombre descriptivo; no puede ser nulo ni estar vacío
     * @param dadaDeBaja {@code true} si la herramienta ya está dada de baja
     * @throws IllegalArgumentException si el id no es positivo o el nombre no es válido
     */
    public Herramienta(int id, String nombre, boolean dadaDeBaja) {
        if (id <= 0) {
            throw new IllegalArgumentException("El id de la herramienta debe ser positivo");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la herramienta no puede estar vacío");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.dadaDeBaja = dadaDeBaja;
    }

    /**
     * Devuelve el identificador de la herramienta.
     *
     * @return el id
     */
    public int getId() {
        return id;
    }

    /**
     * Devuelve el nombre de la herramienta.
     *
     * @return el nombre, sin espacios sobrantes
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Indica si la herramienta está dada de baja.
     *
     * @return {@code true} si está dada de baja
     */
    public boolean isDadaDeBaja() {
        return dadaDeBaja;
    }

    /**
     * Marca la herramienta como dada de baja. La validación de si puede darse de baja
     * corresponde al servicio, no a este método.
     */
    public void darDeBaja() {
        this.dadaDeBaja = true;
    }

    /**
     * Reactiva la herramienta, dejándola disponible para préstamos. La validación
     * corresponde al servicio. El historial de préstamos no se modifica.
     */
    public void reactivar() {
        this.dadaDeBaja = false;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Herramienta otra && otra.id == id);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
