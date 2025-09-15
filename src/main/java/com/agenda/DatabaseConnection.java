/*
 * Manejo de conexiones a la base de datos - Versión actualizada con nuevas tablas
 */
package com.agenda;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manejo de conexiones a la base de datos con soporte para múltiples direcciones
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

    public Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(url, username, password);
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
        System.out.println("DatabaseConnection: Usando conexiones por demanda, no hay conexión persistente que cerrar");
    }

    public void setTestConnection() {
        this.url = DatabaseConfig.TEST_URL;
        this.username = DatabaseConfig.TEST_USERNAME;
        this.password = DatabaseConfig.TEST_PASSWORD;
    }

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
     * Crear todas las tablas si no existen - VERSIÓN ACTUALIZADA
     */
    private void createTablesIfNotExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // 1. Crear tabla Personas (sin cambios)
            String createPersonasTable = """
                CREATE TABLE IF NOT EXISTS Personas (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nombre VARCHAR(100) NOT NULL,
                    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
            stmt.executeUpdate(createPersonasTable);

            // 2. Crear tabla Direcciones (nueva)
            String createDireccionesTable = """
                CREATE TABLE IF NOT EXISTS Direcciones (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    calle VARCHAR(200),
                    ciudad VARCHAR(100),
                    estado VARCHAR(100),
                    codigo_postal VARCHAR(20),
                    pais VARCHAR(100),
                    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_ciudad (ciudad),
                    INDEX idx_codigo_postal (codigo_postal)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
            stmt.executeUpdate(createDireccionesTable);

            // 3. Crear tabla PersonaDirecciones (relación muchos a muchos)
            String createPersonaDireccionesTable = """
                CREATE TABLE IF NOT EXISTS PersonaDirecciones (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    persona_id INT NOT NULL,
                    direccion_id INT NOT NULL,
                    etiqueta VARCHAR(50) DEFAULT 'Principal',
                    es_principal BOOLEAN DEFAULT FALSE,
                    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (persona_id) REFERENCES Personas(id) ON DELETE CASCADE,
                    FOREIGN KEY (direccion_id) REFERENCES Direcciones(id) ON DELETE CASCADE,
                    UNIQUE KEY unique_persona_direccion (persona_id, direccion_id),
                    INDEX idx_persona (persona_id),
                    INDEX idx_direccion (direccion_id),
                    INDEX idx_principal (persona_id, es_principal)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
            stmt.executeUpdate(createPersonaDireccionesTable);

            // 4. Crear tabla Telefonos (sin cambios)
            String createTelefonosTable = """
                CREATE TABLE IF NOT EXISTS Telefonos (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    personaId INT NOT NULL,
                    telefono VARCHAR(20) NOT NULL,
                    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (personaId) REFERENCES Personas(id) ON DELETE CASCADE,
                    INDEX idx_persona (personaId)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
            stmt.executeUpdate(createTelefonosTable);

            System.out.println("✓ Todas las tablas verificadas/creadas correctamente");

            // Verificar si necesitamos migrar datos existentes
            migrarDatosExistentes(conn);

        } catch (SQLException e) {
            System.err.println("Error al crear tablas: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Migrar datos existentes de la tabla Personas si tiene columna 'direccion'
     */
    private void migrarDatosExistentes(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // Verificar si la tabla Personas tiene la columna 'direccion' antigua
            boolean tieneColumnaAntigua = false;
            try {
                var rs = stmt.executeQuery("SHOW COLUMNS FROM Personas LIKE 'direccion'");
                tieneColumnaAntigua = rs.next();
            } catch (SQLException e) {
                // Tabla no existe o no tiene la columna, no hay problema
            }

            if (tieneColumnaAntigua) {
                System.out.println("Detectada columna 'direccion' antigua. Iniciando migración...");

                // Obtener personas con direcciones antiguas
                var rs = stmt.executeQuery("SELECT id, direccion FROM Personas WHERE direccion IS NOT NULL AND direccion != ''");

                int personasMigradas = 0;
                while (rs.next()) {
                    int personaId = rs.getInt("id");
                    String direccionAntigua = rs.getString("direccion");

                    if (direccionAntigua != null && !direccionAntigua.trim().isEmpty()) {
                        // Crear nueva dirección
                        String insertDireccion = "INSERT INTO Direcciones (calle, ciudad, estado, codigo_postal, pais) VALUES (?, '', '', '', '')";
                        try (var pstmt = conn.prepareStatement(insertDireccion, Statement.RETURN_GENERATED_KEYS)) {
                            pstmt.setString(1, direccionAntigua);
                            pstmt.executeUpdate();

                            try (var keys = pstmt.getGeneratedKeys()) {
                                if (keys.next()) {
                                    int direccionId = keys.getInt(1);

                                    // Crear relación persona-dirección
                                    String insertRelacion = "INSERT INTO PersonaDirecciones (persona_id, direccion_id, etiqueta, es_principal) VALUES (?, ?, 'Principal', true)";
                                    try (var pstmt2 = conn.prepareStatement(insertRelacion)) {
                                        pstmt2.setInt(1, personaId);
                                        pstmt2.setInt(2, direccionId);
                                        pstmt2.executeUpdate();

                                        personasMigradas++;
                                    }
                                }
                            }
                        }
                    }
                }

                if (personasMigradas > 0) {
                    System.out.println("✓ Migradas " + personasMigradas + " direcciones al nuevo sistema");

                    // Opcional: Eliminar la columna antigua (comentado por seguridad)
                    // stmt.executeUpdate("ALTER TABLE Personas DROP COLUMN direccion");
                    // System.out.println("✓ Columna 'direccion' antigua eliminada");
                } else {
                    System.out.println("No se encontraron direcciones para migrar");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error durante la migración de datos: " + e.getMessage());
            // No re-lanzamos la excepción para que no falle la inicialización
        }
    }

    /**
     * Insertar datos de prueba si las tablas están vacías - VERSIÓN ACTUALIZADA
     */
    public void insertTestDataIfEmpty() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Verificar si ya hay datos
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM Personas");
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Insertando datos de prueba con nuevas direcciones...");

                // Insertar personas de prueba
                stmt.executeUpdate("INSERT INTO Personas (nombre) VALUES ('Juan Pérez')");
                stmt.executeUpdate("INSERT INTO Personas (nombre) VALUES ('María García')");
                stmt.executeUpdate("INSERT INTO Personas (nombre) VALUES ('Carlos López')");

                // Insertar direcciones de prueba
                stmt.executeUpdate("INSERT INTO Direcciones (calle, ciudad, estado, codigo_postal, pais) VALUES ('Calle Principal 123', 'Mexicali', 'Baja California', '21000', 'México')");
                stmt.executeUpdate("INSERT INTO Direcciones (calle, ciudad, estado, codigo_postal, pais) VALUES ('Avenida Central 456', 'Tijuana', 'Baja California', '22000', 'México')");
                stmt.executeUpdate("INSERT INTO Direcciones (calle, ciudad, estado, codigo_postal, pais) VALUES ('Boulevard Norte 789', 'Ensenada', 'Baja California', '22800', 'México')");
                stmt.executeUpdate("INSERT INTO Direcciones (calle, ciudad, estado, codigo_postal, pais) VALUES ('Oficina Central', 'Ciudad de México', 'CDMX', '03100', 'México')");

                // Insertar relaciones persona-dirección
                stmt.executeUpdate("INSERT INTO PersonaDirecciones (persona_id, direccion_id, etiqueta, es_principal) VALUES (1, 1, 'Casa', true)");
                stmt.executeUpdate("INSERT INTO PersonaDirecciones (persona_id, direccion_id, etiqueta, es_principal) VALUES (1, 4, 'Trabajo', false)");
                stmt.executeUpdate("INSERT INTO PersonaDirecciones (persona_id, direccion_id, etiqueta, es_principal) VALUES (2, 2, 'Casa', true)");
                stmt.executeUpdate("INSERT INTO PersonaDirecciones (persona_id, direccion_id, etiqueta, es_principal) VALUES (2, 4, 'Oficina', false)"); // María también trabaja en la oficina central
                stmt.executeUpdate("INSERT INTO PersonaDirecciones (persona_id, direccion_id, etiqueta, es_principal) VALUES (3, 3, 'Residencia', true)");

                // Insertar teléfonos de prueba (sin cambios)
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (1, '555-0001')");
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (1, '555-0002')");
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (2, '555-0003')");
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (3, '555-0004')");
                stmt.executeUpdate("INSERT INTO Telefonos (personaId, telefono) VALUES (3, '555-0005')");

                System.out.println("✓ Datos de prueba insertados correctamente");
                System.out.println("  - 3 personas creadas");
                System.out.println("  - 4 direcciones creadas");
                System.out.println("  - 5 relaciones persona-dirección creadas");
                System.out.println("  - 5 teléfonos creados");
                System.out.println("  - María García y Juan Pérez comparten la dirección de la oficina central");
            }

        } catch (SQLException e) {
            System.err.println("Error al insertar datos de prueba: " + e.getMessage());
        }
    }

}