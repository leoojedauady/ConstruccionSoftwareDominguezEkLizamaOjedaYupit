package gestor.prestamos.consola;

/**
 * Opción 0 del menú: salir de la aplicación.
 *
 * <p><b>Técnica 10 - Replace Conditional with Polymorphism.</b> En el código original
 * la salida era la rama {@code default -> return true} del {@code switch}. Ahora es una
 * implementación de {@link OpcionMenu} que redefine {@link #terminaElPrograma()}.</p>
 */
final class OpcionSalir implements OpcionMenu {

    /**
     * No realiza ninguna acción: el mensaje de despedida lo muestra el menú al terminar.
     */
    @Override
    public void ejecutar() {
        // Salir no tiene acción propia.
    }

    /**
     * La opción de salir es la única que termina el programa.
     *
     * @return siempre {@code true}
     */
    @Override
    public boolean terminaElPrograma() {
        return true;
    }
}
