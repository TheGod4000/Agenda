/*
 * Servicio para lógica de negocio de Direcciones
 */
package com.agenda;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

/**
 * Servicio para manejar la lógica de negocio de direcciones
 */
public class DireccionService {
    private final DireccionDAO direccionDAO;
    private final PersonaDireccionDAO personaDireccionDAO;

    public DireccionService() {
        this.direccionDAO = new DireccionDAO();
        this.personaDireccionDAO = new PersonaDireccionDAO();
    }

    public DireccionService(DireccionDAO direccionDAO, PersonaDireccionDAO personaDireccionDAO) {
        this.direccionDAO = direccionDAO;
        this.personaDireccionDAO = personaDireccionDAO;
    }

    /**
     * Crear una nueva dirección con validaciones
     */
    public boolean crearDireccion(Direccion direccion) throws IllegalArgumentException {
        validarDireccion(direccion);

        // Verificar si ya existe una dirección similar
        List<Direccion> similares = direccionDAO.buscarSimilares(direccion);
        for (Direccion similar : similares) {
            if (similar.esSimilarA(direccion)) {
                throw new IllegalArgumentException(
                        "Ya existe una dirección similar: " + similar.getDireccionCompleta());
            }
        }

        return direccionDAO.crear(direccion);
    }

    /**
     * Obtener dirección por ID
     */
    public Direccion obtenerDireccion(int id) {
        return direccionDAO.obtenerPorId(id);
    }

    /**
     * Obtener todas las direcciones
     */
    public ObservableList<Direccion> obtenerTodasDirecciones() {
        return direccionDAO.obtenerTodas();
    }

    /**
     * Actualizar dirección con validaciones
     */
    public boolean actualizarDireccion(Direccion direccion) throws IllegalArgumentException {
        validarDireccion(direccion);
        return direccionDAO.actualizar(direccion);
    }

    /**
     * Eliminar dirección (solo si no está en uso)
     */
    public boolean eliminarDireccion(int id) throws IllegalArgumentException {
        if (direccionDAO.estaEnUso(id)) {
            int cantidadPersonas = personaDireccionDAO.contarPersonasPorDireccion(id);
            throw new IllegalArgumentException(
                    "No se puede eliminar la dirección porque está siendo usada por " +
                            cantidadPersonas + " persona(s)");
        }

        return direccionDAO.eliminar(id);
    }

    /**
     * Buscar direcciones por texto
     */
    public ObservableList<Direccion> buscarDirecciones(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return obtenerTodasDirecciones();
        }
        return direccionDAO.buscarPorTexto(texto.trim());
    }

    /**
     * Asociar una dirección existente a una persona
     */
    public boolean asociarDireccionAPersona(int personaId, int direccionId, String etiqueta, boolean esPrincipal)
            throws IllegalArgumentException {

        if (personaDireccionDAO.existeRelacion(personaId, direccionId)) {
            throw new IllegalArgumentException("La persona ya tiene asociada esta dirección");
        }

        // Validar etiqueta
        if (etiqueta == null || etiqueta.trim().isEmpty()) {
            etiqueta = "Dirección";
        }

        if (etiqueta.length() > 50) {
            throw new IllegalArgumentException("La etiqueta no puede exceder 50 caracteres");
        }

        PersonaDireccion relacion = new PersonaDireccion(personaId, direccionId, etiqueta, esPrincipal);
        return personaDireccionDAO.crear(relacion);
    }

    /**
     * Crear una nueva dirección y asociarla a una persona
     */
    public boolean crearYAsociarDireccion(int personaId, Direccion direccion, String etiqueta, boolean esPrincipal)
            throws IllegalArgumentException {

        // Primero crear la dirección
        if (crearDireccion(direccion)) {
            // Luego asociarla a la persona
            return asociarDireccionAPersona(personaId, direccion.getId(), etiqueta, esPrincipal);
        }

        return false;
    }

    /**
     * Desasociar una dirección de una persona
     */
    public boolean desasociarDireccionDePersona(int personaId, int direccionId) {
        List<PersonaDireccion> relaciones = personaDireccionDAO.obtenerPorPersonaId(personaId);

        for (PersonaDireccion relacion : relaciones) {
            if (relacion.getDireccionId() == direccionId) {
                return personaDireccionDAO.eliminar(relacion.getId());
            }
        }

        return false;
    }

    /**
     * Actualizar la relación entre persona y dirección
     */
    public boolean actualizarRelacionPersonaDireccion(PersonaDireccion relacion)
            throws IllegalArgumentException {

        if (relacion.getEtiqueta() == null || relacion.getEtiqueta().trim().isEmpty()) {
            throw new IllegalArgumentException("La etiqueta no puede estar vacía");
        }

        if (relacion.getEtiqueta().length() > 50) {
            throw new IllegalArgumentException("La etiqueta no puede exceder 50 caracteres");
        }

        return personaDireccionDAO.actualizar(relacion);
    }

    /**
     * Establecer una dirección como principal para una persona
     */
    public boolean establecerDireccionPrincipal(int personaId, int direccionId) {
        return personaDireccionDAO.establecerComoPrincipal(personaId, direccionId);
    }

    /**
     * Obtener todas las personas que comparten una dirección
     */
    public List<PersonaDireccion> obtenerPersonasQueCompartenDireccion(int direccionId) {
        return personaDireccionDAO.obtenerPorDireccionId(direccionId);
    }

    /**
     * Obtener estadísticas de uso de direcciones
     */
    public String obtenerEstadisticasDireccion(int direccionId) {
        int cantidadPersonas = personaDireccionDAO.contarPersonasPorDireccion(direccionId);
        Direccion direccion = direccionDAO.obtenerPorId(direccionId);

        if (direccion == null) {
            return "Dirección no encontrada";
        }

        StringBuilder stats = new StringBuilder();
        stats.append("Dirección: ").append(direccion.getDireccionCompleta()).append("\n");
        stats.append("Personas que la usan: ").append(cantidadPersonas).append("\n");

        if (cantidadPersonas > 0) {
            List<PersonaDireccion> relaciones = personaDireccionDAO.obtenerPorDireccionId(direccionId);
            stats.append("Detalles:\n");
            for (PersonaDireccion relacion : relaciones) {
                stats.append("  - Persona ID ").append(relacion.getPersonaId())
                        .append(" (").append(relacion.getEtiqueta()).append(")")
                        .append(relacion.isEsPrincipal() ? " - Principal" : "")
                        .append("\n");
            }
        }

        return stats.toString();
    }

    /**
     * Buscar direcciones similares para evitar duplicados
     */
    public List<Direccion> buscarDireccionesSimilares(Direccion direccion) {
        return direccionDAO.buscarSimilares(direccion);
    }

    /**
     * Sugerir dirección existente basada en texto parcial
     */
    public ObservableList<Direccion> sugerirDirecciones(String textoParcial) {
        if (textoParcial == null || textoParcial.trim().length() < 3) {
            return FXCollections.observableArrayList();
        }

        return direccionDAO.buscarPorTexto(textoParcial.trim());
    }

    /**
     * Validar los datos de una dirección
     */
    private void validarDireccion(Direccion direccion) throws IllegalArgumentException {
        if (direccion == null) {
            throw new IllegalArgumentException("La dirección no puede ser null");
        }

        // Al menos uno de los campos principales debe tener contenido
        boolean tieneContenido = false;

        if (direccion.getCalle() != null && !direccion.getCalle().trim().isEmpty()) {
            tieneContenido = true;
            if (direccion.getCalle().trim().length() > 200) {
                throw new IllegalArgumentException("La calle no puede exceder 200 caracteres");
            }
        }

        if (direccion.getCiudad() != null && !direccion.getCiudad().trim().isEmpty()) {
            tieneContenido = true;
            if (direccion.getCiudad().trim().length() > 100) {
                throw new IllegalArgumentException("La ciudad no puede exceder 100 caracteres");
            }
        }

        if (!tieneContenido) {
            throw new IllegalArgumentException("La dirección debe tener al menos calle o ciudad");
        }

        // Validar longitudes opcionales
        if (direccion.getEstado() != null && direccion.getEstado().length() > 100) {
            throw new IllegalArgumentException("El estado no puede exceder 100 caracteres");
        }

        if (direccion.getCodigoPostal() != null && direccion.getCodigoPostal().length() > 20) {
            throw new IllegalArgumentException("El código postal no puede exceder 20 caracteres");
        }

        if (direccion.getPais() != null && direccion.getPais().length() > 100) {
            throw new IllegalArgumentException("El país no puede exceder 100 caracteres");
        }
    }

    /**
     * Obtener direcciones más utilizadas
     */
    public List<Direccion> obtenerDireccionesMasUtilizadas(int limite) {
        // Esta implementación requeriría una consulta más compleja
        // Por ahora, devolvemos todas las direcciones ordenadas por uso
        ObservableList<Direccion> todasDirecciones = direccionDAO.obtenerTodas();

        // Ordenar por número de usuarios (esto se podría optimizar con una consulta SQL)
        todasDirecciones.sort((d1, d2) -> {
            int uso1 = personaDireccionDAO.contarPersonasPorDireccion(d1.getId());
            int uso2 = personaDireccionDAO.contarPersonasPorDireccion(d2.getId());
            return Integer.compare(uso2, uso1); // Orden descendente
        });

        return todasDirecciones.subList(0, Math.min(limite, todasDirecciones.size()));
    }

    /**
     * Verificar integridad de datos de direcciones
     */
    public void verificarIntegridad() {
        System.out.println("Verificando integridad de direcciones...");

        ObservableList<Direccion> direcciones = direccionDAO.obtenerTodas();
        int direccionesSinUso = 0;
        int direccionesCompartidas = 0;

        for (Direccion direccion : direcciones) {
            int cantidadPersonas = personaDireccionDAO.contarPersonasPorDireccion(direccion.getId());

            if (cantidadPersonas == 0) {
                direccionesSinUso++;
                System.out.println("⚠ Dirección sin uso: " + direccion.getDireccionCompleta());
            } else if (cantidadPersonas > 1) {
                direccionesCompartidas++;
                System.out.println("✓ Dirección compartida por " + cantidadPersonas + " personas: " +
                        direccion.getDireccionResumida());
            }
        }

        System.out.println("Resumen de integridad:");
        System.out.println("- Total direcciones: " + direcciones.size());
        System.out.println("- Direcciones sin uso: " + direccionesSinUso);
        System.out.println("- Direcciones compartidas: " + direccionesCompartidas);
    }
}