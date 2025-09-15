/*
 * Servicio para lógica de negocio de Persona - Versión actualizada
 */
package com.agenda;

import javafx.collections.ObservableList;
import java.util.Map;

/**
 * Servicio para lógica de negocio de Persona con soporte para múltiples direcciones
 */
public class PersonaService {
    private final PersonaDAO personaDAO;
    private final DireccionService direccionService;

    public PersonaService() {
        this.personaDAO = new PersonaDAO();
        this.direccionService = new DireccionService();
    }

    public PersonaService(PersonaDAO personaDAO, DireccionService direccionService) {
        this.personaDAO = personaDAO;
        this.direccionService = direccionService;
    }

    /**
     * Crear una nueva persona con validaciones
     */
    public boolean crearPersona(Persona persona) throws IllegalArgumentException {
        validarPersona(persona);
        return personaDAO.crear(persona);
    }

    /**
     * Obtener persona por ID
     */
    public Persona obtenerPersona(int id) {
        return personaDAO.obtenerPorId(id);
    }

    /**
     * Obtener todas las personas
     */
    public ObservableList<Persona> obtenerTodasPersonas() {
        return personaDAO.obtenerTodas();
    }

    /**
     * Actualizar persona con validaciones
     */
    public boolean actualizarPersona(Persona persona) throws IllegalArgumentException {
        validarPersona(persona);
        return personaDAO.actualizar(persona);
    }

    /**
     * Eliminar persona
     */
    public boolean eliminarPersona(int id) {
        return personaDAO.eliminar(id);
    }

    /**
     * Buscar personas por nombre
     */
    public ObservableList<Persona> buscarPersonas(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return obtenerTodasPersonas();
        }
        return personaDAO.buscarPorNombre(nombre.trim());
    }

    /**
     * Buscar personas que comparten una dirección
     */
    public ObservableList<Persona> buscarPersonasPorDireccion(int direccionId) {
        return personaDAO.buscarPorDireccion(direccionId);
    }

    /**
     * Agregar teléfono a una persona
     */
    public boolean agregarTelefono(Persona persona, String numeroTelefono) {
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de teléfono no puede estar vacío");
        }

        // Validar formato del teléfono
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
     * Agregar una nueva dirección a una persona
     */
    public boolean agregarDireccion(Persona persona, Direccion direccion, String etiqueta, boolean esPrincipal)
            throws IllegalArgumentException {

        try {
            // Verificar si la dirección ya existe
            boolean direccionExiste = direccion.getId() > 0;

            if (!direccionExiste) {
                // Buscar direcciones similares
                var similares = direccionService.buscarDireccionesSimilares(direccion);

                if (!similares.isEmpty()) {
                    // Preguntar al usuario si quiere usar una dirección existente
                    // Por ahora, usamos la primera similar encontrada
                    direccion = similares.get(0);
                    direccionExiste = true;
                    System.out.println("Usando dirección existente similar: " + direccion.getDireccionCompleta());
                }
            }

            // Si es principal, quitar el principal anterior
            if (esPrincipal) {
                for (Map.Entry<Direccion, PersonaDireccion> entry : persona.getRelacionesDireccion().entrySet()) {
                    if (entry.getValue().isEsPrincipal()) {
                        entry.getValue().setEsPrincipal(false);
                        break;
                    }
                }
            }

            // Crear la relación
            PersonaDireccion relacion = new PersonaDireccion(
                    persona.getId(),
                    direccion.getId(),
                    etiqueta != null ? etiqueta : "Dirección",
                    esPrincipal
            );

            // Agregar a la persona
            persona.addDireccion(direccion, relacion);

            return true;

        } catch (Exception e) {
            throw new IllegalArgumentException("Error al agregar dirección: " + e.getMessage());
        }
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
        // Quitar principal anterior
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

    /**
     * Obtener un resumen de las direcciones de una persona
     */
    public String obtenerResumenDirecciones(Persona persona) {
        if (persona.getDirecciones().isEmpty()) {
            return "Sin direcciones registradas";
        }

        StringBuilder resumen = new StringBuilder();
        resumen.append("Direcciones de ").append(persona.getNombre()).append(":\n");

        for (Map.Entry<Direccion, PersonaDireccion> entry : persona.getRelacionesDireccion().entrySet()) {
            Direccion dir = entry.getKey();
            PersonaDireccion relacion = entry.getValue();

            resumen.append("• ").append(relacion.getEtiqueta()).append(": ")
                    .append(dir.getDireccionCompleta());

            if (relacion.isEsPrincipal()) {
                resumen.append(" (PRINCIPAL)");
            }

            resumen.append("\n");
        }

        return resumen.toString();
    }

    /**
     * Migrar persona del sistema antiguo (con dirección como string) al nuevo
     */
    public boolean migrarPersonaANuevoSistema(Persona persona, String direccionAntigua) {
        if (direccionAntigua == null || direccionAntigua.trim().isEmpty()) {
            return true; // No hay nada que migrar
        }

        try {
            // Crear dirección desde el string antiguo
            Direccion nuevaDireccion = parsearDireccionAntigua(direccionAntigua);

            // Buscar si ya existe una dirección similar
            var similares = direccionService.buscarDireccionesSimilares(nuevaDireccion);
            if (!similares.isEmpty()) {
                nuevaDireccion = similares.get(0);
                System.out.println("Usando dirección existente para migración: " + nuevaDireccion.getDireccionCompleta());
            } else {
                // Crear la nueva dirección
                if (!direccionService.crearDireccion(nuevaDireccion)) {
                    throw new RuntimeException("Error al crear dirección durante migración");
                }
            }

            // Asociar la dirección a la persona como principal
            return agregarDireccion(persona, nuevaDireccion, "Principal", true);

        } catch (Exception e) {
            System.err.println("Error al migrar dirección de persona " + persona.getId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Parsear una dirección del formato antiguo (string) a la nueva estructura
     */
    private Direccion parsearDireccionAntigua(String direccionAntigua) {
        // Implementación básica - se puede mejorar con parsing más inteligente
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

    /**
     * Validar los datos de una persona
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

        // Validar direcciones a través del servicio de direcciones
        for (Direccion direccion : persona.getDirecciones()) {
            try {
                direccionService.crearDireccion(new Direccion(
                        direccion.getCalle(),
                        direccion.getCiudad(),
                        direccion.getEstado(),
                        direccion.getCodigoPostal(),
                        direccion.getPais()
                )); // Solo para validación, no se guarda
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Error en dirección: " + e.getMessage());
            } catch (Exception e) {
                // Ignorar otros errores (como duplicados) durante validación
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
     * Obtener estadísticas de una persona
     */
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
}