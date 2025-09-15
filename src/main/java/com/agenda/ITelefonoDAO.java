package com.agenda;

import java.util.List;

public interface ITelefonoDAO {
    boolean crear(Telefono telefono);
    Telefono obtenerPorId(int id);
    List<Telefono> obtenerPorPersonaId(int personaId);
    boolean actualizar(Telefono telefono);
    boolean eliminar(int id);
    boolean eliminarPorPersonaId(int personaId);
}