package com.agenda;

import javafx.collections.ObservableList;
import java.util.List;

public interface IDireccionDAO {
    boolean crear(Direccion direccion);
    Direccion obtenerPorId(int id);
    ObservableList<Direccion> obtenerTodas();
    List<Direccion> obtenerPorPersonaId(int personaId);
    boolean actualizar(Direccion direccion);
    boolean eliminar(int id);
    boolean estaEnUso(int direccionId);
    List<Direccion> buscarSimilares(Direccion direccion);
    ObservableList<Direccion> buscarPorTexto(String texto);
}
