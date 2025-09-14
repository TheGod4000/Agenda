/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.agenda;

/**
 *
 * @author Luisg
 */
import javafx.collections.ObservableList;

/**
 * Servicio para lógica de negocio de Persona
 */
public class PersonaService {
    private final PersonaDAO personaDAO;

    public PersonaService() {
        this.personaDAO = new PersonaDAO();
    }

    public PersonaService(PersonaDAO personaDAO) {
        this.personaDAO = personaDAO;
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

        if (persona.getDireccion() != null && persona.getDireccion().length() > 200) {
            throw new IllegalArgumentException("La dirección no puede exceder 200 caracteres");
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
    }

    /**
     * Validar formato de teléfono
     * Acepta formatos como: 555-1234567, (555) 123-4567, 555.123.4567, +52 555 123 4567
     */
    private boolean validarFormatoTelefono(String telefono) {
        if (telefono == null) return false;
        
        String telefonoLimpio = telefono.replaceAll("[\\s\\-\\(\\)\\.]", "");
        
        // Verificar si comienza con + (código de país)
        if (telefonoLimpio.startsWith("+")) {
            telefonoLimpio = telefonoLimpio.substring(1);
        }
        
        // Verificar que solo contenga dígitos y tenga una longitud razonable
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
}