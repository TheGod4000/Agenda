/*
 * DAO para operaciones CRUD de Persona - Versión actualizada con múltiples direcciones
 */
package com.agenda;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * DAO para operaciones CRUD de Persona con soporte para múltiples direcciones
 */
public class PersonaDAO {
    private final DatabaseConnection dbConnection;
    private final TelefonoDAO telefonoDAO;
    private final PersonaDireccionDAO personaDireccionDAO;

    public PersonaDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.telefonoDAO = new TelefonoDAO();
        this.personaDireccionDAO = new PersonaDireccionDAO();
    }

    /**
     * Constructor para Inyección de Dependencias (para tests)
     */
    public PersonaDAO(DatabaseConnection dbConnection, TelefonoDAO telefonoDAO) {
        this.dbConnection = dbConnection;
        this.telefonoDAO = telefonoDAO;
        this.personaDireccionDAO = new PersonaDireccionDAO(dbConnection);
    }

    /**
     * Crear una nueva persona
     */
    public boolean crear(Persona persona) {
        String sql = "INSERT INTO Personas (nombre) VALUES (?)";

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

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

                                // Si la dirección no tiene ID, crearla primero
                                if (direccion.getId() == 0) {
                                    DireccionDAO direccionDAO = new DireccionDAO(dbConnection);
                                    if (!direccionDAO.crear(direccion)) {
                                        throw new SQLException("Error al crear dirección asociada");
                                    }
                                }

                                // Crear la relación
                                relacion.setPersonaId(persona.getId());
                                relacion.setDireccionId(direccion.getId());

                                if (!personaDireccionDAO.crear(relacion)) {
                                    throw new SQLException("Error al crear relación persona-dirección");
                                }
                            }

                            // Guardar teléfonos asociados (sin cambios)
                            for (Telefono telefono : persona.getTelefonos()) {
                                telefono.setPersonaId(persona.getId());
                                if (!telefonoDAO.crear(telefono)) {
                                    throw new SQLException("Error al crear teléfono asociado");
                                }
                            }

                            conn.commit();
                            System.out.println("✓ Persona creada con ID: " + persona.getId());
                            return true;
                        }
                    }
                } else {
                    conn.rollback();
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error al crear persona: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Obtener una persona por ID con todas sus direcciones y teléfonos
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
                            rs.getString("nombre")
                    );

                    // Cargar direcciones con sus relaciones
                    Map<Direccion, PersonaDireccion> direccionesConInfo =
                            personaDireccionDAO.obtenerDireccionesConInfoPorPersona(persona.getId());
                    persona.setRelacionesDireccion(direccionesConInfo);

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
     * Obtener todas las personas con sus direcciones y teléfonos
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

                    System.out.println("Cargando persona " + contador + ": ID=" + personaId + ", Nombre=" + nombre);

                    Persona persona = new Persona(personaId, nombre);

                    // Cargar direcciones con información de relaciones
                    Map<Direccion, PersonaDireccion> direccionesConInfo =
                            personaDireccionDAO.obtenerDireccionesConInfoPorPersona(personaId);
                    System.out.println("  - Direcciones encontradas: " + direccionesConInfo.size());

                    persona.setRelacionesDireccion(direccionesConInfo);

                    // Cargar teléfonos
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
     * Verificar el estado de todas las tablas cuando no se encuentran datos
     */
    private void verificarEstadoTablas(Connection conn) throws SQLException {
        System.out.println("No se encontraron personas. Verificando estado de las tablas...");

        try (Statement stmt = conn.createStatement()) {

            // Verificar tabla Personas
            ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'Personas'");
            if (!rs.next()) {
                System.err.println("⚠ La tabla 'Personas' no existe");
                return;
            }
            System.out.println("✓ Tabla 'Personas' existe");

            // Contar registros en cada tabla
            String[] tablas = {"Personas", "Direcciones", "PersonaDirecciones", "Telefonos"};

            for (String tabla : tablas) {
                rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabla);
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("Total de registros en " + tabla + ": " + count);
                }
            }
        }
    }

    /**
     * Actualizar una persona con sus direcciones y teléfonos
     */
    public boolean actualizar(Persona persona) {
        String sql = "UPDATE Personas SET nombre = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                System.out.println("Actualizando persona ID: " + persona.getId());

                // Actualizar datos básicos de la persona
                pstmt.setString(1, persona.getNombre());
                pstmt.setInt(2, persona.getId());

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {

                    // Eliminar relaciones persona-dirección existentes
                    personaDireccionDAO.eliminarPorPersonaId(persona.getId());

                    // Crear las nuevas relaciones persona-dirección
                    for (Map.Entry<Direccion, PersonaDireccion> entry : persona.getRelacionesDireccion().entrySet()) {
                        Direccion direccion = entry.getKey();
                        PersonaDireccion relacion = entry.getValue();

                        // Si la dirección no tiene ID, crearla primero
                        if (direccion.getId() == 0) {
                            DireccionDAO direccionDAO = new DireccionDAO(dbConnection);
                            if (!direccionDAO.crear(direccion)) {
                                throw new SQLException("Error al crear nueva dirección");
                            }
                        }

                        // Actualizar IDs en la relación
                        relacion.setPersonaId(persona.getId());
                        relacion.setDireccionId(direccion.getId());

                        if (!personaDireccionDAO.crear(relacion)) {
                            throw new SQLException("Error al crear relación persona-dirección actualizada");
                        }
                    }

                    // Eliminar y recrear teléfonos
                    telefonoDAO.eliminarPorPersonaId(persona.getId());

                    for (Telefono telefono : persona.getTelefonos()) {
                        telefono.setPersonaId(persona.getId());
                        if (!telefonoDAO.crear(telefono)) {
                            throw new SQLException("Error al crear teléfono actualizado");
                        }
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
     * Eliminar una persona y todas sus relaciones
     */
    public boolean eliminar(int id) {
        String sql = "DELETE FROM Personas WHERE id = ?";

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                System.out.println("Eliminando persona ID: " + id);

                // Las relaciones se eliminan automáticamente por CASCADE
                // pero podemos hacerlo explícitamente para tener control
                personaDireccionDAO.eliminarPorPersonaId(id);
                telefonoDAO.eliminarPorPersonaId(id);

                // Eliminar la persona
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, id);
                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        conn.commit();
                        System.out.println("✓ Persona eliminada correctamente");
                        return true;
                    } else {
                        conn.rollback();
                        System.err.println("No se encontró persona con ID: " + id);
                    }
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error al eliminar persona: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Buscar personas por nombre (con sus direcciones y teléfonos)
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

                    int personaId = rs.getInt("id");
                    String nombrePersona = rs.getString("nombre");

                    Persona persona = new Persona(personaId, nombrePersona);

                    // Cargar direcciones
                    Map<Direccion, PersonaDireccion> direccionesConInfo =
                            personaDireccionDAO.obtenerDireccionesConInfoPorPersona(personaId);
                    persona.setRelacionesDireccion(direccionesConInfo);

                    // Cargar teléfonos
                    List<Telefono> telefonos = telefonoDAO.obtenerPorPersonaId(personaId);
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

    /**
     * Buscar personas que comparten una dirección específica
     */
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
            System.out.println("Buscando personas que comparten la dirección ID: " + direccionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Persona persona = obtenerPorId(rs.getInt("id"));
                    if (persona != null) {
                        personas.add(persona);
                    }
                }

                System.out.println("Encontradas " + personas.size() + " personas que comparten la dirección");
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar personas por dirección: " + e.getMessage());
            e.printStackTrace();
        }

        return personas;
    }
}