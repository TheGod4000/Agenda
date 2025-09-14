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
 * Aplicación principal de la Agenda - Versión Mejorada
 */
public class AgendaApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("=== Iniciando Aplicación de Agenda ===");
            
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
                dbConnection.insertTestDataIfEmpty();
            }

            // Cargar la vista principal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/agenda/primary.fxml"));
            Parent root = loader.load();

            // Configurar la escena
            Scene scene = new Scene(root, 1000, 600);
            
            // Configurar el stage
            primaryStage.setTitle("Gestión de Agenda - JavaFX con MySQL");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(500);
            
            // Mostrar la ventana
            primaryStage.show();
            
            System.out.println("✓ Aplicación iniciada correctamente");

            // Configurar el cierre de la aplicación
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("Cerrando aplicación...");
                dbConnection.closeConnection();
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
            PersonaDAO personaDAO = new PersonaDAO();
            var personas = personaDAO.obtenerTodas();
            
            if (personas.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Base de Datos Vacía");
                alert.setHeaderText("No se encontraron datos en la base de datos");
                alert.setContentText("¿Desea insertar algunos datos de prueba para comenzar?");
                
                Optional<ButtonType> result = alert.showAndWait();
                return result.isPresent() && result.get() == ButtonType.OK;
            }
            
        } catch (Exception e) {
            System.err.println("Error al verificar datos existentes: " + e.getMessage());
        }
        
        return false;
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        System.out.println("Iniciando aplicación JavaFX Agenda...");
        launch(args);
    }
}

/**
 * Clase para probar la conexión a la base de datos independientemente - Versión Mejorada
 */
class AgendaDB {
    public static void main(String[] args) {
        System.out.println("=== Prueba de Conexión a MySQL - Agenda ===");
        
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();
        
        if (dbConnection.testConnection()) {
            System.out.println("✓ Conexión exitosa a la base de datos MySQL");
            
            try {
                // Insertar datos de prueba si la base está vacía
                dbConnection.insertTestDataIfEmpty();
                
                // Realizar pruebas con el DAO
                PersonaDAO personaDAO = new PersonaDAO();
                
                System.out.println("\n=== Probando PersonaDAO ===");
                var personas = personaDAO.obtenerTodas();
                System.out.println("Total de personas encontradas: " + personas.size());
                
                for (int i = 0; i < Math.min(3, personas.size()); i++) {
                    Persona p = personas.get(i);
                    System.out.println("- " + p.getNombre() + " (" + p.getDireccion() + ") - Teléfonos: " + p.getTelefonos().size());
                }
                
                if (!personas.isEmpty()) {
                    System.out.println("\n✓ La aplicación debería mostrar todos estos datos en la tabla");
                } else {
                    System.out.println("\n⚠ No hay datos para mostrar. Ejecute la aplicación para insertar datos de prueba.");
                }
                
            } catch (Exception e) {
                System.err.println("✗ Error al probar el DAO: " + e.getMessage());
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
        }
        
        dbConnection.closeConnection();
    }
}