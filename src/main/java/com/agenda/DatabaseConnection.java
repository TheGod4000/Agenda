/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.agenda;

/**
 *
 * @author Luisg
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manejo de conexiones a la base de datos
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private String url;
    private String username;
    private String password;

    private DatabaseConnection() {
        this.url = DatabaseConfig.URL;
        this.username = DatabaseConfig.USERNAME;
        this.password = DatabaseConfig.PASSWORD;
        
        // Cargar el driver al inicializar
        try {
            Class.forName(DatabaseConfig.DRIVER);
            System.out.println("Driver MySQL cargado correctamente");
        } catch (ClassNotFoundException e) {
            System.err.println("Error al cargar driver MySQL: " + e.getMessage());
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Obtiene una nueva conexión cada vez (no reutiliza conexiones)
     * Esto evita problemas con conexiones cerradas o timeout
     */
    public Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            
            // Configurar la conexión
            conn.setAutoCommit(true);
            
            return conn;
            
        } catch (SQLException e) {
            System.err.println("Error al conectar a la base de datos:");
            System.err.println("URL: " + url);
            System.err.println("Usuario: " + username);
            System.err.println("Error: " + e.getMessage());
            throw e;
        }
    }

    public void closeConnection() {
        // No necesitamos mantener una conexión persistente
        System.out.println("DatabaseConnection: Usando conexiones por demanda, no hay conexión persistente que cerrar");
    }

    // Método para configurar conexión de pruebas
    public void setTestConnection() {
        this.url = DatabaseConfig.TEST_URL;
        this.username = DatabaseConfig.TEST_USERNAME;
        this.password = DatabaseConfig.TEST_PASSWORD;
    }

    // Método para restaurar conexión normal
    public void setProductionConnection() {
        this.url = DatabaseConfig.URL;
        this.username = DatabaseConfig.USERNAME;
        this.password = DatabaseConfig.PASSWORD;
    }

    /**
     * Método para probar la conexión y crear las tablas si no existen
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✓ Conexión exitosa a la base de datos");
            
            // Verificar y crear tablas si no existen
            createTablesIfNotExist(conn);
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("✗ Error de conexión: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Crear las tablas si no existen
     */
    private void createTablesIfNotExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            
            // Crear tabla Personas si no existe
            String createPersonasTable = "CREATE TABLE IF NOT EXISTS Personas (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nombre VARCHAR(100) NOT NULL, " +
                    "direccion VARCHAR(200), " +
                    "fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            
            stmt.executeUpdate(createPersonasTable);
            
            // Crear tabla Telefonos si no existe
            String createTelefonosTable = "CREATE TABLE IF NOT EXISTS Telefonos (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "personaId INT NOT NULL, " +
                    "telefono VARCHAR(20) NOT NULL, " +
                    "fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (personaId) REFERENCES Personas(id) ON DELETE CASCADE, " +
                    "INDEX idx_persona (personaId)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            
            stmt.executeUpdate(createTelefonosTable);
            
            System.out.println("✓ Tablas verificadas/creadas correctamente");
            
        } catch (SQLException e) {
            System.err.println("Error al crear tablas: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Insertar datos de prueba si las tablas están vacías
     */
    public void insertTestDataIfEmpty() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Verificar si ya hay datos
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM Personas");
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Insertando datos de prueba...");
                
                // Insertar personas de prueba
                stmt.executeUpdate("INSERT INTO Personas (nombre, direccion) VALUES ('Juan Pérez', 'Calle Principal 123')");
                stmt.executeUpdate("INSERT INTO Personas (nombre, direccion) VALUES ('María García', 'Avenida Central 456')");
                stmt.executeUpdate("INSERT INTO Personas (nombre, direccion) VALUES ('Carlos López', 'Boulevard Norte 789')");
                
                // Insertar teléfonos de prueba
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (1, '555-0001')");
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (1, '555-0002')");
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (2, '555-0003')");
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (3, '555-0004')");
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (3, '555-0005')");
                
                System.out.println("✓ Datos de prueba insertados correctamente");
            }
            
        } catch (SQLException e) {
            System.err.println("Error al insertar datos de prueba: " + e.getMessage());
        }
    }
}