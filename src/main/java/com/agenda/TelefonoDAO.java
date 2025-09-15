package com.agenda;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TelefonoDAO implements ITelefonoDAO {
    private final DatabaseConnection dbConnection;

    public TelefonoDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public TelefonoDAO(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public boolean crear(Telefono telefono) {
        try (Connection conn = dbConnection.getConnection()) {
            return crearConConexion(telefono, conn);
        } catch (SQLException e) {
            System.err.println("Error al crear teléfono: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Crear teléfono usando una conexión proporcionada (para uso en transacciones)
     */
    public boolean crearConConexion(Telefono telefono, Connection conn) throws SQLException {
        String sql = "INSERT INTO Telefonos (personaId, telefono) VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, telefono.getPersonaId());
            pstmt.setString(2, telefono.getTelefono());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        telefono.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Telefono obtenerPorId(int id) {
        String sql = "SELECT * FROM Telefonos WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Telefono(
                            rs.getInt("id"),
                            rs.getInt("personaId"),
                            rs.getString("telefono")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener teléfono: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<Telefono> obtenerPorPersonaId(int personaId) {
        List<Telefono> telefonos = new ArrayList<>();
        String sql = "SELECT * FROM Telefonos WHERE personaId = ? ORDER BY telefono";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, personaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Telefono telefono = new Telefono(
                            rs.getInt("id"),
                            rs.getInt("personaId"),
                            rs.getString("telefono")
                    );
                    telefonos.add(telefono);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener teléfonos para persona " + personaId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return telefonos;
    }

    @Override
    public boolean actualizar(Telefono telefono) {
        String sql = "UPDATE Telefonos SET telefono = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, telefono.getTelefono());
            pstmt.setInt(2, telefono.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar teléfono: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM Telefonos WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar teléfono: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean eliminarPorPersonaId(int personaId) {
        try (Connection conn = dbConnection.getConnection()) {
            return eliminarPorPersonaIdConConexion(personaId, conn);
        } catch (SQLException e) {
            System.err.println("Error al eliminar teléfonos para persona " + personaId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Eliminar teléfonos usando una conexión proporcionada (para uso en transacciones)
     */
    public boolean eliminarPorPersonaIdConConexion(int personaId, Connection conn) throws SQLException {
        String sql = "DELETE FROM Telefonos WHERE personaId = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, personaId);
            int affectedRows = pstmt.executeUpdate();
            System.out.println("Eliminados " + affectedRows + " teléfonos para persona ID: " + personaId);
            return true;
        }
    }
}