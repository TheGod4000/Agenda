package com.agenda;

import javafx.collections.ObservableList;

public interface IPersonaService {
    boolean crearPersona(Persona persona) throws IllegalArgumentException;
    Persona obtenerPersona(int id);
    ObservableList<Persona> obtenerTodasPersonas();
    boolean actualizarPersona(Persona persona) throws IllegalArgumentException;
    boolean eliminarPersona(int id);
    ObservableList<Persona> buscarPersonas(String nombre);
    ObservableList<Persona> buscarPersonasPorDireccion(int direccionId);
    boolean agregarTelefono(Persona persona, String numeroTelefono);
    String obtenerEstadisticasPersona(Persona persona);
}
