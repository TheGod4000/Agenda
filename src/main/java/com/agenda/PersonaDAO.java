// PersonaDAO.java - Refactorizado para implementar IPersonaDAO y quitar manejo de transacciones
package com.agenda;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.util.List;
import java.util.Map;

public class PersonaDAO implements IPersonaDAO {
    private final DatabaseConnection dbConnection;
    private final ITelefonoDAO telefonoDAO;
    private final PersonaDireccionDAO personaDireccionDAO;

    public PersonaDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.telefonoDAO = new TelefonoDAO();
        this.personaDireccionDAO = new PersonaDireccionDAO();
    }

    // Constructor para inyección de dependencias
    public PersonaDAO(DatabaseConnection dbConnection, ITelefonoDAO telefonoDAO, PersonaDireccionDAO personaDireccionDAO) {
        this.dbConnection = dbConnection;
        this.telefonoDAO = telefonoDAO;
        this.personaDireccionDAO = personaDireccionDAO;
    }

    @Override
    public boolean crear(Persona persona) {
        // Método simple sin transacción - la transacción se maneja en el servicio
        try (Connection conn = dbConnection.getConnection()) {
            return crearConConexion(persona, conn);
        } catch (SQLException e) {
            System.err.println("Error al crear persona: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Crear persona usando una conexión proporcionada (para uso en transacciones)
     */
    public boolean crearConConexion(Persona persona, Connection conn) throws SQLException {
        String sql = "INSERT INTO Personas (nombre) VALUES (?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            System.out.println("Creando persona: " + persona.getNombre());
            pstmt.setString(1, persona.getNombre());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        persona.setId(generatedKeys.getInt(1));

                        // Guardar direcciones asociadas
                        for (Map.Entry<Direccion, PersonaDireccion> entry : persona.getRelacionesDireccion().entrySet()) {
                            Direccion direccion = entry.getKey();
                            PersonaDireccion relacion = entry.getValue();

                            if (direccion.getId() == 0) {
                                DireccionDAO direccionDAO = new DireccionDAO(dbConnection);
                                if (!direccionDAO.crearConConexion(direccion, conn)) {
                                    throw new SQLException("Error al crear dirección asociada");
                                }
                            }

                            relacion.setPersonaId(persona.getId());
                            relacion.setDireccionId(direccion.getId());

                            if (!personaDireccionDAO.crearConConexion(relacion, conn)) {
                                throw new SQLException("Error al crear relación persona-dirección");
                            }
                        }

                        // Guardar teléfonos asociados
                        for (Telefono telefono : persona.getTelefonos()) {
                            telefono.setPersonaId(persona.getId());
                            if (!((TelefonoDAO) telefonoDAO).crearConConexion(telefono, conn)) {
                                throw new SQLException("Error al crear teléfono asociado");
                            }
                        }

                        System.out.println("✓ Persona creada con ID: " + persona.getId());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Persona obtenerPorId(int id) {
        String sql = "SELECT * FROM Personas WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Persona persona = new Persona(
                            rs.getInt("id"),
                            rs.getString("nombre")
                    );

                    Map<Direccion, PersonaDireccion> direccionesConInfo =
                            personaDireccionDAO.obtenerDireccionesConInfoPorPersona(persona.getId());
                    persona.setRelacionesDireccion(direccionesConInfo);

                    List<Telefono> telefonos = telefonoDAO.obtenerPorPersonaId(persona.getId());
                    persona.getTelefonos().addAll(telefonos);

                    return persona;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener persona: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ObservableList<Persona> obtenerTodas() {
        ObservableList<Persona> personas = FXCollections.observableArrayList();
        String sql = "SELECT * FROM Personas ORDER BY nombre";

        try (Connection conn = dbConnection.getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    int personaId = rs.getInt("id");
                    String nombre = rs.getString("nombre");

                    Persona persona = new Persona(personaId, nombre);

                    Map<Direccion, PersonaDireccion> direccionesConInfo =
                            personaDireccionDAO.obtenerDireccionesConInfoPorPersona(personaId);
                    persona.setRelacionesDireccion(direccionesConInfo);

                    List<Telefono> telefonos = telefonoDAO.obtenerPorPersonaId(personaId);
                    persona.getTelefonos().clear();
                    persona.getTelefonos().addAll(telefonos);

                    personas.add(persona);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener personas: " + e.getMessage());
            e.printStackTrace();
        }

        return personas;
    }

    @Override
    public boolean actualizar(Persona persona) {
        try (Connection conn = dbConnection.getConnection()) {
            return actualizarConConexion(persona, conn);
        } catch (SQLException e) {
            System.err.println("Error al actualizar persona: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Actualizar persona usando una conexión proporcionada (para uso en transacciones)
     */
    public boolean actualizarConConexion(Persona persona, Connection conn) throws SQLException {
        String sql = "UPDATE Personas SET nombre = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            System.out.println("Actualizando persona ID: " + persona.getId());

            pstmt.setString(1, persona.getNombre());
            pstmt.setInt(2, persona.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                personaDireccionDAO.eliminarPorPersonaIdConConexion(persona.getId(), conn);

                for (Map.Entry<Direccion, PersonaDireccion> entry : persona.getRelacionesDireccion().entrySet()) {
                    Direccion direccion = entry.getKey();
                    PersonaDireccion relacion = entry.getValue();

                    if (direccion.getId() == 0) {
                        DireccionDAO direccionDAO = new DireccionDAO(dbConnection);
                        if (!direccionDAO.crearConConexion(direccion, conn)) {
                            throw new SQLException("Error al crear nueva dirección");
                        }
                    }

                    relacion.setPersonaId(persona.getId());
                    relacion.setDireccionId(direccion.getId());

                    if (!personaDireccionDAO.crearConConexion(relacion, conn)) {
                        throw new SQLException("Error al crear relación persona-dirección actualizada");
                    }
                }

                ((TelefonoDAO) telefonoDAO).eliminarPorPersonaIdConConexion(persona.getId(), conn);

                for (Telefono telefono : persona.getTelefonos()) {
                    telefono.setPersonaId(persona.getId());
                    if (!((TelefonoDAO) telefonoDAO).crearConConexion(telefono, conn)) {
                        throw new SQLException("Error al crear teléfono actualizado");
                    }
                }

                System.out.println("✓ Persona actualizada correctamente");
                return true;
            } else {
                System.err.println("No se pudo actualizar la persona (no se encontró el ID)");
                return false;
            }
        }
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM Personas WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            System.out.println("Eliminando persona ID: " + id);
            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Persona eliminada correctamente");
                return true;
            } else {
                System.err.println("No se encontró persona con ID: " + id);
            }

        } catch (SQLException e) {
            System.err.println("Error al eliminar persona: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public ObservableList<Persona> buscarPorNombre(String nombre) {
        ObservableList<Persona> personas = FXCollections.observableArrayList();
        String sql = "SELECT * FROM Personas WHERE nombre LIKE ? ORDER BY nombre";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String busqueda = "%" + nombre + "%";
            pstmt.setString(1, busqueda);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int personaId = rs.getInt("id");
                    String nombrePersona = rs.getString("nombre");

                    Persona persona = new Persona(personaId, nombrePersona);

                    Map<Direccion, PersonaDireccion> direccionesConInfo =
                            personaDireccionDAO.obtenerDireccionesConInfoPorPersona(personaId);
                    persona.setRelacionesDireccion(direccionesConInfo);

                    List<Telefono> telefonos = telefonoDAO.obtenerPorPersonaId(personaId);
                    persona.getTelefonos().addAll(telefonos);

                    personas.add(persona);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar personas: " + e.getMessage());
            e.printStackTrace();
        }

        return personas;
    }

    @Override
    public ObservableList<Persona> buscarPorDireccion(int direccionId) {
        ObservableList<Persona> personas = FXCollections.observableArrayList();
        String sql = """
            SELECT p.* FROM Personas p 
            INNER JOIN PersonaDirecciones pd ON p.id = pd.persona_id 
            WHERE pd.direccion_id = ? 
            ORDER BY p.nombre
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, direccionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Persona persona = obtenerPorId(rs.getInt("id"));
                    if (persona != null) {
                        personas.add(persona);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar personas por dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return personas;
    }
}