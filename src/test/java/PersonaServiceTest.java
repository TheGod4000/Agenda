/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Luisg
 */
import com.agenda.PersonaDAO;
import com.agenda.Persona;
import com.agenda.PersonaService;
import com.agenda.Telefono;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PersonaService
 */
class PersonaServiceTest {

    @Mock
    private PersonaDAO personaDAO;

    private PersonaService personaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        personaService = new PersonaService(personaDAO);
    }

    @Test
    void testCrearPersonaValida() {
        // Arrange
        Persona persona = new Persona("Juan Pérez", "Calle Principal 123");
        when(personaDAO.crear(persona)).thenReturn(true);

        // Act
        boolean resultado = personaService.crearPersona(persona);

        // Assert
        assertTrue(resultado);
        verify(personaDAO, times(1)).crear(persona);
    }

    @Test
    void testCrearPersonaNombreVacio() {
        // Arrange
        Persona persona = new Persona("", "Calle Principal 123");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personaService.crearPersona(persona)
        );

        assertEquals("El nombre es obligatorio", exception.getMessage());
        verify(personaDAO, never()).crear(any());
    }

    @Test
    void testCrearPersonaNombreMuyLargo() {
        // Arrange
        String nombreLargo = "a".repeat(101);
        Persona persona = new Persona(nombreLargo, "Calle Principal 123");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personaService.crearPersona(persona)
        );

        assertEquals("El nombre no puede exceder 100 caracteres", exception.getMessage());
    }

    @Test
    void testCrearPersonaDireccionMuyLarga() {
        // Arrange
        String direccionLarga = "a".repeat(201);
        Persona persona = new Persona("Juan Pérez", direccionLarga);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personaService.crearPersona(persona)
        );

        assertEquals("La dirección no puede exceder 200 caracteres", exception.getMessage());
    }

    @Test
    void testCrearPersonaNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personaService.crearPersona(null)
        );

        assertEquals("La persona no puede ser null", exception.getMessage());
    }

    @Test
    void testObtenerPersonaExistente() {
        // Arrange
        Persona personaEsperada = new Persona(1, "Juan Pérez", "Calle Principal 123");
        when(personaDAO.obtenerPorId(1)).thenReturn(personaEsperada);

        // Act
        Persona resultado = personaService.obtenerPersona(1);

        // Assert
        assertNotNull(resultado);
        assertEquals("Juan Pérez", resultado.getNombre());
        assertEquals("Calle Principal 123", resultado.getDireccion());
        verify(personaDAO, times(1)).obtenerPorId(1);
    }

    @Test
    void testObtenerPersonaNoExistente() {
        // Arrange
        when(personaDAO.obtenerPorId(999)).thenReturn(null);

        // Act
        Persona resultado = personaService.obtenerPersona(999);

        // Assert
        assertNull(resultado);
        verify(personaDAO, times(1)).obtenerPorId(999);
    }

    @Test
    void testObtenerTodasPersonas() {
        // Arrange
        ObservableList<Persona> personasEsperadas = FXCollections.observableArrayList(
            new Persona(1, "Juan Pérez", "Calle A"),
            new Persona(2, "María García", "Calle B")
        );
        when(personaDAO.obtenerTodas()).thenReturn(personasEsperadas);

        // Act
        ObservableList<Persona> resultado = personaService.obtenerTodasPersonas();

        // Assert
        assertEquals(2, resultado.size());
        assertEquals("Juan Pérez", resultado.get(0).getNombre());
        assertEquals("María García", resultado.get(1).getNombre());
    }

    @Test
    void testActualizarPersonaValida() {
        // Arrange
        Persona persona = new Persona(1, "Juan Pérez Actualizado", "Nueva Dirección");
        when(personaDAO.actualizar(persona)).thenReturn(true);

        // Act
        boolean resultado = personaService.actualizarPersona(persona);

        // Assert
        assertTrue(resultado);
        verify(personaDAO, times(1)).actualizar(persona);
    }

    @Test
    void testEliminarPersona() {
        // Arrange
        when(personaDAO.eliminar(1)).thenReturn(true);

        // Act
        boolean resultado = personaService.eliminarPersona(1);

        // Assert
        assertTrue(resultado);
        verify(personaDAO, times(1)).eliminar(1);
    }

    @Test
    void testBuscarPersonasConTexto() {
        // Arrange
        String textoBusqueda = "Juan";
        ObservableList<Persona> personasEsperadas = FXCollections.observableArrayList(
            new Persona(1, "Juan Pérez", "Calle A")
        );
        when(personaDAO.buscarPorNombre(textoBusqueda)).thenReturn(personasEsperadas);

        // Act
        ObservableList<Persona> resultado = personaService.buscarPersonas(textoBusqueda);

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("Juan Pérez", resultado.get(0).getNombre());
        verify(personaDAO, times(1)).buscarPorNombre(textoBusqueda);
    }

    @Test
    void testBuscarPersonasTextoVacio() {
        // Arrange
        ObservableList<Persona> todasPersonas = FXCollections.observableArrayList(
            new Persona(1, "Juan Pérez", "Calle A"),
            new Persona(2, "María García", "Calle B")
        );
        when(personaDAO.obtenerTodas()).thenReturn(todasPersonas);

        // Act
        ObservableList<Persona> resultado = personaService.buscarPersonas("");

        // Assert
        assertEquals(2, resultado.size());
        verify(personaDAO, times(1)).obtenerTodas();
        verify(personaDAO, never()).buscarPorNombre(anyString());
    }

    @Test
    void testAgregarTelefonoValido() {
        // Arrange
        Persona persona = new Persona(1, "Juan Pérez", "Calle A");
        String telefono = "555-1234567";

        // Act
        boolean resultado = personaService.agregarTelefono(persona, telefono);

        // Assert
        assertTrue(resultado);
        assertEquals(1, persona.getTelefonos().size());
        assertEquals("555-1234567", persona.getTelefonos().get(0).getTelefono());
    }

    @Test
    void testAgregarTelefonoVacio() {
        // Arrange
        Persona persona = new Persona(1, "Juan Pérez", "Calle A");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personaService.agregarTelefono(persona, "")
        );

        assertEquals("El número de teléfono no puede estar vacío", exception.getMessage());
    }

    @Test
    void testAgregarTelefonoDuplicado() {
        // Arrange
        Persona persona = new Persona(1, "Juan Pérez", "Calle A");
        persona.addTelefono(new Telefono(0, "555-1234567"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personaService.agregarTelefono(persona, "555-1234567")
        );

        assertEquals("Este teléfono ya existe para la persona", exception.getMessage());
    }

    @Test
    void testAgregarTelefonoFormatoInvalido() {
        // Arrange
        Persona persona = new Persona(1, "Juan Pérez", "Calle A");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personaService.agregarTelefono(persona, "abc")
        );

        assertEquals("Formato de teléfono inválido", exception.getMessage());
    }

    @Test
    void testValidarTelefonosConFormatosValidos() {
        // Arrange
        Persona persona = new Persona("Juan Pérez", "Calle A");
        persona.addTelefono(new Telefono(0, "555-1234567"));
        persona.addTelefono(new Telefono(0, "(555) 123-4567"));
        persona.addTelefono(new Telefono(0, "555.123.4567"));
        persona.addTelefono(new Telefono(0, "+52 555 123 4567"));
        
        when(personaDAO.crear(persona)).thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() -> personaService.crearPersona(persona));
    }

    @Test
    void testValidarTelefonoConFormatoInvalido() {
        // Arrange
        Persona persona = new Persona("Juan Pérez", "Calle A");
        persona.addTelefono(new Telefono(0, "123")); // Muy corto

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personaService.crearPersona(persona)
        );

        assertTrue(exception.getMessage().contains("Formato de teléfono inválido"));
    }

    @Test
    void testExistePersonaConNombre() {
        // Arrange
        ObservableList<Persona> personas = FXCollections.observableArrayList(
            new Persona(1, "Juan Pérez", "Calle A"),
            new Persona(2, "María García", "Calle B")
        );
        when(personaDAO.buscarPorNombre("Juan")).thenReturn(personas);

        // Act
        boolean existe = personaService.existePersonaConNombre("Juan Pérez", 0);

        // Assert
        assertTrue(existe);
    }

    @Test
    void testNoExistePersonaConNombreExcluyendoId() {
        // Arrange
        ObservableList<Persona> personas = FXCollections.observableArrayList(
            new Persona(1, "Juan Pérez", "Calle A")
        );
        when(personaDAO.buscarPorNombre("Juan")).thenReturn(personas);

        // Act
        boolean existe = personaService.existePersonaConNombre("Juan Pérez", 1);

        // Assert
        assertFalse(existe); // No existe porque excluye el ID 1
    }
}
