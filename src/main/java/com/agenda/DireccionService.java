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
    /**
     * Validar dirección básica
     */
    private void validarDireccion(Direccion direccion) throws IllegalArgumentException {
        if (direccion == null) {
            throw new IllegalArgumentException("La dirección no puede ser nula");
        }

        // Al menos calle o ciudad deben tener contenido
        if ((direccion.getCalle() == null || direccion.getCalle().trim().isEmpty()) &&
                (direccion.getCiudad() == null || direccion.getCiudad().trim().isEmpty())) {
            throw new IllegalArgumentException("Debe especificar al menos la calle o la ciudad");
        }

        // Validar longitudes máximas
        if (direccion.getCalle() != null && direccion.getCalle().length() > 200) {
            throw new IllegalArgumentException("La calle no puede exceder 200 caracteres");
        }

        if (direccion.getCiudad() != null && direccion.getCiudad().length() > 100) {
            throw new IllegalArgumentException("La ciudad no puede exceder 100 caracteres");
        }

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
     * Validar dirección con validaciones más exhaustivas
     */
    private void validarDireccionCompleta(Direccion direccion) throws IllegalArgumentException {
        validarDireccion(direccion); // Validaciones básicas

        // Validaciones adicionales para creación completa
        if (direccion.getCalle() == null || direccion.getCalle().trim().isEmpty()) {
            throw new IllegalArgumentException("La calle es requerida para validación completa");
        }

        if (direccion.getCiudad() == null || direccion.getCiudad().trim().isEmpty()) {
            throw new IllegalArgumentException("La ciudad es requerida para validación completa");
        }

        // Validar formato de código postal básico (números, letras y guiones)
        if (direccion.getCodigoPostal() != null && !direccion.getCodigoPostal().trim().isEmpty()) {
            String codigoPostal = direccion.getCodigoPostal().trim();
            if (!codigoPostal.matches("^[A-Za-z0-9\\-\\s]+$")) {
                throw new IllegalArgumentException("El código postal contiene caracteres inválidos");
            }
        }

        // Validar que no contenga solo espacios en blanco
        if (direccion.getCalle().trim().isEmpty()) {
            throw new IllegalArgumentException("La calle no puede estar vacía");
        }

        if (direccion.getCiudad().trim().isEmpty()) {
            throw new IllegalArgumentException("La ciudad no puede estar vacía");
        }
    }

    /**
     * Verificar si dos direcciones son muy parecidas
     */
    private boolean sonDireccionesMuyParecidas(Direccion direccion1, Direccion direccion2) {
        if (direccion1 == null || direccion2 == null) {
            return false;
        }

        // Normalizar textos para comparación
        String calle1 = normalizar(direccion1.getCalle());
        String calle2 = normalizar(direccion2.getCalle());
        String ciudad1 = normalizar(direccion1.getCiudad());
        String ciudad2 = normalizar(direccion2.getCiudad());
        String cp1 = normalizar(direccion1.getCodigoPostal());
        String cp2 = normalizar(direccion2.getCodigoPostal());

        // Son muy parecidas si:
        // 1. Tienen el mismo código postal Y calle muy similar
        boolean mismoCodigoPostal = !cp1.isEmpty() && !cp2.isEmpty() && cp1.equals(cp2);
        boolean callesSimilares = sonTextosSimilares(calle1, calle2, 0.8);

        if (mismoCodigoPostal && callesSimilares) {
            return true;
        }

        // 2. Misma ciudad Y calle idéntica
        boolean mismaCiudad = !ciudad1.isEmpty() && !ciudad2.isEmpty() && ciudad1.equals(ciudad2);
        boolean callesIdenticas = !calle1.isEmpty() && !calle2.isEmpty() && calle1.equals(calle2);

        return mismaCiudad && callesIdenticas;
    }

    /**
     * Normalizar texto para comparaciones
     */
    private String normalizar(String texto) {
        if (texto == null) return "";
        return texto.trim().toLowerCase()
                .replaceAll("\\s+", " ") // Múltiples espacios a uno solo
                .replaceAll("[^a-z0-9\\s]", ""); // Quitar caracteres especiales
    }

    /**
     * Verificar si dos textos son similares usando distancia de Levenshtein simplificada
     */
    private boolean sonTextosSimilares(String texto1, String texto2, double umbralSimilitud) {
        if (texto1.isEmpty() || texto2.isEmpty()) {
            return false;
        }

        int distancia = calcularDistanciaLevenshtein(texto1, texto2);
        int longitudMaxima = Math.max(texto1.length(), texto2.length());
        double similitud = 1.0 - (double) distancia / longitudMaxima;

        return similitud >= umbralSimilitud;
    }

    /**
     * Calcular distancia de Levenshtein entre dos cadenas
     */
    private int calcularDistanciaLevenshtein(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                                dp[i - 1][j] + 1,      // Eliminación
                                dp[i][j - 1] + 1),     // Inserción
                        dp[i - 1][j - 1] + cost // Sustitución
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Asociar una dirección a una persona con etiqueta y configuración principal
     */
    public boolean asociarDireccionAPersona(int personaId, int direccionId, String etiqueta, boolean esPrincipal)
            throws IllegalArgumentException {

        if (personaId <= 0) {
            throw new IllegalArgumentException("ID de persona inválido");
        }

        if (direccionId <= 0) {
            throw new IllegalArgumentException("ID de dirección inválido");
        }

        if (etiqueta == null || etiqueta.trim().isEmpty()) {
            etiqueta = "Dirección";
        }

        // Verificar que la persona existe
        PersonaService personaService = new PersonaService();
        if (personaService.obtenerPersona(personaId) == null) {
            throw new IllegalArgumentException("No existe una persona con ID: " + personaId);
        }

        // Verificar que la dirección existe
        if (obtenerDireccion(direccionId) == null) {
            throw new IllegalArgumentException("No existe una dirección con ID: " + direccionId);
        }

        PersonaDireccionDAO personaDireccionDAO = new PersonaDireccionDAO();

        // Verificar si ya existe la asociación
        List<PersonaDireccion> relacionesExistentes = personaDireccionDAO.obtenerPorPersonaId(personaId);
        for (PersonaDireccion relacion : relacionesExistentes) {
            if (relacion.getDireccionId() == direccionId) {
                throw new IllegalArgumentException("La persona ya tiene asociada esta dirección");
            }
        }

        // Si se marca como principal, quitar el flag principal de otras direcciones
        if (esPrincipal) {
            for (PersonaDireccion relacion : relacionesExistentes) {
                if (relacion.isEsPrincipal()) {
                    relacion.setEsPrincipal(false);
                    personaDireccionDAO.actualizar(relacion);
                }
            }
        }

        // Crear la nueva relación
        PersonaDireccion nuevaRelacion = new PersonaDireccion(personaId, direccionId, etiqueta.trim(), esPrincipal);

        return personaDireccionDAO.crear(nuevaRelacion);
    }

    /**
     * Desasociar una dirección de una persona
     */
    public boolean desasociarDireccionDePersona(int personaId, int direccionId) throws IllegalArgumentException {
        if (personaId <= 0) {
            throw new IllegalArgumentException("ID de persona inválido");
        }

        if (direccionId <= 0) {
            throw new IllegalArgumentException("ID de dirección inválido");
        }

        PersonaDireccionDAO personaDireccionDAO = new PersonaDireccionDAO();

        // Buscar la relación específica
        List<PersonaDireccion> relaciones = personaDireccionDAO.obtenerPorPersonaId(personaId);
        PersonaDireccion relacionAEliminar = null;

        for (PersonaDireccion relacion : relaciones) {
            if (relacion.getDireccionId() == direccionId) {
                relacionAEliminar = relacion;
                break;
            }
        }

        if (relacionAEliminar == null) {
            throw new IllegalArgumentException("No existe una asociación entre la persona y la dirección especificadas");
        }

        boolean eliminada = personaDireccionDAO.eliminar(relacionAEliminar.getId());

        // Si se eliminó una dirección principal y quedan otras direcciones,
        // establecer la primera como principal
        if (eliminada && relacionAEliminar.isEsPrincipal()) {
            List<PersonaDireccion> relacionesRestantes = personaDireccionDAO.obtenerPorPersonaId(personaId);
            if (!relacionesRestantes.isEmpty()) {
                PersonaDireccion nuevaPrincipal = relacionesRestantes.get(0);
                nuevaPrincipal.setEsPrincipal(true);
                personaDireccionDAO.actualizar(nuevaPrincipal);
                System.out.println("Nueva dirección principal establecida para persona " + personaId);
            }
        }

        return eliminada;
    }

    /**
     * Actualizar una relación persona-dirección
     */
    public boolean actualizarRelacionPersonaDireccion(PersonaDireccion relacion) throws IllegalArgumentException {
        if (relacion == null) {
            throw new IllegalArgumentException("La relación no puede ser nula");
        }

        if (relacion.getId() <= 0) {
            throw new IllegalArgumentException("ID de relación inválido");
        }

        if (relacion.getEtiqueta() == null || relacion.getEtiqueta().trim().isEmpty()) {
            relacion.setEtiqueta("Dirección");
        }

        PersonaDireccionDAO personaDireccionDAO = new PersonaDireccionDAO();

        // Si se marca como principal, quitar el flag principal de otras direcciones de la misma persona
        if (relacion.isEsPrincipal()) {
            List<PersonaDireccion> relacionesPersona = personaDireccionDAO.obtenerPorPersonaId(relacion.getPersonaId());
            for (PersonaDireccion otraRelacion : relacionesPersona) {
                if (otraRelacion.getId() != relacion.getId() && otraRelacion.isEsPrincipal()) {
                    otraRelacion.setEsPrincipal(false);
                    personaDireccionDAO.actualizar(otraRelacion);
                }
            }
        }

        return personaDireccionDAO.actualizar(relacion);
    }

    /**
     * Verificar la integridad de los datos de direcciones
     */
    public void verificarIntegridad() {
        System.out.println("\n=== Verificación de Integridad de Direcciones ===");

        try {
            ObservableList<Direccion> todasDirecciones = obtenerTodasDirecciones();
            PersonaDireccionDAO personaDireccionDAO = new PersonaDireccionDAO();

            int direccionesSinUso = 0;
            int direccionesCompartidas = 0;
            int relacionesOrfanas = 0;

            // Verificar direcciones
            for (Direccion direccion : todasDirecciones) {
                int cantidadPersonas = personaDireccionDAO.contarPersonasPorDireccion(direccion.getId());

                if (cantidadPersonas == 0) {
                    direccionesSinUso++;
                    System.out.println("⚠ Dirección sin uso: " + direccion.getDireccionResumida());
                } else if (cantidadPersonas > 1) {
                    direccionesCompartidas++;
                }
            }

            // Verificar relaciones persona-dirección
            List<PersonaDireccion> todasRelaciones = personaDireccionDAO.obtenerTodas();
            PersonaService personaService = new PersonaService();

            for (PersonaDireccion relacion : todasRelaciones) {
                // Verificar que la persona existe
                if (personaService.obtenerPersona(relacion.getPersonaId()) == null) {
                    relacionesOrfanas++;
                    System.out.println("⚠ Relación huérfana: PersonaID " + relacion.getPersonaId() +
                            " no existe (DirecciónID: " + relacion.getDireccionId() + ")");
                }

                // Verificar que la dirección existe
                if (obtenerDireccion(relacion.getDireccionId()) == null) {
                    relacionesOrfanas++;
                    System.out.println("⚠ Relación huérfana: DirecciónID " + relacion.getDireccionId() +
                            " no existe (PersonaID: " + relacion.getPersonaId() + ")");
                }
            }

            // Verificar direcciones principales múltiples por persona
            Map<Integer, Integer> direccionesPrincipalesPorPersona = new java.util.HashMap<>();
            for (PersonaDireccion relacion : todasRelaciones) {
                if (relacion.isEsPrincipal()) {
                    direccionesPrincipalesPorPersona.put(
                            relacion.getPersonaId(),
                            direccionesPrincipalesPorPersona.getOrDefault(relacion.getPersonaId(), 0) + 1
                    );
                }
            }

            int personasConMultiplesPrincipales = 0;
            for (Map.Entry<Integer, Integer> entry : direccionesPrincipalesPorPersona.entrySet()) {
                if (entry.getValue() > 1) {
                    personasConMultiplesPrincipales++;
                    System.out.println("⚠ Persona " + entry.getKey() +
                            " tiene " + entry.getValue() + " direcciones principales");
                }
            }

            // Resumen
            System.out.println("\n--- Resumen de Integridad ---");
            System.out.println("Total direcciones: " + todasDirecciones.size());
            System.out.println("Direcciones sin uso: " + direccionesSinUso);
            System.out.println("Direcciones compartidas: " + direccionesCompartidas);
            System.out.println("Total relaciones: " + todasRelaciones.size());
            System.out.println("Relaciones huérfanas: " + relacionesOrfanas);
            System.out.println("Personas con múltiples direcciones principales: " + personasConMultiplesPrincipales);

            if (direccionesSinUso == 0 && relacionesOrfanas == 0 && personasConMultiplesPrincipales == 0) {
                System.out.println("✓ La integridad de los datos es correcta");
            } else {
                System.out.println("⚠ Se encontraron problemas de integridad");
            }

        } catch (Exception e) {
            System.err.println("Error durante verificación de integridad: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
