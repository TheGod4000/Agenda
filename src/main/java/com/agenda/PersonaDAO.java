/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.agenda;

/**
 *
 * @author Luisg
 */
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de Persona - Versión Mejorada
 */
public class PersonaDAO {
    private final DatabaseConnection dbConnection;
    private final TelefonoDAO telefonoDAO;

    public PersonaDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.telefonoDAO = new TelefonoDAO();
    }
    /**
     * Nuevo constructor para Inyección de Dependencias.
     * Este es el que usaremos (indirectamente) en los tests.
     * Permite "inyectar" mocks o versiones de prueba de las dependencias.
     */
    public PersonaDAO(DatabaseConnection dbConnection, TelefonoDAO telefonoDAO) {
        this.dbConnection = dbConnection;
        this.telefonoDAO = telefonoDAO;
    }
    /**
     * Crear una nueva persona
     */
    public boolean crear(Persona persona) {
        String sql = "INSERT INTO Personas (nombre, direccion) VALUES (?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            System.out.println("Creando persona: " + persona.getNombre());
            
            pstmt.setString(1, persona.getNombre());
            pstmt.setString(2, persona.getDireccion());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        persona.setId(generatedKeys.getInt(1));
                        
                        // Guardar teléfonos asociados
                        for (Telefono telefono : persona.getTelefonos()) {
                            telefono.setPersonaId(persona.getId());
                            telefonoDAO.crear(telefono);
                        }
                        
                        System.out.println("✓ Persona creada con ID: " + persona.getId());
                        return true;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error al crear persona: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Obtener una persona por ID
     */
    public Persona obtenerPorId(int id) {
        String sql = "SELECT * FROM Personas WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Persona persona = new Persona(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("direccion")
                    );
                    
                    // Cargar teléfonos
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

    /**
     * Obtener todas las personas - VERSIÓN MEJORADA
     */
    public ObservableList<Persona> obtenerTodas() {
        ObservableList<Persona> personas = FXCollections.observableArrayList();
        String sql = "SELECT * FROM Personas ORDER BY nombre";
        
        try (Connection conn = dbConnection.getConnection()) {
            System.out.println("Conectado a la base de datos, obteniendo personas...");
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                int contador = 0;
                while (rs.next()) {
                    contador++;
                    
                    int personaId = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    String direccion = rs.getString("direccion");
                    
                    System.out.println("Cargando persona " + contador + ": ID=" + personaId + ", Nombre=" + nombre);
                    
                    Persona persona = new Persona(personaId, nombre, direccion);
                    
                    // Cargar teléfonos para esta persona
                    List<Telefono> telefonos = telefonoDAO.obtenerPorPersonaId(personaId);
                    System.out.println("  - Teléfonos encontrados: " + telefonos.size());
                    
                    persona.getTelefonos().clear();
                    persona.getTelefonos().addAll(telefonos);
                    
                    personas.add(persona);
                }
                
                System.out.println("Total de personas cargadas: " + contador);
                
                // Si no hay datos, verificar si las tablas existen y tienen datos
                if (contador == 0) {
                    verificarEstadoTablas(conn);
                }
                
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener personas: " + e.getMessage());
            e.printStackTrace();
        }
        
        return personas;
    }

    /**
     * Método para verificar el estado de las tablas cuando no se encuentran datos
     */
    private void verificarEstadoTablas(Connection conn) throws SQLException {
        System.out.println("No se encontraron personas. Verificando estado de las tablas...");
        
        try (Statement stmt = conn.createStatement()) {
            
            // Verificar si la tabla Personas existe
            ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'Personas'");
            if (!rs.next()) {
                System.err.println("⚠ La tabla 'Personas' no existe");
                return;
            }
            System.out.println("✓ Tabla 'Personas' existe");
            
            // Contar registros en Personas
            rs = stmt.executeQuery("SELECT COUNT(*) FROM Personas");
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("Total de registros en Personas: " + count);
                
                if (count == 0) {
                    System.out.println("La tabla está vacía. Puede insertar datos de prueba.");
                }
            }
            
            // Verificar tabla Telefonos
            rs = stmt.executeQuery("SHOW TABLES LIKE 'Telefonos'");
            if (rs.next()) {
                System.out.println("✓ Tabla 'Telefonos' existe");
                
                rs = stmt.executeQuery("SELECT COUNT(*) FROM Telefonos");
                if (rs.next()) {
                    System.out.println("Total de registros en Telefonos: " + rs.getInt(1));
                }
            } else {
                System.err.println("⚠ La tabla 'Telefonos' no existe");
            }
        }
    }

    /**
     * Actualizar una persona
     */
    public boolean actualizar(Persona persona) {
        String sql = "UPDATE Personas SET nombre = ?, direccion = ? WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                System.out.println("Actualizando persona ID: " + persona.getId());
                
                // Actualizar datos de la persona
                pstmt.setString(1, persona.getNombre());
                pstmt.setString(2, persona.getDireccion());
                pstmt.setInt(3, persona.getId());
                
                int affectedRows = pstmt.executeUpdate();
                
                if (affectedRows > 0) {
                    // Eliminar teléfonos existentes y crear los nuevos
                    telefonoDAO.eliminarPorPersonaId(persona.getId());
                    
                    for (Telefono telefono : persona.getTelefonos()) {
                        telefono.setPersonaId(persona.getId());
                        telefonoDAO.crear(telefono);
                    }
                    
                    conn.commit();
                    System.out.println("✓ Persona actualizada correctamente");
                    return true;
                } else {
                    conn.rollback();
                    System.err.println("No se pudo actualizar la persona (no se encontró el ID)");
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar persona: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Eliminar una persona
     */
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

    /**
     * Buscar personas por nombre
     */
    public ObservableList<Persona> buscarPorNombre(String nombre) {
        ObservableList<Persona> personas = FXCollections.observableArrayList();
        String sql = "SELECT * FROM Personas WHERE nombre LIKE ? ORDER BY nombre";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String busqueda = "%" + nombre + "%";
            pstmt.setString(1, busqueda);
            System.out.println("Buscando personas con nombre como: " + busqueda);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                int contador = 0;
                while (rs.next()) {
                    contador++;
                    
                    Persona persona = new Persona(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("direccion")
                    );
                    
                    // Cargar teléfonos
                    List<Telefono> telefonos = telefonoDAO.obtenerPorPersonaId(persona.getId());
                    persona.getTelefonos().addAll(telefonos);
                    
                    personas.add(persona);
                }
                
                System.out.println("Encontradas " + contador + " personas que coinciden con: " + nombre);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al buscar personas: " + e.getMessage());
            e.printStackTrace();
        }
        
        return personas;
    }
}