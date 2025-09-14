/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.agenda;

/**
 *
 * @author Luisg
 */
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de Teléfono - Versión Mejorada
 */
public class TelefonoDAO {
    private final DatabaseConnection dbConnection;

    public TelefonoDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Crear un nuevo teléfono
     */
    public boolean crear(Telefono telefono) {
        String sql = "INSERT INTO Telefonos (personaId, telefono) VALUES (?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
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
            
        } catch (SQLException e) {
            System.err.println("Error al crear teléfono: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Obtener un teléfono por ID
     */
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

    /**
     * Obtener todos los teléfonos de una persona - VERSIÓN MEJORADA
     */
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

    /**
     * Actualizar un teléfono
     */
    public boolean actualizar(Telefono telefono) {
        String sql = "UPDATE Telefonos SET telefono = ? WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, telefono.getTelefono());
            pstmt.setInt(2, telefono.getId());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ Teléfono actualizado correctamente");
                return true;
            } else {
                System.err.println("No se pudo actualizar el teléfono (ID no encontrado)");
            }
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar teléfono: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Eliminar un teléfono
     */
    public boolean eliminar(int id) {
        String sql = "DELETE FROM Telefonos WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ Teléfono eliminado correctamente");
                return true;
            } else {
                System.err.println("No se encontró teléfono con ID: " + id);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al eliminar teléfono: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Eliminar todos los teléfonos de una persona - VERSIÓN MEJORADA
     */
    public boolean eliminarPorPersonaId(int personaId) {
        String sql = "DELETE FROM Telefonos WHERE personaId = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, personaId);
            int affectedRows = pstmt.executeUpdate();
            
            System.out.println("Eliminados " + affectedRows + " teléfonos para persona ID: " + personaId);
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error al eliminar teléfonos para persona " + personaId + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
}