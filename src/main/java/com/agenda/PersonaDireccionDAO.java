/*
 * DAO para operaciones CRUD de la relación PersonaDireccion
 */
package com.agenda;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para manejar la relación muchos-a-muchos entre Persona y Dirección
 */
public class PersonaDireccionDAO {
    private final DatabaseConnection dbConnection;

    public PersonaDireccionDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public PersonaDireccionDAO(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Crear una nueva relación persona-dirección
     */
    public boolean crear(PersonaDireccion personaDireccion) {
        String sql = "INSERT INTO PersonaDirecciones (persona_id, direccion_id, etiqueta, es_principal) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            System.out.println("Creando relación persona-dirección: " +
                    personaDireccion.getPersonaId() + " -> " + personaDireccion.getDireccionId());

            if (personaDireccion.isEsPrincipal()) {
                quitarPrincipalAnterior(personaDireccion.getPersonaId());
            }

            pstmt.setInt(1, personaDireccion.getPersonaId());
            pstmt.setInt(2, personaDireccion.getDireccionId());
            pstmt.setString(3, personaDireccion.getEtiqueta());
            pstmt.setBoolean(4, personaDireccion.isEsPrincipal());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        personaDireccion.setId(generatedKeys.getInt(1));
                        System.out.println("✓ Relación persona-dirección creada con ID: " + personaDireccion.getId());
                        return true;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al crear relación persona-dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private void quitarPrincipalAnterior(int personaId) throws SQLException {
        String sql = "UPDATE PersonaDirecciones SET es_principal = false WHERE persona_id = ? AND es_principal = true";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, personaId);
            int updated = pstmt.executeUpdate();

            if (updated > 0) {
                System.out.println("Removido flag principal de " + updated + " direcciones anteriores");
            }
        }
    }

    /**
     * Obtener todas las relaciones de una persona
     */
    public List<PersonaDireccion> obtenerPorPersonaId(int personaId) {
        List<PersonaDireccion> relaciones = new ArrayList<>();
        String sql = "SELECT * FROM PersonaDirecciones WHERE persona_id = ? ORDER BY es_principal DESC, etiqueta";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, personaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PersonaDireccion relacion = new PersonaDireccion(
                            rs.getInt("id"),
                            rs.getInt("persona_id"),
                            rs.getInt("direccion_id"),
                            rs.getString("etiqueta"),
                            rs.getBoolean("es_principal")
                    );
                    relaciones.add(relacion);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener relaciones para persona " + personaId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return relaciones;
    }

    public boolean actualizar(PersonaDireccion personaDireccion) {
        String sql = "UPDATE PersonaDirecciones SET etiqueta = ?, es_principal = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (personaDireccion.isEsPrincipal()) {
                quitarPrincipalAnterior(personaDireccion.getPersonaId());
            }

            System.out.println("Actualizando relación persona-dirección ID: " + personaDireccion.getId());

            pstmt.setString(1, personaDireccion.getEtiqueta());
            pstmt.setBoolean(2, personaDireccion.isEsPrincipal());
            pstmt.setInt(3, personaDireccion.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Relación persona-dirección actualizada correctamente");
                return true;
            } else {
                System.err.println("No se pudo actualizar la relación (no se encontró el ID)");
            }

        } catch (SQLException e) {
            System.err.println("Error al actualizar relación persona-dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM PersonaDirecciones WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            System.out.println("Eliminando relación persona-dirección ID: " + id);

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✓ Relación persona-dirección eliminada correctamente");
                return true;
            } else {
                System.err.println("No se encontró relación con ID: " + id);
            }

        } catch (SQLException e) {
            System.err.println("Error al eliminar relación persona-dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean eliminarPorPersonaId(int personaId) {
        String sql = "DELETE FROM PersonaDirecciones WHERE persona_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            System.out.println("Eliminando todas las direcciones para persona ID: " + personaId);

            pstmt.setInt(1, personaId);
            int affectedRows = pstmt.executeUpdate();

            System.out.println("Eliminadas " + affectedRows + " relaciones persona-dirección");
            return true;

        } catch (SQLException e) {
            System.err.println("Error al eliminar relaciones para persona " + personaId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean existeRelacion(int personaId, int direccionId) {
        String sql = "SELECT COUNT(*) FROM PersonaDirecciones WHERE persona_id = ? AND direccion_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, personaId);
            pstmt.setInt(2, direccionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar existencia de relación: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public Direccion obtenerDireccionPrincipal(int personaId) {
        String sql = """
            SELECT d.* FROM Direcciones d 
            INNER JOIN PersonaDirecciones pd ON d.id = pd.direccion_id 
            WHERE pd.persona_id = ? AND pd.es_principal = true
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, personaId);

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
            System.err.println("Error al obtener dirección principal para persona " + personaId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public int contarPersonasPorDireccion(int direccionId) {
        String sql = "SELECT COUNT(DISTINCT persona_id) FROM PersonaDirecciones WHERE direccion_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, direccionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al contar personas para dirección " + direccionId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    public boolean establecerComoPrincipal(int personaId, int direccionId) {
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                quitarPrincipalAnterior(personaId);

                String sql = "UPDATE PersonaDirecciones SET es_principal = true WHERE persona_id = ? AND direccion_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, personaId);
                    pstmt.setInt(2, direccionId);

                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        conn.commit();
                        System.out.println("✓ Dirección establecida como principal");
                        return true;
                    } else {
                        conn.rollback();
                        System.err.println("No se encontró la relación persona-dirección");
                        return false;
                    }
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error al establecer dirección como principal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Obtener todas las personas que comparten una dirección
     */
    public List<PersonaDireccion> obtenerPorDireccionId(int direccionId) {
        List<PersonaDireccion> relaciones = new ArrayList<>();
        String sql = "SELECT * FROM PersonaDirecciones WHERE direccion_id = ? ORDER BY etiqueta";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, direccionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PersonaDireccion relacion = new PersonaDireccion(
                            rs.getInt("id"),
                            rs.getInt("persona_id"),
                            rs.getInt("direccion_id"),
                            rs.getString("etiqueta"),
                            rs.getBoolean("es_principal")
                    );
                    relaciones.add(relacion);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener personas para dirección " + direccionId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return relaciones;
    }

    /**
     * Obtener direcciones con información adicional para una persona
     */
    public Map<Direccion, PersonaDireccion> obtenerDireccionesConInfoPorPersona(int personaId) {
        Map<Direccion, PersonaDireccion> resultado = new HashMap<>();
        String sql = """
            SELECT d.*, pd.id as pd_id, pd.etiqueta, pd.es_principal, pd.persona_id, pd.direccion_id
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

                    PersonaDireccion relacion = new PersonaDireccion(
                            rs.getInt("pd_id"),
                            rs.getInt("persona_id"),
                            rs.getInt("direccion_id"),
                            rs.getString("etiqueta"),
                            rs.getBoolean("es_principal")
                    );

                    resultado.put(direccion, relacion);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener direcciones con info para persona " + personaId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return resultado;
    }
    /**
     * Crear relación persona-dirección usando una conexión proporcionada (para transacciones)
     */
    public boolean crearConConexion(PersonaDireccion personaDireccion, Connection conn) throws SQLException {
        String sql = "INSERT INTO PersonaDirecciones (persona_id, direccion_id, etiqueta, es_principal) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            System.out.println("Creando relación persona-dirección en transacción: " +
                    personaDireccion.getPersonaId() + " -> " + personaDireccion.getDireccionId());

            if (personaDireccion.isEsPrincipal()) {
                quitarPrincipalAnteriorConConexion(personaDireccion.getPersonaId(), conn);
            }

            pstmt.setInt(1, personaDireccion.getPersonaId());
            pstmt.setInt(2, personaDireccion.getDireccionId());
            pstmt.setString(3, personaDireccion.getEtiqueta());
            pstmt.setBoolean(4, personaDireccion.isEsPrincipal());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        personaDireccion.setId(generatedKeys.getInt(1));
                        System.out.println("✓ Relación persona-dirección creada con ID: " + personaDireccion.getId());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Eliminar relaciones por persona usando una conexión proporcionada (para transacciones)
     */
    public boolean eliminarPorPersonaIdConConexion(int personaId, Connection conn) throws SQLException {
        String sql = "DELETE FROM PersonaDirecciones WHERE persona_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            System.out.println("Eliminando todas las direcciones para persona ID en transacción: " + personaId);

            pstmt.setInt(1, personaId);
            int affectedRows = pstmt.executeUpdate();

            System.out.println("Eliminadas " + affectedRows + " relaciones persona-dirección en transacción");
            return true;
        }
    }

    /**
     * Quitar principal anterior usando una conexión proporcionada (para transacciones)
     */
    private void quitarPrincipalAnteriorConConexion(int personaId, Connection conn) throws SQLException {
        String sql = "UPDATE PersonaDirecciones SET es_principal = false WHERE persona_id = ? AND es_principal = true";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, personaId);
            int updated = pstmt.executeUpdate();

            if (updated > 0) {
                System.out.println("Removido flag principal de " + updated + " direcciones anteriores en transacción");
            }
        }
    }
}

