// MainController.java - Refactorizado para depender de interfaces
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
 * Controlador principal refactorizado que depende de interfaces (DIP)
 * y tiene responsabilidades más claras (SRP)
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

    // Dependencias de interfaces, no de clases concretas (DIP)
    private final IPersonaService personaService;
    private final IDireccionService direccionService;

    private Persona personaSeleccionada;

    // Constructor por defecto - inicializa con implementaciones concretas
    public MainController() {
        this.personaService = new PersonaService();
        this.direccionService = new DireccionService();
    }

    // Constructor para inyección de dependencias (ideal para testing)
    public MainController(IPersonaService personaService, IDireccionService direccionService) {
        this.personaService = personaService;
        this.direccionService = direccionService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Inicializando MainController con principios SOLID aplicados...");

        configurarTabla();
        configurarEventos();
        cargarPersonas();
        establecerEstadoInicial();

        System.out.println("MainController inicializado correctamente");
    }

    /**
     * Configurar las columnas de la tabla (SRP - método específico para configuración de tabla)
     */
    private void configurarTabla() {
        columnaNombre.setCellValueFactory(cellData -> cellData.getValue().nombreProperty());

        columnaDireccion.setCellValueFactory(cellData -> {
            Persona persona = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(persona.getDescripcionDirecciones());
        });

        columnaTelefonos.setCellValueFactory(cellData -> {
            Persona persona = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(persona.getTelefonosAsString());
        });
    }

    /**
     * Configurar todos los eventos de la interfaz (SRP - método específico para eventos)
     */
    private void configurarEventos() {
        // Eventos de selección
        tablaPersonas.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> manejarSeleccionPersona(newValue));

        // Eventos de botones
        btnNuevo.setOnAction(e -> nuevoRegistro());
        btnGuardar.setOnAction(e -> guardarRegistro());
        btnActualizar.setOnAction(e -> actualizarRegistro());
        btnEliminar.setOnAction(e -> eliminarRegistro());
        btnAgregarTelefono.setOnAction(e -> agregarTelefono());
        btnEliminarTelefono.setOnAction(e -> eliminarTelefono());
        btnBuscar.setOnAction(e -> buscarPersonas());
        btnMostrarTodos.setOnAction(e -> cargarPersonas());

        // Eventos especiales
        listaTelefonos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                editarTelefonoSeleccionado();
            }
        });

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                buscarPersonas();
            } else {
                cargarPersonas();
            }
        });

        txtDireccion.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && personaSeleccionada != null) {
                abrirGestionDirecciones();
            }
        });
    }

    /**
     * Establecer el estado inicial de la interfaz
     */
    private void establecerEstadoInicial() {
        limpiarFormulario();
        habilitarBotones(false, false, false);
    }

    /**
     * Manejar la selección de una persona en la tabla
     */
    private void manejarSeleccionPersona(Persona nuevaSeleccion) {
        personaSeleccionada = nuevaSeleccion;

        if (nuevaSeleccion != null) {
            System.out.println("Persona seleccionada: " + nuevaSeleccion.getNombre());
            cargarPersonaEnFormulario(nuevaSeleccion);
            habilitarBotones(false, true, true);
        } else {
            limpiarFormulario();
            habilitarBotones(false, false, false);
        }
    }

    /**
     * Cargar datos de persona en el formulario
     */
    private void cargarPersonaEnFormulario(Persona persona) {
        if (persona != null) {
            txtNombre.setText(persona.getNombre());

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

    /**
     * Cargar todas las personas desde el servicio
     */
    private void cargarPersonas() {
        try {
            System.out.println("Cargando personas usando servicio...");
            ObservableList<Persona> personas = personaService.obtenerTodasPersonas();

            tablaPersonas.setItems(personas);
            tablaPersonas.refresh();

            System.out.println("Tabla actualizada con " + personas.size() + " personas");

        } catch (Exception e) {
            System.err.println("Error al cargar personas: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error al cargar personas", e.getMessage());
        }
    }

    /**
     * Limpiar el formulario
     */
    private void limpiarFormulario() {
        txtNombre.clear();
        txtDireccion.clear();
        txtDireccion.setStyle("");
        txtTelefono.clear();
        listaTelefonos.getItems().clear();
        personaSeleccionada = null;
        tablaPersonas.getSelectionModel().clearSelection();
    }

    /**
     * Controlar el estado de los botones
     */
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
            if (!validarFormulario()) return;

            Persona nuevaPersona = crearPersonaDesdeFormulario();

            if (personaService.crearPersona(nuevaPersona)) {
                mostrarExito("Persona guardada correctamente",
                        "Para agregar direcciones, seleccione la persona y haga doble clic en el campo de dirección.");

                cargarPersonas();
                limpiarFormulario();
                habilitarBotones(false, false, false);
            } else {
                mostrarError("Error", "No se pudo guardar la persona");
            }

        } catch (IllegalArgumentException e) {
            mostrarAdvertencia("Error de validación", e.getMessage());
        } catch (Exception e) {
            System.err.println("Error al guardar: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error inesperado", e.getMessage());
        }
    }

    @FXML
    private void actualizarRegistro() {
        if (personaSeleccionada == null) {
            mostrarAdvertencia("Error", "No hay persona seleccionada");
            return;
        }

        try {
            if (!validarFormulario()) return;

            actualizarPersonaDesdeFormulario(personaSeleccionada);

            if (personaService.actualizarPersona(personaSeleccionada)) {
                mostrarExito("Persona actualizada correctamente",
                        "Las direcciones se gestionan por separado haciendo doble clic en el campo de dirección.");

                cargarPersonas();
                tablaPersonas.getSelectionModel().select(personaSeleccionada);
            } else {
                mostrarError("Error", "No se pudo actualizar la persona");
            }

        } catch (IllegalArgumentException e) {
            mostrarAdvertencia("Error de validación", e.getMessage());
        } catch (Exception e) {
            System.err.println("Error al actualizar: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error inesperado", e.getMessage());
        }
    }

    @FXML
    private void eliminarRegistro() {
        if (personaSeleccionada == null) {
            mostrarAdvertencia("Error", "No hay persona seleccionada");
            return;
        }

        if (confirmarEliminacion()) {
            if (personaService.eliminarPersona(personaSeleccionada.getId())) {
                mostrarExito("Persona eliminada correctamente", null);
                cargarPersonas();
                limpiarFormulario();
                habilitarBotones(false, false, false);
            } else {
                mostrarError("Error", "No se pudo eliminar la persona");
            }
        }
    }

    @FXML
    private void agregarTelefono() {
        String telefono = txtTelefono.getText().trim();
        if (telefono.isEmpty()) {
            mostrarAdvertencia("Error", "Ingrese un número de teléfono");
            return;
        }

        try {
            // Verificar duplicados
            for (Telefono tel : listaTelefonos.getItems()) {
                if (tel.getTelefono().equals(telefono)) {
                    mostrarAdvertencia("Error", "Este teléfono ya existe");
                    return;
                }
            }

            // Validación básica
            if (!telefono.matches(".*\\d.*")) {
                mostrarAdvertencia("Error", "El teléfono debe contener al menos un dígito");
                return;
            }

            Telefono nuevoTelefono = new Telefono(0, telefono);
            listaTelefonos.getItems().add(nuevoTelefono);
            txtTelefono.clear();

        } catch (Exception e) {
            mostrarError("Error al agregar teléfono", e.getMessage());
        }
    }

    @FXML
    private void eliminarTelefono() {
        Telefono telefonoSeleccionado = listaTelefonos.getSelectionModel().getSelectedItem();
        if (telefonoSeleccionado == null) {
            mostrarAdvertencia("Error", "Seleccione un teléfono para eliminar");
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

    /**
     * Abrir la ventana de gestión de direcciones
     */
    private void abrirGestionDirecciones() {
        if (personaSeleccionada == null) {
            mostrarAdvertencia("Error", "Primero debe guardar la persona para poder gestionar sus direcciones");
            return;
        }

        try {
            System.out.println("Abriendo gestión de direcciones para: " + personaSeleccionada.getNombre());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/agenda/secondary.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Gestión de Direcciones - " + personaSeleccionada.getNombre());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1200, 700));

            // Si existe un controlador de direcciones, configurarlo aquí
            // DireccionesController controller = loader.getController();
            // controller.setPersonaSeleccionada(personaSeleccionada);

            stage.showAndWait();

            // Recargar datos después de cerrar la ventana
            cargarPersonas();
            if (personaSeleccionada != null) {
                tablaPersonas.getSelectionModel().select(personaSeleccionada);
            }

        } catch (Exception e) {
            System.err.println("Error al abrir gestión de direcciones: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir la gestión de direcciones: " + e.getMessage());
        }
    }

    /**
     * Editar teléfono seleccionado
     */
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

            // Verificar duplicados
            for (Telefono tel : listaTelefonos.getItems()) {
                if (tel != telefonoSeleccionado && tel.getTelefono().equals(nuevoTelefono)) {
                    mostrarAdvertencia("Error", "Este teléfono ya existe");
                    return;
                }
            }

            telefonoSeleccionado.setTelefono(nuevoTelefono);
            listaTelefonos.refresh();
        }
    }

    /**
     * Crear una persona desde los datos del formulario
     */
    private Persona crearPersonaDesdeFormulario() {
        Persona nuevaPersona = new Persona(txtNombre.getText().trim());

        // Agregar teléfonos
        for (Telefono tel : listaTelefonos.getItems()) {
            nuevaPersona.addTelefono(new Telefono(0, tel.getTelefono()));
        }

        return nuevaPersona;
    }

    /**
     * Actualizar una persona con los datos del formulario
     */
    private void actualizarPersonaDesdeFormulario(Persona persona) {
        persona.setNombre(txtNombre.getText().trim());

        // Actualizar teléfonos
        persona.clearTelefonos();
        for (Telefono tel : listaTelefonos.getItems()) {
            persona.addTelefono(new Telefono(0, tel.getTelefono()));
        }
    }

    /**
     * Validar los datos del formulario
     */
    private boolean validarFormulario() {
        if (txtNombre.getText().trim().isEmpty()) {
            mostrarAdvertencia("Error de validación", "El nombre es obligatorio");
            txtNombre.requestFocus();
            return false;
        }

        if (txtNombre.getText().trim().length() > 100) {
            mostrarAdvertencia("Error de validación", "El nombre no puede exceder 100 caracteres");
            txtNombre.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Confirmar eliminación con diálogo
     */
    private boolean confirmarEliminacion() {
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
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    // Métodos de utilidad para mostrar mensajes (SRP)
    private void mostrarMensaje(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarExito(String titulo, String mensaje) {
        mostrarMensaje(titulo, mensaje, Alert.AlertType.INFORMATION);
    }

    private void mostrarError(String titulo, String mensaje) {
        mostrarMensaje(titulo, mensaje, Alert.AlertType.ERROR);
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        mostrarMensaje(titulo, mensaje, Alert.AlertType.WARNING);
    }
}