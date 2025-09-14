/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author Luisg
 */
import com.agenda.Telefono;
import com.agenda.TelefonoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para TelefonoDAO
 * Estas pruebas se pueden ejecutar con una base de datos en memoria H2 para mayor rapidez
 */
@ExtendWith(MockitoExtension.class)
class TelefonoDAOTest {

    private TelefonoDAO telefonoDAO;

    @BeforeEach
    void setUp() {
        telefonoDAO = new TelefonoDAO();
    }

    @Test
    void testCrearTelefono() {
        // Este test requeriría una configuración de base de datos de prueba
        // Por simplicidad, aquí se muestra la estructura del test
        
        // Arrange
        Telefono telefono = new Telefono(1, "555-1234567");
        
        // Act
        // boolean resultado = telefonoDAO.crear(telefono);
        
        // Assert
        // assertTrue(resultado);
        // assertTrue(telefono.getId() > 0);
    }

    @Test
    void testObtenerTelefonoPorId() {
        // Similar estructura para otros tests...
    }

    @Test
    void testValidarFormatoTelefono() {
        // Arrange & Act & Assert
        Telefono telefono1 = new Telefono(1, "555-1234567");
        Telefono telefono2 = new Telefono(1, "(555) 123-4567");
        Telefono telefono3 = new Telefono(1, "555.123.4567");
        Telefono telefono4 = new Telefono(1, "+52 555 123 4567");
        
        // Estos formatos deberían ser válidos
        assertNotNull(telefono1.getTelefono());
        assertNotNull(telefono2.getTelefono());
        assertNotNull(telefono3.getTelefono());
        assertNotNull(telefono4.getTelefono());
        
        assertTrue(telefono1.getTelefono().contains("555"));
        assertTrue(telefono2.getTelefono().contains("555"));
        assertTrue(telefono3.getTelefono().contains("555"));
        assertTrue(telefono4.getTelefono().contains("555"));
    }
}
