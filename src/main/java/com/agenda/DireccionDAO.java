/*
 * DAO para operaciones CRUD de Dirección
 */
package com.agenda;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones CRUD de Direcciones
 */
public class DireccionDAO implements IDireccionDAO { // <-- SE IMPLEMENTA LA INTERFAZ
    private final DatabaseConnection dbConnection;

    public DireccionDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public DireccionDAO(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Crear una nueva dirección
     */
    @Override // <-- SE AÑADE ANOTACIÓN
    public boolean crear(Direccion direccion) {
        String sql = "INSERT INTO Direcciones (calle, ciudad, estado, codigo_postal, pais) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            System.out.println("Creando dirección: " + direccion.getDireccionCompleta());

            pstmt.setString(1, direccion.getCalle());
            pstmt.setString(2, direccion.getCiudad());
            pstmt.setString(3, direccion.getEstado());
            pstmt.setString(4, direccion.getCodigoPostal());
            pstmt.setString(5, direccion.getPais());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        direccion.setId(generatedKeys.getInt(1));
                        System.out.println("✓ Dirección creada con ID: " + direccion.getId());
                        return true;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al crear dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Obtener una dirección por ID
     */
    @Override // <-- SE AÑADE ANOTACIÓN
    public Direccion obtenerPorId(int id) {
        String sql = "SELECT * FROM Direcciones WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Direccion(
                            rs.getInt("id"),
                            rs.getString("calle"),
                            rs.getString("ciudad"),
                            rs.getString("estado"),
                            rs.getString("codigo_postal"),
                            rs.getString("pais")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Obtener todas las direcciones
     */
    @Override // <-- SE AÑADE ANOTACIÓN
    public ObservableList<Direccion> obtenerTodas() {
        ObservableList<Direccion> direcciones = FXCollections.observableArrayList();
        String sql = "SELECT * FROM Direcciones ORDER BY ciudad, calle";

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Direccion direccion = new Direccion(
                        rs.getInt("id"),
                        rs.getString("calle"),
                        rs.getString("ciudad"),
                        rs.getString("estado"),
                        rs.getString("codigo_postal"),
                        rs.getString("pais")
                );
                direcciones.add(direccion);
            }

            System.out.println("Direcciones cargadas: " + direcciones.size());

        } catch (SQLException e) {
            System.err.println("Error al obtener direcciones: " + e.getMessage());
            e.printStackTrace();
        }

        return direcciones;
    }

    /**
     * Obtener direcciones de una persona específica
     */
    public List<Direccion> obtenerPorPersonaId(int personaId) {
        List<Direccion> direcciones = new ArrayList<>();
        String sql = """
            SELECT d.*, pd.etiqueta, pd.es_principal
            FROM Direcciones d
            INNER JOIN PersonaDirecciones pd ON d.id = pd.direccion_id
            WHERE pd.persona_id = ?
            ORDER BY pd.es_principal DESC, pd.etiqueta
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, personaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Direccion direccion = new Direccion(
                            rs.getInt("id"),
                            rs.getString("calle"),
                            rs.getString("ciudad"),
                            rs.getString("estado"),
                            rs.getString("codigo_postal"),
                            rs.getString("pais")
                    );
                    direcciones.add(direccion);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener direcciones para persona " + personaId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return direcciones;
    }

    /**
     * Actualizar una dirección
     */
    @Override // <-- SE AÑADE ANOTACIÓN
    public boolean actualizar(Direccion direccion) {
        String sql = "UPDATE Direcciones SET calle = ?, ciudad = ?, estado = ?, codigo_postal = ?, pais = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            System.out.println("Actualizando dirección ID: " + direccion.getId());

            pstmt.setString(1, direccion.getCalle());
            pstmt.setString(2, direccion.getCiudad());
            pstmt.setString(3, direccion.getEstado());
            pstmt.setString(4, direccion.getCodigoPostal());
            pstmt.setString(5, direccion.getPais());
            pstmt.setInt(6, direccion.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Dirección actualizada correctamente");
                return true;
            } else {
                System.err.println("No se pudo actualizar la dirección (no se encontró el ID)");
            }

        } catch (SQLException e) {
            System.err.println("Error al actualizar dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Eliminar una dirección
     */
    @Override // <-- SE AÑADE ANOTACIÓN
    public boolean eliminar(int id) {
        // Primero verificar si la dirección está siendo usada por alguna persona
        if (estaEnUso(id)) {
            System.err.println("No se puede eliminar la dirección porque está siendo usada por una o más personas");
            return false;
        }

        String sql = "DELETE FROM Direcciones WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            System.out.println("Eliminando dirección ID: " + id);

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Dirección eliminada correctamente");
                return true;
            } else {
                System.err.println("No se encontró dirección con ID: " + id);
            }

        } catch (SQLException e) {
            System.err.println("Error al eliminar dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Verificar si una dirección está siendo usada por alguna persona
     */
    @Override // <-- SE AÑADE ANOTACIÓN
    public boolean estaEnUso(int direccionId) {
        String sql = "SELECT COUNT(*) FROM PersonaDirecciones WHERE direccion_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, direccionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar uso de dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Buscar direcciones similares para evitar duplicados
     */
    @Override // <-- SE AÑADE ANOTACIÓN
    public List<Direccion> buscarSimilares(Direccion direccion) {
        List<Direccion> similares = new ArrayList<>();
        String sql = "SELECT * FROM Direcciones WHERE calle LIKE ? AND ciudad LIKE ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + direccion.getCalle() + "%");
            pstmt.setString(2, "%" + direccion.getCiudad() + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Direccion encontrada = new Direccion(
                            rs.getInt("id"),
                            rs.getString("calle"),
                            rs.getString("ciudad"),
                            rs.getString("estado"),
                            rs.getString("codigo_postal"),
                            rs.getString("pais")
                    );
                    similares.add(encontrada);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar direcciones similares: " + e.getMessage());
            e.printStackTrace();
        }

        return similares;
    }

    /**
     * Buscar direcciones por texto
     */
    @Override // <-- SE AÑADE ANOTACIÓN
    public ObservableList<Direccion> buscarPorTexto(String texto) {
        ObservableList<Direccion> direcciones = FXCollections.observableArrayList();
        String sql = """
            SELECT * FROM Direcciones
            WHERE calle LIKE ? OR ciudad LIKE ? OR estado LIKE ? OR codigo_postal LIKE ? OR pais LIKE ?
            ORDER BY ciudad, calle
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String busqueda = "%" + texto + "%";
            for (int i = 1; i <= 5; i++) {
                pstmt.setString(i, busqueda);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Direccion direccion = new Direccion(
                            rs.getInt("id"),
                            rs.getString("calle"),
                            rs.getString("ciudad"),
                            rs.getString("estado"),
                            rs.getString("codigo_postal"),
                            rs.getString("pais")
                    );
                    direcciones.add(direccion);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar direcciones: " + e.getMessage());
            e.printStackTrace();
        }

        return direcciones;
    }
    public boolean crearConConexion(Direccion direccion, Connection conn) throws SQLException {
        String sql = "INSERT INTO Direcciones (calle, ciudad, estado, codigo_postal, pais) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            System.out.println("Creando dirección con conexión existente: " + direccion.getDireccionCompleta());

            pstmt.setString(1, direccion.getCalle());
            pstmt.setString(2, direccion.getCiudad());
            pstmt.setString(3, direccion.getEstado());
            pstmt.setString(4, direccion.getCodigoPostal());
            pstmt.setString(5, direccion.getPais());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        direccion.setId(generatedKeys.getInt(1));
                        System.out.println("✓ Dirección creada con ID: " + direccion.getId());
                        return true;
                    }
                }
            }

            return false;
        }
    }
}