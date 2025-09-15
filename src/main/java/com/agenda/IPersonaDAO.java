package com.agenda;

import javafx.collections.ObservableList;

public interface IPersonaDAO {
    boolean crear(Persona persona);
    Persona obtenerPorId(int id);
    ObservableList<Persona> obtenerTodas();
    boolean actualizar(Persona persona);
    boolean eliminar(int id);
    ObservableList<Persona> buscarPorNombre(String nombre);
    ObservableList<Persona> buscarPorDireccion(int direccionId);
}
