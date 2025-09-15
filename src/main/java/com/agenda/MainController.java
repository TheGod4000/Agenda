package com.agenda;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador principal de la aplicación - Actualizado para direcciones múltiples
 */
public class MainController implements Initializable {

    @FXML private TableView<Persona> tablaPersonas;
    @FXML private TableColumn<Persona, String> columnaNombre;
    @FXML private TableColumn<Persona, String> columnaDireccion;
    @FXML private TableColumn<Persona, String> columnaTelefonos;

    @FXML private TextField txtNombre;
    @FXML private TextField txtDireccion;
    @FXML private ListView<Telefono> listaTelefonos;
    @FXML private TextField txtTelefono;

    @FXML private Button btnNuevo;
    @FXML private Button btnGuardar;
    @FXML private Button btnActualizar;
    @FXML private Button btnEliminar;
    @FXML private Button btnAgregarTelefono;
    @FXML private Button btnEliminarTelefono;

    @FXML private TextField txtBuscar;
    @FXML private Button btnBuscar;
    @FXML private Button btnMostrarTodos;

    private PersonaService personaService;
    private DireccionService direccionService;
    private Persona personaSeleccionada;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Inicializando MainController con soporte para direcciones múltiples...");

        personaService = new PersonaService();
        direccionService = new DireccionService();

        // Configurar columnas de la tabla usando Properties
        columnaNombre.setCellValueFactory(cellData -> cellData.getValue().nombreProperty());

        // Columna personalizada para mostrar descripción de direcciones
        columnaDireccion.setCellValueFactory(cellData -> {
            Persona persona = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(persona.getDescripcionDirecciones());
        });

        // Columna personalizada para mostrar teléfonos
        columnaTelefonos.setCellValueFactory(cellData -> {
            Persona persona = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(persona.getTelefonosAsString());
        });

        // Configurar eventos
        configurarEventos();

        // Cargar datos iniciales
        System.out.println("Cargando personas...");
        cargarPersonas();

        // Estado inicial
        limpiarFormulario();
        habilitarBotones(false, false, false);

        System.out.println("MainController inicializado correctamente");
    }

    private void configurarEventos() {
        // Selección en tabla
        tablaPersonas.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    personaSeleccionada = newValue;
                    if (newValue != null) {
                        System.out.println("Persona seleccionada: " + newValue.getNombre());
                        cargarPersonaEnFormulario(newValue);
                        habilitarBotones(false, true, true);
                    } else {
                        limpiarFormulario();
                        habilitarBotones(false, false, false);
                    }
                }
        );

        // Doble clic en teléfono para editar
        listaTelefonos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                editarTelefonoSeleccionado();
            }
        });

        // Eventos de botones
        btnNuevo.setOnAction(e -> nuevoRegistro());
        btnGuardar.setOnAction(e -> guardarRegistro());
        btnActualizar.setOnAction(e -> actualizarRegistro());
        btnEliminar.setOnAction(e -> eliminarRegistro());
        btnAgregarTelefono.setOnAction(e -> agregarTelefono());
        btnEliminarTelefono.setOnAction(e -> eliminarTelefono());
        btnBuscar.setOnAction(e -> buscarPersonas());
        btnMostrarTodos.setOnAction(e -> cargarPersonas());

        // Búsqueda en tiempo real
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                buscarPersonas();
            } else {
                cargarPersonas(); // Mostrar todos si el campo está vacío
            }
        });

        // Doble clic en la dirección para abrir gestión de direcciones
        txtDireccion.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && personaSeleccionada != null) {
                abrirGestionDirecciones();
            }
        });
    }

    private void cargarPersonas() {
        try {
            System.out.println("Obteniendo personas de la base de datos...");
            ObservableList<Persona> personas = personaService.obtenerTodasPersonas();
            System.out.println("Personas obtenidas: " + personas.size());

            // Debug: mostrar información de cada persona
            for (int i = 0; i < Math.min(5, personas.size()); i++) {
                Persona p = personas.get(i);
                System.out.println("Persona " + (i+1) + ": " + p.getNombre() +
                        " - Direcciones: " + p.getTotalDirecciones() +
                        " - Teléfonos: " + p.getTelefonos().size());
            }

            tablaPersonas.setItems(personas);
            tablaPersonas.refresh(); // Forzar actualización de la tabla

            System.out.println("Tabla actualizada con " + personas.size() + " personas");

        } catch (Exception e) {
            System.err.println("Error al cargar personas: " + e.getMessage());
            e.printStackTrace();
            mostrarMensaje("Error", "Error al cargar personas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void cargarPersonaEnFormulario(Persona persona) {
        if (persona != null) {
            txtNombre.setText(persona.getNombre());

            // Mostrar información resumida de direcciones
            if (persona.tieneDirecciones()) {
                txtDireccion.setText(persona.getDescripcionDirecciones() + " (Doble clic para gestionar)");
                txtDireccion.setStyle("-fx-text-fill: blue; -fx-font-style: italic;");
            } else {
                txtDireccion.setText("Sin direcciones (Doble clic para agregar)");
                txtDireccion.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            }

            listaTelefonos.setItems(persona.getTelefonos());
            System.out.println("Formulario cargado para: " + persona.getNombre() +
                    " con " + persona.getTotalDirecciones() + " direcciones");
        }
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtDireccion.clear();
        txtDireccion.setStyle("");
        txtTelefono.clear();
        listaTelefonos.getItems().clear();
        personaSeleccionada = null;
        tablaPersonas.getSelectionModel().clearSelection();
    }

    private void habilitarBotones(boolean guardar, boolean actualizar, boolean eliminar) {
        btnGuardar.setDisable(!guardar);
        btnActualizar.setDisable(!actualizar);
        btnEliminar.setDisable(!eliminar);
    }

    @FXML
    private void nuevoRegistro() {
        limpiarFormulario();
        habilitarBotones(true, false, false);
        txtNombre.requestFocus();
        txtDireccion.setText("Guarde primero la persona, luego doble clic para agregar direcciones");
        txtDireccion.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
    }

    @FXML
    private void guardarRegistro() {
        try {
            if (validarCampos()) {
                // Crear solo con nombre (las direcciones se gestionan por separado)
                Persona nuevaPersona = new Persona(txtNombre.getText().trim());

                // Agregar teléfonos
                for (Telefono tel : listaTelefonos.getItems()) {
                    nuevaPersona.addTelefono(new Telefono(0, tel.getTelefono()));
                }

                if (personaService.crearPersona(nuevaPersona)) {
                    mostrarMensaje("Éxito",
                            "Persona guardada correctamente.\n" +
                                    "Para agregar direcciones, seleccione la persona y haga doble clic en el campo de dirección.",
                            Alert.AlertType.INFORMATION);
                    cargarPersonas();
                    limpiarFormulario();
                    habilitarBotones(false, false, false);
                } else {
                    mostrarMensaje("Error", "No se pudo guardar la persona", Alert.AlertType.ERROR);
                }
            }
        } catch (IllegalArgumentException e) {
            mostrarMensaje("Error de validación", e.getMessage(), Alert.AlertType.WARNING);
        } catch (Exception e) {
            System.err.println("Error al guardar: " + e.getMessage());
            e.printStackTrace();
            mostrarMensaje("Error", "Error inesperado: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void actualizarRegistro() {
        if (personaSeleccionada == null) {
            mostrarMensaje("Error", "No hay persona seleccionada", Alert.AlertType.WARNING);
            return;
        }

        try {
            if (validarCampos()) {
                personaSeleccionada.setNombre(txtNombre.getText().trim());

                // Actualizar teléfonos
                personaSeleccionada.clearTelefonos();
                for (Telefono tel : listaTelefonos.getItems()) {
                    personaSeleccionada.addTelefono(new Telefono(0, tel.getTelefono()));
                }

                if (personaService.actualizarPersona(personaSeleccionada)) {
                    mostrarMensaje("Éxito",
                            "Persona actualizada correctamente.\n" +
                                    "Las direcciones se gestionan por separado haciendo doble clic en el campo de dirección.",
                            Alert.AlertType.INFORMATION);
                    cargarPersonas();
                    // Mantener la selección
                    tablaPersonas.getSelectionModel().select(personaSeleccionada);
                } else {
                    mostrarMensaje("Error", "No se pudo actualizar la persona", Alert.AlertType.ERROR);
                }
            }
        } catch (IllegalArgumentException e) {
            mostrarMensaje("Error de validación", e.getMessage(), Alert.AlertType.WARNING);
        } catch (Exception e) {
            System.err.println("Error al actualizar: " + e.getMessage());
            e.printStackTrace();
            mostrarMensaje("Error", "Error inesperado: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void eliminarRegistro() {
        if (personaSeleccionada == null) {
            mostrarMensaje("Error", "No hay persona seleccionada", Alert.AlertType.WARNING);
            return;
        }

        String mensaje = "Esta acción no se puede deshacer. Se eliminarán:\n" +
                "- Los datos de la persona\n" +
                "- Todos los teléfonos asociados\n" +
                "- Todas las asociaciones con direcciones\n" +
                "(Las direcciones no se eliminan, solo se desvinculan)";

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro de eliminar a " + personaSeleccionada.getNombre() + "?");
        confirmacion.setContentText(mensaje);

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            if (personaService.eliminarPersona(personaSeleccionada.getId())) {
                mostrarMensaje("Éxito", "Persona eliminada correctamente", Alert.AlertType.INFORMATION);
                cargarPersonas();
                limpiarFormulario();
                habilitarBotones(false, false, false);
            } else {
                mostrarMensaje("Error", "No se pudo eliminar la persona", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void agregarTelefono() {
        String telefono = txtTelefono.getText().trim();
        if (telefono.isEmpty()) {
            mostrarMensaje("Error", "Ingrese un número de teléfono", Alert.AlertType.WARNING);
            return;
        }

        try {
            // Verificar que no exista el mismo teléfono
            for (Telefono tel : listaTelefonos.getItems()) {
                if (tel.getTelefono().equals(telefono)) {
                    mostrarMensaje("Error", "Este teléfono ya existe", Alert.AlertType.WARNING);
                    return;
                }
            }

            // Validar formato básico
            if (!telefono.matches(".*\\d.*")) {
                mostrarMensaje("Error", "El teléfono debe contener al menos un dígito", Alert.AlertType.WARNING);
                return;
            }

            Telefono nuevoTelefono = new Telefono(0, telefono);
            listaTelefonos.getItems().add(nuevoTelefono);
            txtTelefono.clear();

        } catch (Exception e) {
            mostrarMensaje("Error", "Error al agregar teléfono: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void eliminarTelefono() {
        Telefono telefonoSeleccionado = listaTelefonos.getSelectionModel().getSelectedItem();
        if (telefonoSeleccionado == null) {
            mostrarMensaje("Error", "Seleccione un teléfono para eliminar", Alert.AlertType.WARNING);
            return;
        }

        listaTelefonos.getItems().remove(telefonoSeleccionado);
    }

    @FXML
    private void buscarPersonas() {
        String textoBusqueda = txtBuscar.getText().trim();
        System.out.println("Buscando: " + textoBusqueda);

        ObservableList<Persona> resultados = personaService.buscarPersonas(textoBusqueda);
        System.out.println("Resultados encontrados: " + resultados.size());

        tablaPersonas.setItems(resultados);
        tablaPersonas.refresh();
    }

    private void abrirGestionDirecciones() {
        if (personaSeleccionada == null) {
            mostrarMensaje("Error",
                    "Primero debe guardar la persona para poder gestionar sus direcciones",
                    Alert.AlertType.WARNING);
            return;
        }

        try {
            System.out.println("Abriendo gestión de direcciones para: " + personaSeleccionada.getNombre());

            // Cargar la vista de direcciones
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/agenda/secondary.fxml"));
            Parent root = loader.load();

            // Configurar ventana
            Stage stage = new Stage();
            stage.setTitle("Gestión de Direcciones - " + personaSeleccionada.getNombre());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1200, 700));

            // Pasar datos al controlador de direcciones
            DireccionesController controller = loader.getController();
            // Si tienes un método para establecer la persona seleccionada, úsalo aquí
            // controller.setPersonaSeleccionada(personaSeleccionada);

            stage.showAndWait();

            // Recargar datos después de cerrar la ventana de direcciones
            cargarPersonas();
            if (personaSeleccionada != null) {
                // Reseleccionar la persona para mostrar cambios
                tablaPersonas.getSelectionModel().select(personaSeleccionada);
            }

        } catch (Exception e) {
            System.err.println("Error al abrir gestión de direcciones: " + e.getMessage());
            e.printStackTrace();
            mostrarMensaje("Error",
                    "No se pudo abrir la gestión de direcciones: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void editarTelefonoSeleccionado() {
        Telefono telefonoSeleccionado = listaTelefonos.getSelectionModel().getSelectedItem();
        if (telefonoSeleccionado == null) return;

        TextInputDialog dialog = new TextInputDialog(telefonoSeleccionado.getTelefono());
        dialog.setTitle("Editar Teléfono");
        dialog.setHeaderText("Editar número de teléfono:");
        dialog.setContentText("Teléfono:");

        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isPresent() && !resultado.get().trim().isEmpty()) {
            String nuevoTelefono = resultado.get().trim();

            // Verificar que no exista otro teléfono igual
            for (Telefono tel : listaTelefonos.getItems()) {
                if (tel != telefonoSeleccionado && tel.getTelefono().equals(nuevoTelefono)) {
                    mostrarMensaje("Error", "Este teléfono ya existe", Alert.AlertType.WARNING);
                    return;
                }
            }

            telefonoSeleccionado.setTelefono(nuevoTelefono);
            listaTelefonos.refresh();
        }
    }

    private boolean validarCampos() {
        if (txtNombre.getText().trim().isEmpty()) {
            mostrarMensaje("Error de validación", "El nombre es obligatorio", Alert.AlertType.WARNING);
            txtNombre.requestFocus();
            return false;
        }

        if (txtNombre.getText().trim().length() > 100) {
            mostrarMensaje("Error de validación", "El nombre no puede exceder 100 caracteres", Alert.AlertType.WARNING);
            txtNombre.requestFocus();
            return false;
        }

        return true;
    }

    private void mostrarMensaje(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}