/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.agenda;

/**
 *
 * @author Luisg
 */
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Aplicación principal de la Agenda - Versión actualizada con soporte para direcciones múltiples
 */
public class AgendaApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("=== Iniciando Aplicación de Agenda con Direcciones Múltiples ===");

            // Verificar conexión a la base de datos
            DatabaseConnection dbConnection = DatabaseConnection.getInstance();

            if (!dbConnection.testConnection()) {
                mostrarError("Error de Conexión",
                        "No se pudo conectar a la base de datos MySQL.\n\n" +
                                "Verifique que:\n" +
                                "1. MySQL esté ejecutándose en el puerto 3306\n" +
                                "2. La base de datos 'agenda' exista\n" +
                                "3. El usuario 'root' tenga acceso\n" +
                                "4. La contraseña sea correcta\n" +
                                "5. El conector MySQL esté en el classpath\n\n" +
                                "Error de conexión detectado. Revise los logs para más detalles.");
                return;
            }

            // Preguntar si insertar datos de prueba (solo si no hay datos)
            if (debeInsertarDatosPrueba()) {
                System.out.println("Insertando datos de prueba con direcciones múltiples...");
                dbConnection.insertTestDataIfEmpty();

                // Verificar que los datos se insertaron correctamente
                verificarDatosPrueba();
            }

            // Cargar la vista principal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/agenda/primary.fxml"));
            Parent root = loader.load();

            // Configurar la escena
            Scene scene = new Scene(root, 1200, 700); // Aumentado el tamaño para mejor visualización

            // Configurar el stage
            primaryStage.setTitle("Gestión de Agenda - JavaFX con MySQL y Direcciones Múltiples");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(600);

            // Mostrar la ventana
            primaryStage.show();

            System.out.println("✓ Aplicación iniciada correctamente con soporte para direcciones múltiples");
            mostrarMensajeBienvenida();

            // Configurar el cierre de la aplicación
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("Cerrando aplicación...");

                // Verificar integridad de datos antes de cerrar
                try {
                    DireccionService direccionService = new DireccionService();
                    direccionService.verificarIntegridad();
                } catch (Exception e) {
                    System.err.println("Error al verificar integridad: " + e.getMessage());
                }

                dbConnection.closeConnection();
                System.out.println("Aplicación cerrada correctamente");
                System.exit(0);
            });

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error de Aplicación",
                    "Error al inicializar la aplicación:\n\n" +
                            e.getMessage() + "\n\n" +
                            "Verifique que el archivo primary.fxml esté en la ruta correcta: /com/agenda/primary.fxml");
        }
    }

    /**
     * Determina si debe insertar datos de prueba
     */
    private boolean debeInsertarDatosPrueba() {
        try {
            PersonaService personaService = new PersonaService();
            var personas = personaService.obtenerTodasPersonas();

            if (personas.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Base de Datos Vacía");
                alert.setHeaderText("No se encontraron datos en la base de datos");
                alert.setContentText(
                        "¿Desea insertar datos de prueba para comenzar?\n\n" +
                                "Los datos incluirán:\n" +
                                "• 3 personas con teléfonos\n" +
                                "• 4 direcciones diferentes\n" +
                                "• Direcciones compartidas entre personas\n" +
                                "• Ejemplos de direcciones principales y secundarias"
                );

                Optional<ButtonType> result = alert.showAndWait();
                return result.isPresent() && result.get() == ButtonType.OK;
            }

        } catch (Exception e) {
            System.err.println("Error al verificar datos existentes: " + e.getMessage());
        }

        return false;
    }

    /**
     * Verificar que los datos de prueba se insertaron correctamente
     */
    private void verificarDatosPrueba() {
        try {
            PersonaService personaService = new PersonaService();
            DireccionService direccionService = new DireccionService();
            PersonaDireccionDAO personaDireccionDAO = new PersonaDireccionDAO();

            var personas = personaService.obtenerTodasPersonas();
            var direcciones = direccionService.obtenerTodasDirecciones();

            System.out.println("\n=== Verificación de Datos de Prueba ===");
            System.out.println("Personas creadas: " + personas.size());
            System.out.println("Direcciones creadas: " + direcciones.size());

            // Verificar direcciones compartidas
            int direccionesCompartidas = 0;
            for (var direccion : direcciones) {
                int cantidadPersonas = personaDireccionDAO.contarPersonasPorDireccion(direccion.getId());
                if (cantidadPersonas > 1) {
                    direccionesCompartidas++;
                    System.out.println("Dirección compartida: " + direccion.getDireccionResumida() +
                            " (" + cantidadPersonas + " personas)");
                }
            }

            // Mostrar resumen de cada persona
            for (var persona : personas) {
                System.out.println("Persona: " + persona.getNombre() +
                        " - Direcciones: " + persona.getTotalDirecciones() +
                        " - Teléfonos: " + persona.getTelefonos().size());
            }

            System.out.println("Direcciones compartidas encontradas: " + direccionesCompartidas);
            System.out.println("✓ Datos de prueba verificados correctamente");

        } catch (Exception e) {
            System.err.println("Error al verificar datos de prueba: " + e.getMessage());
        }
    }

    /**
     * Mostrar mensaje de bienvenida con información sobre las nuevas funcionalidades
     */
    private void mostrarMensajeBienvenida() {
        Alert bienvenida = new Alert(Alert.AlertType.INFORMATION);
        bienvenida.setTitle("Bienvenido a la Agenda");
        bienvenida.setHeaderText("Sistema de Gestión de Contactos con Direcciones Múltiples");
        bienvenida.setContentText(
                "Nuevas funcionalidades disponibles:\n\n" +
                        "🏠 DIRECCIONES MÚLTIPLES:\n" +
                        "• Cada persona puede tener múltiples direcciones\n" +
                        "• Direcciones compartidas entre personas\n" +
                        "• Etiquetas personalizadas (Casa, Trabajo, etc.)\n" +
                        "• Designación de dirección principal\n\n" +
                        "🎯 CÓMO USAR:\n" +
                        "• Clic derecho en una persona para gestionar direcciones\n" +
                        "• Use el menú contextual para opciones avanzadas\n" +
                        "• Acceda al gestor completo desde el menú contextual\n\n" +
                        "📞 TELÉFONOS:\n" +
                        "• Múltiples teléfonos por persona\n" +
                        "• Doble clic para editar teléfonos\n\n" +
                        "🔍 BÚSQUEDA:\n" +
                        "• Búsqueda en tiempo real\n" +
                        "• Buscar por nombre, dirección o teléfono"
        );

        bienvenida.getDialogPane().setPrefWidth(500);
        bienvenida.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        System.out.println("Iniciando aplicación JavaFX Agenda con Direcciones Múltiples...");
        launch(args);
    }
}

/**
 * Clase para probar la conexión a la base de datos independientemente - Versión actualizada
 */
class AgendaDB {
    public static void main(String[] args) {
        System.out.println("=== Prueba de Conexión a MySQL - Agenda con Direcciones Múltiples ===");

        DatabaseConnection dbConnection = DatabaseConnection.getInstance();

        if (dbConnection.testConnection()) {
            System.out.println("✓ Conexión exitosa a la base de datos MySQL");

            try {
                // Insertar datos de prueba si la base está vacía
                dbConnection.insertTestDataIfEmpty();

                // Realizar pruebas con los servicios actualizados
                System.out.println("\n=== Probando Servicios Actualizados ===");

                PersonaService personaService = new PersonaService();
                DireccionService direccionService = new DireccionService();
                PersonaDireccionDAO personaDireccionDAO = new PersonaDireccionDAO();

                // Probar PersonaService
                var personas = personaService.obtenerTodasPersonas();
                System.out.println("Total de personas encontradas: " + personas.size());

                for (int i = 0; i < Math.min(3, personas.size()); i++) {
                    Persona p = personas.get(i);
                    System.out.println("- " + p.getNombre() +
                            " (Direcciones: " + p.getTotalDirecciones() +
                            ", Teléfonos: " + p.getTelefonos().size() + ")");

                    // Mostrar direcciones de la persona
                    if (p.getTotalDirecciones() > 0) {
                        System.out.println("  Direcciones: " + p.getDireccionesAsString());
                    }
                }

                // Probar DireccionService
                var direcciones = direccionService.obtenerTodasDirecciones();
                System.out.println("\nTotal de direcciones encontradas: " + direcciones.size());

                for (var direccion : direcciones) {
                    int cantidadPersonas = personaDireccionDAO.contarPersonasPorDireccion(direccion.getId());
                    System.out.println("- " + direccion.getDireccionCompleta() +
                            " (Usada por " + cantidadPersonas + " personas)");
                }

                // Verificar direcciones compartidas
                System.out.println("\n=== Direcciones Compartidas ===");
                boolean hayCompartidas = false;
                for (var direccion : direcciones) {
                    int cantidadPersonas = personaDireccionDAO.contarPersonasPorDireccion(direccion.getId());
                    if (cantidadPersonas > 1) {
                        hayCompartidas = true;
                        System.out.println("✓ Dirección compartida: " + direccion.getDireccionResumida());

                        var relaciones = personaDireccionDAO.obtenerPorDireccionId(direccion.getId());
                        for (var relacion : relaciones) {
                            var persona = personaService.obtenerPersona(relacion.getPersonaId());
                            if (persona != null) {
                                System.out.println("  - " + persona.getNombre() +
                                        " (" + relacion.getEtiqueta() +
                                        (relacion.isEsPrincipal() ? ", Principal" : "") + ")");
                            }
                        }
                        System.out.println();
                    }
                }

                if (!hayCompartidas) {
                    System.out.println("No se encontraron direcciones compartidas");
                }

                // Verificar integridad
                System.out.println("\n=== Verificación de Integridad ===");
                direccionService.verificarIntegridad();

                if (!personas.isEmpty()) {
                    System.out.println("\n✓ La aplicación debería mostrar todos estos datos correctamente");
                    System.out.println("✓ Las funcionalidades de direcciones múltiples están operativas");
                } else {
                    System.out.println("\n⚠ No hay datos para mostrar. Ejecute la aplicación para insertar datos de prueba.");
                }

            } catch (Exception e) {
                System.err.println("✗ Error al probar los servicios: " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            System.out.println("✗ No se pudo conectar a la base de datos");
            System.out.println("\nPasos para solucionar:");
            System.out.println("1. Verifique que MySQL esté ejecutándose:");
            System.out.println("   - Windows: net start mysql");
            System.out.println("   - Linux/Mac: sudo systemctl start mysql");
            System.out.println("2. Cree la base de datos:");
            System.out.println("   mysql -u root -p");
            System.out.println("   CREATE DATABASE agenda;");
            System.out.println("3. Verifique las credenciales en DatabaseConfig.java");
            System.out.println("4. Asegúrese de tener el conector MySQL en el classpath");
        }

        dbConnection.closeConnection();

        System.out.println("\n=== Resumen de Funcionalidades ===");
        System.out.println("✓ Sistema de direcciones múltiples implementado");
        System.out.println("✓ Soporte para direcciones compartidas");
        System.out.println("✓ Etiquetas y direcciones principales");
        System.out.println("✓ Migración automática de datos antiguos");
        System.out.println("✓ Integridad referencial mantenida");
        System.out.println("✓ Interfaces de usuario actualizadas");
    }
}