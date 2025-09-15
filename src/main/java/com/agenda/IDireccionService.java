package com.agenda;

import javafx.collections.ObservableList;
import java.util.List;

public interface IDireccionService {
    boolean crearDireccion(Direccion direccion) throws IllegalArgumentException;
    Direccion obtenerDireccion(int id);
    ObservableList<Direccion> obtenerTodasDirecciones();
    boolean actualizarDireccion(Direccion direccion) throws IllegalArgumentException;
    boolean eliminarDireccion(int id);
    List<Direccion> buscarDireccionesSimilares(Direccion direccion);
    ObservableList<Direccion> buscarDirecciones(String texto);
}