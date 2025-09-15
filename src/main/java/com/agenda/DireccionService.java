// DireccionService.java - Refactorizado para implementar IDireccionService
package com.agenda;

import javafx.collections.ObservableList;
import java.util.List;
import java.util.Map;

/**
 * Servicio para lógica de negocio de Direcciones que implementa IDireccionService
 * Aplica los principios SRP (manejo de lógica de negocio de direcciones) y DIP (depende de abstracciones)
 */
public class DireccionService implements IDireccionService {
    // Dependencia de abstracción, no de clase concreta (DIP)
    private final IDireccionDAO direccionDAO;
    private final ITransactionManager transactionManager;

    // Constructor por defecto
    public DireccionService() {
        this.direccionDAO = new DireccionDAO();
        this.transactionManager = new TransactionManager();
    }

    // Constructor para inyección de dependencias
    public DireccionService(IDireccionDAO direccionDAO, ITransactionManager transactionManager) {
        this.direccionDAO = direccionDAO;
        this.transactionManager = transactionManager;
    }

    @Override
    public boolean crearDireccion(Direccion direccion) throws IllegalArgumentException {
        validarDireccion(direccion);

        // Verificar si ya existe una dirección similar
        List<Direccion> similares = direccionDAO.buscarSimilares(direccion);
        if (!similares.isEmpty()) {
            System.out.println("Se encontraron direcciones similares. Considere usar una existente.");
            // En una implementación real, podrías lanzar una excepción específica
            // o devolver información sobre las direcciones similares
        }

        // Guardar la dirección en BD
        return direccionDAO.crear(direccion);
    }

    @Override
    public Direccion obtenerDireccion(int id) {
        return direccionDAO.obtenerPorId(id);
    }

    @Override
    public ObservableList<Direccion> obtenerTodasDirecciones() {
        return direccionDAO.obtenerTodas();
    }

    @Override
    public boolean actualizarDireccion(Direccion direccion) throws IllegalArgumentException {
        validarDireccion(direccion);

        if (direccion.getId() <= 0) {
            throw new IllegalArgumentException("La dirección debe tener un ID válido para ser actualizada");
        }

        return direccionDAO.actualizar(direccion);
    }

    @Override
    public boolean eliminarDireccion(int id) {
        // Verificar si la dirección está en uso antes de eliminar
        if (direccionDAO.estaEnUso(id)) {
            throw new IllegalArgumentException("No se puede eliminar la dirección porque está siendo usada por una o más personas");
        }

        return direccionDAO.eliminar(id);
    }

    @Override
    public List<Direccion> buscarDireccionesSimilares(Direccion direccion) {
        return direccionDAO.buscarSimilares(direccion);
    }

    @Override
    public ObservableList<Direccion> buscarDirecciones(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return obtenerTodasDirecciones();
        }
        return direccionDAO.buscarPorTexto(texto.trim());
    }

    /**
     * Obtener direcciones de una persona específica
     */
    public List<Direccion> obtenerDireccionesPorPersona(int personaId) {
        return direccionDAO.obtenerPorPersonaId(personaId);
    }

    /**
     * Verificar si una dirección está en uso
     */
    public boolean direccionEstaEnUso(int direccionId) {
        return direccionDAO.estaEnUso(direccionId);
    }

    /**
     * Crear una dirección con validación exhaustiva
     */
    public boolean crearDireccionConValidacionCompleta(Direccion direccion) throws IllegalArgumentException {
        validarDireccionCompleta(direccion);

        return transactionManager.executeInTransaction(conn -> {
            try {
                // Buscar direcciones muy similares (mismo código postal + calle similar)
                List<Direccion> similares = buscarDireccionesSimilares(direccion);

                for (Direccion similar : similares) {
                    if (sonDireccionesMuyParecidas(direccion, similar)) {
                        throw new RuntimeException("Ya existe una dirección muy similar: " + similar.getDireccionCompleta());
                    }
                }

                DireccionDAO concreteDAO = (DireccionDAO) direccionDAO;
                return concreteDAO.crearConConexion(direccion, conn);

            } catch (Exception e) {
                throw new RuntimeException("Error al crear dirección: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Obtener estadísticas de direcciones
     */
    public String obtenerEstadisticasDirecciones() {
        ObservableList<Direccion> todasDirecciones = obtenerTodasDirecciones();

        // Contar por ciudad
        Map<String, Long> porCiudad = todasDirecciones.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        d -> d.getCiudad() != null ? d.getCiudad() : "Sin ciudad",
                        java.util.stream.Collectors.counting()
                ));

        // Contar por estado
        Map<String, Long> porEstado = todasDirecciones.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        d -> d.getEstado() != null ? d.getEstado() : "Sin estado",
                        java.util.stream.Collectors.counting()
                ));

        StringBuilder stats = new StringBuilder();
        stats.append("Estadísticas de Direcciones:\n");
        stats.append("Total de direcciones: ").append(todasDirecciones.size()).append("\n\n");

        stats.append("Por Ciudad:\n");
        porCiudad.forEach((ciudad, count) ->
                stats.append("  - ").append(ciudad).append(": ").append(count).append("\n"));

        stats.append("\nPor Estado:\n");
        porEstado.forEach((estado, count) ->
                stats.append("  - ").append(estado).append(": ").append(count).append("\n"));

        return stats.toString();
    }
}
