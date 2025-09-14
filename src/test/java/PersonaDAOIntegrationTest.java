/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Luisg
 */

import com.agenda.DatabaseConnection;
import com.agenda.Persona;
import com.agenda.PersonaDAO;
import com.agenda.Telefono;
import com.agenda.TelefonoDAO;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Pruebas de integración para PersonaDAO
 * Requiere una base de datos MySQL de pruebas configurada
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PersonaDAOIntegrationTest {

    private static PersonaDAO personaDAO;
    private static TelefonoDAO telefonoDAO;
    private static DatabaseConnection dbConnection;

    @BeforeAll
    static void setUpClass() throws Exception {
        // Configurar conexión de pruebas
        dbConnection = DatabaseConnection.getInstance();
        dbConnection.setTestConnection();

        // Verificar conexión
        if (!dbConnection.testConnection()) {
            throw new RuntimeException("No se pudo conectar a la base de datos de pruebas");
        }

        personaDAO = new PersonaDAO();
        telefonoDAO = new TelefonoDAO();

        // Crear tablas de prueba si no existen
        crearTablasPrueba();
    }

    @AfterAll
    static void tearDownClass() {
        // Restaurar conexión de producción
        dbConnection.setProductionConnection();
        dbConnection.closeConnection();
    }

    @BeforeEach
    void setUp() {
        // Limpiar tablas antes de cada prueba
        limpiarTablas();
    }

    private static void crearTablasPrueba() throws SQLException {
        Connection conn = dbConnection.getConnection();
        Statement stmt = conn.createStatement();

        try {
            // Crear tabla Personas si no existe
            stmt.execute("CREATE TABLE IF NOT EXISTS Personas (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nombre VARCHAR(100) NOT NULL, " +
                    "direccion VARCHAR(200))");

            // Crear tabla Telefonos si no existe
            stmt.execute("CREATE TABLE IF NOT EXISTS Telefonos (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "personaId INT NOT NULL, " +
                    "telefono VARCHAR(20) NOT NULL, " +
                    "FOREIGN KEY (personaId) REFERENCES Personas(id) " +
                    "ON DELETE CASCADE ON UPDATE CASCADE)");

        } finally {
            stmt.close();
        }
    }

    private void limpiarTablas() {
        try {
            Connection conn = dbConnection.getConnection();
            Statement stmt = conn.createStatement();

            // Deshabilitar verificaciones de claves foráneas temporalmente
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("DELETE FROM Telefonos");
            stmt.execute("DELETE FROM Personas");
            stmt.execute("ALTER TABLE Personas AUTO_INCREMENT = 1");
            stmt.execute("ALTER TABLE Telefonos AUTO_INCREMENT = 1");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error al limpiar tablas: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    void testCrearPersonaSinTelefonos() {
        // Arrange
        Persona persona = new Persona("Juan Pérez", "Calle Principal 123");

        // Act
        boolean resultado = personaDAO.crear(persona);

        // Assert
        Assertions.assertTrue(resultado);
       Assertions.assertTrue(persona.getId() > 0); // Verificar que se asignó un ID
    }

    @Test
    @Order(2)
    void testCrearPersonaConTelefonos() {
        // Arrange
        Persona persona = new Persona("María García", "Avenida Central 456");
        persona.addTelefono(new Telefono(0, "555-1234567"));
        persona.addTelefono(new Telefono(0, "666-7654321"));

        // Act
        boolean resultado = personaDAO.crear(persona);

        // Assert
        Assertions.assertTrue(resultado);
        Assertions.assertTrue(persona.getId() > 0);
        Assertions.assertEquals(2, persona.getTelefonos().size());
        
        // Verificar que los teléfonos tienen IDs asignados
        for (Telefono tel : persona.getTelefonos()) {
            Assertions.assertTrue(tel.getId() > 0);
            Assertions.assertEquals(persona.getId(), tel.getPersonaId());
        }
    }

    @Test
    @Order(3)
    void testObtenerPersonaPorId() {
        // Arrange - Crear persona primero
        Persona personaOriginal = new Persona("Carlos López", "Boulevard Norte 789");
        personaOriginal.addTelefono(new Telefono(0, "777-1111111"));
        personaDAO.crear(personaOriginal);

        // Act
        Persona personaObtenida = personaDAO.obtenerPorId(personaOriginal.getId());

        // Assert
        Assertions.assertNotNull(personaObtenida);
        Assertions.assertEquals("Carlos López", personaObtenida.getNombre());
        Assertions.assertEquals("Boulevard Norte 789", personaObtenida.getDireccion());
        Assertions.assertEquals(1, personaObtenida.getTelefonos().size());
        Assertions.assertEquals("777-1111111", personaObtenida.getTelefonos().get(0).getTelefono());
    }

    @Test
    @Order(4)
    void testObtenerPersonaNoExistente() {
        // Act
        Persona persona = personaDAO.obtenerPorId(999);

        // Assert
        Assertions.assertNull(persona);
    }

    @Test
    @Order(5)
    void testObtenerTodasPersonas() {
        // Arrange - Crear varias personas
        Persona persona1 = new Persona("Ana Martínez", "Calle A");
        Persona persona2 = new Persona("Pedro Rodríguez", "Calle B");
        personaDAO.crear(persona1);
        personaDAO.crear(persona2);

        // Act
        ObservableList<Persona> personas = personaDAO.obtenerTodas();

        // Assert
        Assertions.assertNotNull(personas);
        Assertions.assertEquals(2, personas.size());
        
        // Verificar que están ordenadas por nombre
        Assertions.assertTrue(personas.get(0).getNombre().compareTo(personas.get(1).getNombre()) <= 0);
    }

    @Test
    @Order(6)
    void testActualizarPersona() {
        // Arrange - Crear persona
        Persona personaOriginal = new Persona("Luis González", "Dirección Original");
        personaOriginal.addTelefono(new Telefono(0, "888-2222222"));
        personaDAO.crear(personaOriginal);

        // Modificar datos
        personaOriginal.setNombre("Luis González Actualizado");
        personaOriginal.setDireccion("Nueva Dirección");
        personaOriginal.clearTelefonos();
        personaOriginal.addTelefono(new Telefono(0, "999-3333333"));
        personaOriginal.addTelefono(new Telefono(0, "111-4444444"));

        // Act
        boolean resultado = personaDAO.actualizar(personaOriginal);

        // Assert
        Assertions.assertTrue(resultado);

        // Verificar cambios en la base de datos
        Persona personaActualizada = personaDAO.obtenerPorId(personaOriginal.getId());
        Assertions.assertEquals("Luis González Actualizado", personaActualizada.getNombre());
        Assertions.assertEquals("Nueva Dirección", personaActualizada.getDireccion());
        Assertions.assertEquals(2, personaActualizada.getTelefonos().size());
    }

    @Test
    @Order(7)
    void testEliminarPersona() {
        // Arrange - Crear persona con teléfonos
        Persona persona = new Persona("Elena Ruiz", "Calle Temporal");
        persona.addTelefono(new Telefono(0, "555-9999999"));
        personaDAO.crear(persona);

        int personaId = persona.getId();

        // Act
        boolean resultado = personaDAO.eliminar(personaId);

        // Assert
        Assertions.assertTrue(resultado);
        
        // Verificar que la persona ya no existe
        Persona personaEliminada = personaDAO.obtenerPorId(personaId);
        Assertions.assertNull(personaEliminada);

        // Verificar que los teléfonos también se eliminaron (CASCADE)
        Assertions.assertTrue(telefonoDAO.obtenerPorPersonaId(personaId).isEmpty());
    }

    @Test
    @Order(8)
    void testBuscarPersonasPorNombre() {
        // Arrange - Crear personas con nombres similares
        Persona persona1 = new Persona("Juan Carlos", "Calle 1");
        Persona persona2 = new Persona("Juan Pablo", "Calle 2");
        Persona persona3 = new Persona("María Juan", "Calle 3");
        Persona persona4 = new Persona("Pedro Sánchez", "Calle 4");

        personaDAO.crear(persona1);
        personaDAO.crear(persona2);
        personaDAO.crear(persona3);
        personaDAO.crear(persona4);

        // Act - Buscar por "Juan"
        ObservableList<Persona> resultados = personaDAO.buscarPorNombre("Juan");

        // Assert
        Assertions.assertEquals(3, resultados.size()); // Debería encontrar 3 personas que contienen "Juan"
        
        boolean encontroJuanCarlos = false;
        boolean encontroJuanPablo = false;
        boolean encontroMariaJuan = false;
        
        for (Persona p : resultados) {
            if (p.getNombre().equals("Juan Carlos")) encontroJuanCarlos = true;
            if (p.getNombre().equals("Juan Pablo")) encontroJuanPablo = true;
            if (p.getNombre().equals("María Juan")) encontroMariaJuan = true;
        }
        
        Assertions.assertTrue(encontroJuanCarlos);
        Assertions.assertTrue(encontroJuanPablo);
        Assertions.assertTrue(encontroMariaJuan);
    }

    @Test
    @Order(9)
    void testIntegridadReferencial() {
        // Arrange - Crear persona con teléfonos
        Persona persona = new Persona("Test Integridad", "Test Address");
        persona.addTelefono(new Telefono(0, "111-1111111"));
        persona.addTelefono(new Telefono(0, "222-2222222"));
        personaDAO.crear(persona);

        // Verificar que los teléfonos se crearon
        Assertions.assertEquals(2, telefonoDAO.obtenerPorPersonaId(persona.getId()).size());

        // Act - Eliminar persona (debe eliminar teléfonos por CASCADE)
        boolean eliminado = personaDAO.eliminar(persona.getId());

        // Assert
        Assertions.assertTrue(eliminado);
        Assertions.assertTrue(telefonoDAO.obtenerPorPersonaId(persona.getId()).isEmpty());
    }

    @Test
    @Order(10)
    void testTransaccionActualizacion() {
        // Arrange - Crear persona
        Persona persona = new Persona("Test Transacción", "Dirección Original");
        persona.addTelefono(new Telefono(0, "333-3333333"));
        personaDAO.crear(persona);

        // Modificar con datos válidos
        persona.setNombre("Test Transacción Modificado");
        persona.clearTelefonos();
        persona.addTelefono(new Telefono(0, "444-4444444"));
        persona.addTelefono(new Telefono(0, "555-5555555"));

        // Act
        boolean actualizado = personaDAO.actualizar(persona);

        // Assert
        Assertions.assertTrue(actualizado);

        // Verificar que todos los cambios se aplicaron correctamente
        Persona personaVerificada = personaDAO.obtenerPorId(persona.getId());
        Assertions.assertEquals("Test Transacción Modificado", personaVerificada.getNombre());
        Assertions.assertEquals(2, personaVerificada.getTelefonos().size());
    }
}
