package com.agenda;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador principal de la aplicación - Versión corregida
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
    private Persona personaSeleccionada;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Inicializando MainController...");
        
        personaService = new PersonaService();
        
        // Configurar columnas de la tabla usando Properties
        columnaNombre.setCellValueFactory(cellData -> cellData.getValue().nombreProperty());
        columnaDireccion.setCellValueFactory(cellData -> cellData.getValue().direccionProperty());
        
        // Columna personalizada para mostrar teléfonos
        columnaTelefonos.setCellValueFactory(cellData -> {
            Persona persona = cellData.getValue();
            StringBuilder telefonos = new StringBuilder();
            for (Telefono tel : persona.getTelefonos()) {
                if (telefonos.length() > 0) telefonos.append(", ");
                telefonos.append(tel.getTelefono());
            }
            return new javafx.beans.property.SimpleStringProperty(telefonos.toString());
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
    }
    
    private void cargarPersonas() {
        try {
            System.out.println("Obteniendo personas de la base de datos...");
            ObservableList<Persona> personas = personaService.obtenerTodasPersonas();
            System.out.println("Personas obtenidas: " + personas.size());
            
            // Debug: mostrar información de cada persona
            for (int i = 0; i < personas.size(); i++) {
                Persona p = personas.get(i);
                System.out.println("Persona " + (i+1) + ": " + p.getNombre() + 
                                 " - Dirección: " + p.getDireccion() + 
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
            txtDireccion.setText(persona.getDireccion());
            listaTelefonos.setItems(persona.getTelefonos());
            System.out.println("Formulario cargado para: " + persona.getNombre());
        }
    }
    
    private void limpiarFormulario() {
        txtNombre.clear();
        txtDireccion.clear();
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
    }
    
    @FXML
    private void guardarRegistro() {
        try {
            if (validarCampos()) {
                Persona nuevaPersona = new Persona(
                    txtNombre.getText().trim(),
                    txtDireccion.getText().trim()
                );
                
                // Agregar teléfonos
                for (Telefono tel : listaTelefonos.getItems()) {
                    nuevaPersona.addTelefono(new Telefono(0, tel.getTelefono()));
                }
                
                if (personaService.crearPersona(nuevaPersona)) {
                    mostrarMensaje("Éxito", "Persona guardada correctamente", Alert.AlertType.INFORMATION);
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
                personaSeleccionada.setDireccion(txtDireccion.getText().trim());
                
                // Actualizar teléfonos
                personaSeleccionada.clearTelefonos();
                for (Telefono tel : listaTelefonos.getItems()) {
                    personaSeleccionada.addTelefono(new Telefono(0, tel.getTelefono()));
                }
                
                if (personaService.actualizarPersona(personaSeleccionada)) {
                    mostrarMensaje("Éxito", "Persona actualizada correctamente", Alert.AlertType.INFORMATION);
                    cargarPersonas();
                    limpiarFormulario();
                    habilitarBotones(false, false, false);
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
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro de eliminar esta persona?");
        confirmacion.setContentText("Esta acción no se puede deshacer. También se eliminarán todos los teléfonos asociados.");
        
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
        
        if (txtDireccion.getText().length() > 200) {
            mostrarMensaje("Error de validación", "La dirección no puede exceder 200 caracteres", Alert.AlertType.WARNING);
            txtDireccion.requestFocus();
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