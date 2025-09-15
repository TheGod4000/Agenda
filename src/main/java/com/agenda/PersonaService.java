// PersonaService.java - Refactorizado para implementar IPersonaService y manejar transacciones
package com.agenda;

import javafx.collections.ObservableList;
import java.sql.Connection;
import java.util.Map;

/**
 * Servicio refactorizado que implementa IPersonaService y maneja transacciones
 * Aplica los principios SRP (manejo de lógica de negocio) y DIP (depende de abstracciones)
 */
public class PersonaService implements IPersonaService {
    // Dependencias de abstracciones, no de clases concretas (DIP)
    private final IPersonaDAO personaDAO;
    private final IDireccionService direccionService;
    private final ITransactionManager transactionManager;

    // Constructor por defecto (para compatibilidad)
    public PersonaService() {
        this.personaDAO = new PersonaDAO();
        this.direccionService = new DireccionService();
        this.transactionManager = new TransactionManager();
    }

    // Constructor para inyección de dependencias (ideal para testing)
    public PersonaService(IPersonaDAO personaDAO, IDireccionService direccionService, ITransactionManager transactionManager) {
        this.personaDAO = personaDAO;
        this.direccionService = direccionService;
        this.transactionManager = transactionManager;
    }

    /**
     * Crear una nueva persona con transacción (SRP - el servicio maneja la transacción, no el DAO)
     */
    @Override
    public boolean crearPersona(Persona persona) throws IllegalArgumentException {
        validarPersona(persona);

        // El servicio maneja la transacción completa
        return transactionManager.executeInTransaction(conn -> {
            try {
                // Usar el método que acepta conexión para mantener la transacción
                PersonaDAO concreteDAO = (PersonaDAO) personaDAO;
                return concreteDAO.crearConConexion(persona, conn);
            } catch (Exception e) {
                throw new RuntimeException("Error al crear persona en transacción", e);
            }
        });
    }

    @Override
    public Persona obtenerPersona(int id) {
        return personaDAO.obtenerPorId(id);
    }

    @Override
    public ObservableList<Persona> obtenerTodasPersonas() {
        return personaDAO.obtenerTodas();
    }

    /**
     * Actualizar persona con transacción
     */
    @Override
    public boolean actualizarPersona(Persona persona) throws IllegalArgumentException {
        validarPersona(persona);

        return transactionManager.executeInTransaction(conn -> {
            try {
                PersonaDAO concreteDAO = (PersonaDAO) personaDAO;
                return concreteDAO.actualizarConConexion(persona, conn);
            } catch (Exception e) {
                throw new RuntimeException("Error al actualizar persona en transacción", e);
            }
        });
    }

    @Override
    public boolean eliminarPersona(int id) {
        // Para eliminar, podemos usar transacción o el método simple según necesidad
        return personaDAO.eliminar(id);
    }

    @Override
    public ObservableList<Persona> buscarPersonas(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return obtenerTodasPersonas();
        }
        return personaDAO.buscarPorNombre(nombre.trim());
    }

    @Override
    public ObservableList<Persona> buscarPersonasPorDireccion(int direccionId) {
        return personaDAO.buscarPorDireccion(direccionId);
    }

    @Override
    public boolean agregarTelefono(Persona persona, String numeroTelefono) {
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de teléfono no puede estar vacío");
        }

        if (!validarFormatoTelefono(numeroTelefono.trim())) {
            throw new IllegalArgumentException("Formato de teléfono inválido");
        }

        // Verificar que no exista el mismo teléfono para esta persona
        for (Telefono tel : persona.getTelefonos()) {
            if (tel.getTelefono().equals(numeroTelefono.trim())) {
                throw new IllegalArgumentException("Este teléfono ya existe para la persona");
            }
        }

        Telefono nuevoTelefono = new Telefono(persona.getId(), numeroTelefono.trim());
        persona.addTelefono(nuevoTelefono);

        return true;
    }

    /**
     * Agregar una nueva dirección a una persona con transacción
     */
    public boolean agregarDireccion(Persona persona, Direccion direccion, String etiqueta, boolean esPrincipal) {
        final Direccion direccionFinal = direccion; // copia efectivamente final

        return transactionManager.executeInTransaction(conn -> {
            try {
                boolean direccionExiste = direccionFinal.getId() > 0;

                Direccion direccionUsada = direccionFinal; // nueva referencia local

                if (!direccionExiste) {
                    var similares = direccionService.buscarDireccionesSimilares(direccionFinal);

                    if (!similares.isEmpty()) {
                        direccionUsada = similares.get(0);
                        direccionExiste = true;
                        System.out.println("Usando dirección existente similar: " + direccionUsada.getDireccionCompleta());
                    } else {
                        if (!direccionService.crearDireccion(direccionFinal)) {
                            throw new RuntimeException("No se pudo crear la dirección");
                        }
                    }
                }

                if (esPrincipal) {
                    for (Map.Entry<Direccion, PersonaDireccion> entry : persona.getRelacionesDireccion().entrySet()) {
                        if (entry.getValue().isEsPrincipal()) {
                            entry.getValue().setEsPrincipal(false);
                            break;
                        }
                    }
                }

                PersonaDireccion relacion = new PersonaDireccion(
                        persona.getId(),
                        direccionUsada.getId(),
                        etiqueta != null ? etiqueta : "Dirección",
                        esPrincipal
                );

                persona.addDireccion(direccionUsada, relacion);

                return true;
            } catch (Exception e) {
                throw new RuntimeException("Error al agregar dirección: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Agregar una dirección existente a una persona
     */
    public boolean agregarDireccionExistente(Persona persona, int direccionId, String etiqueta, boolean esPrincipal)
            throws IllegalArgumentException {

        Direccion direccion = direccionService.obtenerDireccion(direccionId);
        if (direccion == null) {
            throw new IllegalArgumentException("La dirección especificada no existe");
        }

        // Verificar que la persona no tenga ya esta dirección
        for (Direccion dir : persona.getDirecciones()) {
            if (dir.getId() == direccionId) {
                throw new IllegalArgumentException("La persona ya tiene asociada esta dirección");
            }
        }

        return agregarDireccion(persona, direccion, etiqueta, esPrincipal);
    }

    /**
     * Remover una dirección de una persona
     */
    public boolean removerDireccion(Persona persona, int direccionId) {
        Direccion direccionARemover = null;

        for (Direccion dir : persona.getDirecciones()) {
            if (dir.getId() == direccionId) {
                direccionARemover = dir;
                break;
            }
        }

        if (direccionARemover != null) {
            persona.removeDireccion(direccionARemover);
            return true;
        }

        return false;
    }

    /**
     * Establecer una dirección como principal para una persona
     */
    public boolean establecerDireccionPrincipal(Persona persona, int direccionId) {
        for (Map.Entry<Direccion, PersonaDireccion> entry : persona.getRelacionesDireccion().entrySet()) {
            PersonaDireccion relacion = entry.getValue();
            if (relacion.getDireccionId() == direccionId) {
                // Quitar principal de todas las demás
                for (PersonaDireccion otraRelacion : persona.getRelacionesDireccion().values()) {
                    otraRelacion.setEsPrincipal(false);
                }
                // Establecer esta como principal
                relacion.setEsPrincipal(true);
                return true;
            }
        }

        return false;
    }

    @Override
    public String obtenerEstadisticasPersona(Persona persona) {
        StringBuilder stats = new StringBuilder();
        stats.append("Estadísticas de ").append(persona.getNombre()).append(":\n");
        stats.append("- Teléfonos: ").append(persona.getTelefonos().size()).append("\n");
        stats.append("- Direcciones: ").append(persona.getDirecciones().size()).append("\n");

        if (!persona.getDirecciones().isEmpty()) {
            Direccion principal = persona.getDireccionPrincipal();
            stats.append("- Dirección principal: ")
                    .append(principal != null ? principal.getDireccionResumida() : "No establecida")
                    .append("\n");
        }

        return stats.toString();
    }

    /**
     * Crear persona completa con direcciones y teléfonos en una sola transacción
     */
    public boolean crearPersonaCompleta(Persona persona) throws IllegalArgumentException {
        validarPersona(persona);

        return transactionManager.executeInTransaction(conn -> {
            try {
                // Crear persona básica
                PersonaDAO concreteDAO = (PersonaDAO) personaDAO;
                if (!concreteDAO.crearConConexion(persona, conn)) {
                    throw new RuntimeException("Error al crear persona");
                }

                // Crear direcciones y relaciones si las hay
                if (!persona.getDirecciones().isEmpty()) {
                    PersonaDireccionDAO personaDireccionDAO = new PersonaDireccionDAO();
                    DireccionDAO direccionDAO = new DireccionDAO();

                    for (Map.Entry<Direccion, PersonaDireccion> entry : persona.getRelacionesDireccion().entrySet()) {
                        Direccion direccion = entry.getKey();
                        PersonaDireccion relacion = entry.getValue();

                        // Crear dirección si no existe
                        if (direccion.getId() == 0) {
                            if (!direccionDAO.crearConConexion(direccion, conn)) {
                                throw new RuntimeException("Error al crear dirección");
                            }
                        }

                        // Actualizar relación con IDs correctos
                        relacion.setPersonaId(persona.getId());
                        relacion.setDireccionId(direccion.getId());

                        // Crear relación persona-dirección
                        if (!personaDireccionDAO.crearConConexion(relacion, conn)) {
                            throw new RuntimeException("Error al crear relación persona-dirección");
                        }
                    }
                }

                return true;

            } catch (Exception e) {
                throw new RuntimeException("Error en transacción completa: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Validar los datos de una persona (SRP - responsabilidad de validación)
     */
    private void validarPersona(Persona persona) throws IllegalArgumentException {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser null");
        }

        if (persona.getNombre() == null || persona.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        if (persona.getNombre().trim().length() > 100) {
            throw new IllegalArgumentException("El nombre no puede exceder 100 caracteres");
        }

        // Validar teléfonos
        for (Telefono telefono : persona.getTelefonos()) {
            if (telefono.getTelefono() == null || telefono.getTelefono().trim().isEmpty()) {
                throw new IllegalArgumentException("Los teléfonos no pueden estar vacíos");
            }

            if (!validarFormatoTelefono(telefono.getTelefono())) {
                throw new IllegalArgumentException("Formato de teléfono inválido: " + telefono.getTelefono());
            }
        }

        // Validar direcciones básicamente (la validación completa está en DireccionService)
        for (Direccion direccion : persona.getDirecciones()) {
            if (direccion.getCalle() != null && direccion.getCalle().length() > 200) {
                throw new IllegalArgumentException("La calle no puede exceder 200 caracteres");
            }
            if (direccion.getCiudad() != null && direccion.getCiudad().length() > 100) {
                throw new IllegalArgumentException("La ciudad no puede exceder 100 caracteres");
            }
        }
    }

    /**
     * Validar formato de teléfono
     */
    private boolean validarFormatoTelefono(String telefono) {
        if (telefono == null) return false;

        String telefonoLimpio = telefono.replaceAll("[\\s\\-\\(\\)\\.]", "");

        if (telefonoLimpio.startsWith("+")) {
            telefonoLimpio = telefonoLimpio.substring(1);
        }

        return telefonoLimpio.matches("\\d+") && telefonoLimpio.length() >= 7 && telefonoLimpio.length() <= 15;
    }

    /**
     * Verificar si existe una persona con el mismo nombre
     */
    public boolean existePersonaConNombre(String nombre, int excludeId) {
        ObservableList<Persona> personas = buscarPersonas(nombre);
        for (Persona p : personas) {
            if (p.getNombre().equalsIgnoreCase(nombre.trim()) && p.getId() != excludeId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Migrar persona del sistema antiguo al nuevo (para compatibilidad)
     */
    public boolean migrarPersonaANuevoSistema(Persona persona, String direccionAntigua) {
        if (direccionAntigua == null || direccionAntigua.trim().isEmpty()) {
            return true;
        }

        try {
            Direccion nuevaDireccion = parsearDireccionAntigua(direccionAntigua);

            var similares = direccionService.buscarDireccionesSimilares(nuevaDireccion);
            if (!similares.isEmpty()) {
                nuevaDireccion = similares.get(0);
            } else {
                if (!direccionService.crearDireccion(nuevaDireccion)) {
                    throw new RuntimeException("Error al crear dirección durante migración");
                }
            }

            return agregarDireccion(persona, nuevaDireccion, "Principal", true);

        } catch (Exception e) {
            System.err.println("Error al migrar dirección de persona " + persona.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private Direccion parsearDireccionAntigua(String direccionAntigua) {
        String[] partes = direccionAntigua.split(",");

        if (partes.length == 1) {
            return new Direccion(direccionAntigua.trim(), "", "", "", "");
        } else if (partes.length == 2) {
            return new Direccion(partes[0].trim(), partes[1].trim(), "", "", "");
        } else if (partes.length >= 3) {
            return new Direccion(
                    partes[0].trim(),
                    partes[1].trim(),
                    partes[2].trim(),
                    "",
                    ""
            );
        }

        return new Direccion(direccionAntigua, "", "", "", "");
    }
}