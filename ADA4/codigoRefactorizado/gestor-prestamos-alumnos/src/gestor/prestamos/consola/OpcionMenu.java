package gestor.prestamos.consola;

/**
 * Una opción del menú principal.
 *
 * <p><b>Técnica 10 - Replace Conditional with Polymorphism.</b> Sustituye al
 * {@code switch} del método {@code MenuConsola.ejecutarOpcion()}. Cada opción del menú
 * es una implementación de esta interfaz y sabe qué hacer al ejecutarse; el menú ya no
 * necesita preguntar qué número se eligió para decidir el comportamiento.</p>
 */
@FunctionalInterface
public interface OpcionMenu {

    /**
     * Realiza la acción de la opción.
     */
    void ejecutar();

    /**
     * Indica si, tras ejecutar la opción, la aplicación debe terminar.
     * Por defecto, ninguna opción termina el programa.
     *
     * @return {@code true} si hay que salir del menú
     */
    default boolean terminaElPrograma() {
        return false;
    }
}
