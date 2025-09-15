/*
 * Controlador para la gestión de direcciones
 */
package com.agenda;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador para gestionar direcciones compartidas
 */
public class DireccionesController implements Initializable {

    @FXML private TableView<Direccion> tablaDirecciones;
    @FXML private TableColumn<Direccion, String> columnaCalle;
    @FXML private TableColumn<Direccion, String> columnaCiudad;
    @FXML private TableColumn<Direccion, String> columnaEstado;
    @FXML private TableColumn<Direccion, Integer> columnaPersonas;

    @FXML private TableView<PersonaConRelacion> tablaPersonasCompartidas;
    @FXML private TableColumn<PersonaConRelacion, String> columnaNombrePersona;
    @FXML private TableColumn<PersonaConRelacion, String> columnaEtiqueta;
    @FXML private TableColumn<PersonaConRelacion, String> columnaPrincipal;

    @FXML private TextField txtCalle;
    @FXML private TextField txtCiudad;
    @FXML private TextField txtEstado;
    @FXML private TextField txtCodigoPostal;
    @FXML private TextField txtPais;
    @FXML private TextField txtBuscarDireccion;

    @FXML private Button btnNuevaDireccion;
    @FXML private Button btnEditarDireccion;
    @FXML private Button btnEliminarDireccion;
    @FXML private Button btnGuardarDireccion;
    @FXML private Button btnCancelarDireccion;
    @FXML private Button btnAsociarPersona;
    @FXML private Button btnDesasociarPersona;
    @FXML private Button btnEditarRelacion;
    @FXML private Button btnVerCompartidas;
    @FXML private Button btnBuscarDireccion;
    @FXML private Button btnVolver;

    private DireccionService direccionService;
    private PersonaService personaService;
    private PersonaDireccionDAO personaDireccionDAO;
    private Direccion direccionSeleccionada;
    private boolean modoEdicion = false;

    // Clase auxiliar para mostrar personas con información de relación
    public static class PersonaConRelacion {
        private final Persona persona;
        private final PersonaDireccion relacion;

        public PersonaConRelacion(Persona persona, PersonaDireccion relacion) {
            this.persona = persona;
            this.relacion = relacion;
        }

        public String getNombre() {
            return persona.getNombre();
        }

        public String getEtiqueta() {
            return relacion.getEtiqueta();
        }

        public String getPrincipal() {
            return relacion.isEsPrincipal() ? "Sí" : "No";
        }

        public Persona getPersona() { return persona; }
        public PersonaDireccion getRelacion() { return relacion; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Inicializando DireccionesController...");

        direccionService = new DireccionService();
        personaService = new PersonaService();
        personaDireccionDAO = new PersonaDireccionDAO();

        // Configurar columnas de la tabla de direcciones
        columnaCalle.setCellValueFactory(cellData -> cellData.getValue().calleProperty());
        columnaCiudad.setCellValueFactory(cellData -> cellData.getValue().ciudadProperty());
        columnaEstado.setCellValueFactory(cellData -> cellData.getValue().estadoProperty());

        // Columna personalizada para mostrar cantidad de personas
        columnaPersonas.setCellValueFactory(cellData -> {
            Direccion direccion = cellData.getValue();
            int cantidad = personaDireccionDAO.contarPersonasPorDireccion(direccion.getId());
            return new javafx.beans.property.SimpleIntegerProperty(cantidad).asObject();
        });

        // Configurar columnas de la tabla de personas compartidas
        columnaNombrePersona.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        columnaEtiqueta.setCellValueFactory(new PropertyValueFactory<>("etiqueta"));
        columnaPrincipal.setCellValueFactory(new PropertyValueFactory<>("principal"));

        // Configurar eventos
        configurarEventos();

        // Cargar datos iniciales
        cargarDirecciones();

        // Estado inicial
        limpiarFormulario();
        habilitarBotones(false, false, false, false, false);

        System.out.println("DireccionesController inicializado correctamente");
    }

    private void configurarEventos() {
        // Selección en tabla de direcciones
        tablaDirecciones.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    direccionSeleccionada = newValue;
                    if (newValue != null) {
                        System.out.println("Dirección seleccionada: " + newValue.getDireccionCompleta());
                        cargarDireccionEnFormulario(newValue);
                        cargarPersonasCompartidas(newValue.getId());
                        habilitarBotones(true, true, true, false, true);
                    } else {
                        limpiarFormulario();
                        tablaPersonasCompartidas.getItems().clear();
                        habilitarBotones(false, false, false, false, false);
                    }
                }
        );

        // Eventos de botones principales
        btnNuevaDireccion.setOnAction(e -> nuevaDireccion());
        btnEditarDireccion.setOnAction(e -> editarDireccion());
        btnEliminarDireccion.setOnAction(e -> eliminarDireccion());
        btnGuardarDireccion.setOnAction(e -> guardarDireccion());
        btnCancelarDireccion.setOnAction(e -> cancelarEdicion());

        // Eventos de gestión de asociaciones
        btnAsociarPersona.setOnAction(e -> mostrarDialogoAsociarPersona());
        btnDesasociarPersona.setOnAction(e -> desasociarPersona());
        btnEditarRelacion.setOnAction(e -> editarRelacion());

        // Eventos de búsqueda y navegación
        btnBuscarDireccion.setOnAction(e -> buscarDirecciones());
        btnVerCompartidas.setOnAction(e -> mostrarSoloCompartidas());
        btnVolver.setOnAction(e -> volverAPersonas());

        // Búsqueda en tiempo real
        txtBuscarDireccion.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                buscarDirecciones();
            } else {
                cargarDirecciones();
            }
        });
    }

    private void cargarDirecciones() {
        try {
            System.out.println("Cargando direcciones...");
            ObservableList<Direccion> direcciones = direccionService.obtenerTodasDirecciones();
            System.out.println("Direcciones cargadas: " + direcciones.size());

            tablaDirecciones.setItems(direcciones);
            tablaDirecciones.refresh();

        } catch (Exception e) {
            System.err.println("Error al cargar direcciones: " + e.getMessage());
            e.printStackTrace();
            mostrarMensaje("Error", "Error al cargar direcciones: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void cargarDireccionEnFormulario(Direccion direccion) {
        if (direccion != null) {
            txtCalle.setText(direccion.getCalle());
            txtCiudad.setText(direccion.getCiudad());
            txtEstado.setText(direccion.getEstado());
            txtCodigoPostal.setText(direccion.getCodigoPostal());
            txtPais.setText(direccion.getPais());

            System.out.println("Formulario cargado para: " + direccion.getDireccionCompleta());
        }
    }

    private void cargarPersonasCompartidas(int direccionId) {
        try {
            List<PersonaDireccion> relaciones = personaDireccionDAO.obtenerPorDireccionId(direccionId);
            ObservableList<PersonaConRelacion> personasConRelacion = FXCollections.observableArrayList();

            for (PersonaDireccion relacion : relaciones) {
                Persona persona = personaService.obtenerPersona(relacion.getPersonaId());
                if (persona != null) {
                    personasConRelacion.add(new PersonaConRelacion(persona, relacion));
                }
            }

            tablaPersonasCompartidas.setItems(personasConRelacion);
            System.out.println("Cargadas " + personasConRelacion.size() + " personas que comparten la dirección");

        } catch (Exception e) {
            System.err.println("Error al cargar personas compartidas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void limpiarFormulario() {
        txtCalle.clear();
        txtCiudad.clear();
        txtEstado.clear();
        txtCodigoPostal.clear();
        txtPais.clear();
        direccionSeleccionada = null;
        modoEdicion = false;

        // Habilitar/deshabilitar campos
        habilitarCampos(false);
    }

    private void habilitarCampos(boolean habilitar) {
        txtCalle.setDisable(!habilitar);
        txtCiudad.setDisable(!habilitar);
        txtEstado.setDisable(!habilitar);
        txtCodigoPostal.setDisable(!habilitar);
        txtPais.setDisable(!habilitar);
    }

    private void habilitarBotones(boolean editar, boolean eliminar, boolean asociar, boolean guardar, boolean desasociar) {
        btnEditarDireccion.setDisable(!editar);
        btnEliminarDireccion.setDisable(!eliminar);
        btnAsociarPersona.setDisable(!asociar);
        btnGuardarDireccion.setDisable(!guardar);
        btnDesasociarPersona.setDisable(!desasociar);
        btnCancelarDireccion.setDisable(!guardar);
    }

    @FXML
    private void nuevaDireccion() {
        limpiarFormulario();
        modoEdicion = true;
        habilitarCampos(true);
        habilitarBotones(false, false, false, true, false);
        txtCalle.requestFocus();

        // Limpiar selección de tabla
        tablaDirecciones.getSelectionModel().clearSelection();
        tablaPersonasCompartidas.getItems().clear();
    }

    @FXML
    private void editarDireccion() {
        if (direccionSeleccionada == null) {
            mostrarMensaje("Error", "No hay dirección seleccionada", Alert.AlertType.WARNING);
            return;
        }

        modoEdicion = true;
        habilitarCampos(true);
        habilitarBotones(false, false, false, true, false);
        txtCalle.requestFocus();
    }

    @FXML
    private void guardarDireccion() {
        try {
            if (validarCampos()) {
                if (direccionSeleccionada == null) {
                    // Crear nueva dirección
                    Direccion nuevaDireccion = new Direccion(
                            txtCalle.getText().trim(),
                            txtCiudad.getText().trim(),
                            txtEstado.getText().trim(),
                            txtCodigoPostal.getText().trim(),
                            txtPais.getText().trim()
                    );

                    if (direccionService.crearDireccion(nuevaDireccion)) {
                        mostrarMensaje("Éxito", "Dirección guardada correctamente", Alert.AlertType.INFORMATION);
                        cargarDirecciones();
                        cancelarEdicion();
                    } else {
                        mostrarMensaje("Error", "No se pudo guardar la dirección", Alert.AlertType.ERROR);
                    }
                } else {
                    // Actualizar dirección existente
                    direccionSeleccionada.setCalle(txtCalle.getText().trim());
                    direccionSeleccionada.setCiudad(txtCiudad.getText().trim());
                    direccionSeleccionada.setEstado(txtEstado.getText().trim());
                    direccionSeleccionada.setCodigoPostal(txtCodigoPostal.getText().trim());
                    direccionSeleccionada.setPais(txtPais.getText().trim());

                    if (direccionService.actualizarDireccion(direccionSeleccionada)) {
                        mostrarMensaje("Éxito", "Dirección actualizada correctamente", Alert.AlertType.INFORMATION);
                        cargarDirecciones();
                        cancelarEdicion();
                    } else {
                        mostrarMensaje("Error", "No se pudo actualizar la dirección", Alert.AlertType.ERROR);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            mostrarMensaje("Error de validación", e.getMessage(), Alert.AlertType.WARNING);
        } catch (Exception e) {
            System.err.println("Error al guardar dirección: " + e.getMessage());
            e.printStackTrace();
            mostrarMensaje("Error", "Error inesperado: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void cancelarEdicion() {
        modoEdicion = false;
        habilitarCampos(false);

        if (direccionSeleccionada != null) {
            cargarDireccionEnFormulario(direccionSeleccionada);
            habilitarBotones(true, true, true, false, true);
        } else {
            limpiarFormulario();
            habilitarBotones(false, false, false, false, false);
        }
    }

    @FXML
    private void eliminarDireccion() {
        if (direccionSeleccionada == null) {
            mostrarMensaje("Error", "No hay dirección seleccionada", Alert.AlertType.WARNING);
            return;
        }

        // Verificar si está en uso
        int cantidadPersonas = personaDireccionDAO.contarPersonasPorDireccion(direccionSeleccionada.getId());

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro de eliminar esta dirección?");

        if (cantidadPersonas > 0) {
            confirmacion.setContentText(
                    "Esta dirección está siendo usada por " + cantidadPersonas + " persona(s).\n" +
                            "Al eliminarla, se quitará de todas las personas.\n" +
                            "Esta acción no se puede deshacer."
            );
        } else {
            confirmacion.setContentText("Esta acción no se puede deshacer.");
        }

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                if (cantidadPersonas > 0) {
                    // Eliminar todas las asociaciones primero
                    personaDireccionDAO.eliminarPorPersonaId(direccionSeleccionada.getId());
                }

                if (direccionService.eliminarDireccion(direccionSeleccionada.getId())) {
                    mostrarMensaje("Éxito", "Dirección eliminada correctamente", Alert.AlertType.INFORMATION);
                    cargarDirecciones();
                    limpiarFormulario();
                    tablaPersonasCompartidas.getItems().clear();
                    habilitarBotones(false, false, false, false, false);
                } else {
                    mostrarMensaje("Error", "No se pudo eliminar la dirección", Alert.AlertType.ERROR);
                }
            } catch (IllegalArgumentException e) {
                mostrarMensaje("Error", e.getMessage(), Alert.AlertType.WARNING);
            } catch (Exception e) {
                System.err.println("Error al eliminar dirección: " + e.getMessage());
                e.printStackTrace();
                mostrarMensaje("Error", "Error inesperado: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void mostrarDialogoAsociarPersona() {
        if (direccionSeleccionada == null) {
            mostrarMensaje("Error", "No hay dirección seleccionada", Alert.AlertType.WARNING);
            return;
        }

        // Crear diálogo personalizado para asociar persona
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Asociar Persona a Dirección");
        dialog.setHeaderText("Seleccione la persona y configure la asociación");

        // Crear controles del diálogo
        VBox content = new VBox(10);

        ComboBox<Persona> comboPersonas = new ComboBox<>();
        comboPersonas.setItems(personaService.obtenerTodasPersonas());
        comboPersonas.setConverter(new javafx.util.StringConverter<Persona>() {
            @Override
            public String toString(Persona persona) {
                return persona != null ? persona.getNombre() : "";
            }

            @Override
            public Persona fromString(String string) {
                return null;
            }
        });

        TextField txtEtiquetaAsoc = new TextField();
        txtEtiquetaAsoc.setPromptText("Casa, Trabajo, Oficina, etc.");
        txtEtiquetaAsoc.setText("Casa");

        CheckBox chkPrincipal = new CheckBox("Establecer como dirección principal");

        content.getChildren().addAll(
                new Label("Persona:"),
                comboPersonas,
                new Label("Etiqueta:"),
                txtEtiquetaAsoc,
                chkPrincipal
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Persona personaSeleccionadaDialog = comboPersonas.getValue();
            String etiqueta = txtEtiquetaAsoc.getText().trim();
            boolean esPrincipal = chkPrincipal.isSelected();

            if (personaSeleccionadaDialog == null) {
                mostrarMensaje("Error", "Debe seleccionar una persona", Alert.AlertType.WARNING);
                return;
            }

            if (etiqueta.isEmpty()) {
                etiqueta = "Dirección";
            }

            try {
                if (direccionService.asociarDireccionAPersona(
                        personaSeleccionadaDialog.getId(),
                        direccionSeleccionada.getId(),
                        etiqueta,
                        esPrincipal)) {

                    mostrarMensaje("Éxito", "Persona asociada correctamente", Alert.AlertType.INFORMATION);
                    cargarPersonasCompartidas(direccionSeleccionada.getId());
                    tablaDirecciones.refresh(); // Para actualizar el contador
                } else {
                    mostrarMensaje("Error", "No se pudo asociar la persona", Alert.AlertType.ERROR);
                }
            } catch (IllegalArgumentException e) {
                mostrarMensaje("Error", e.getMessage(), Alert.AlertType.WARNING);
            }
        }
    }

    @FXML
    private void desasociarPersona() {
        PersonaConRelacion seleccionada = tablaPersonasCompartidas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Error", "Seleccione una persona para desasociar", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar desasociación");
        confirmacion.setHeaderText("¿Desasociar esta dirección de " + seleccionada.getNombre() + "?");
        confirmacion.setContentText("Esta acción no se puede deshacer.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            if (direccionService.desasociarDireccionDePersona(
                    seleccionada.getPersona().getId(),
                    direccionSeleccionada.getId())) {

                mostrarMensaje("Éxito", "Persona desasociada correctamente", Alert.AlertType.INFORMATION);
                cargarPersonasCompartidas(direccionSeleccionada.getId());
                tablaDirecciones.refresh();
            } else {
                mostrarMensaje("Error", "No se pudo desasociar la persona", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void editarRelacion() {
        PersonaConRelacion seleccionada = tablaPersonasCompartidas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Error", "Seleccione una relación para editar", Alert.AlertType.WARNING);
            return;
        }

        // Diálogo para editar la relación
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Relación");
        dialog.setHeaderText("Editar relación de " + seleccionada.getNombre());

        VBox content = new VBox(10);

        TextField txtEtiquetaEdit = new TextField();
        txtEtiquetaEdit.setText(seleccionada.getEtiqueta());

        CheckBox chkPrincipalEdit = new CheckBox("Establecer como dirección principal");
        chkPrincipalEdit.setSelected(seleccionada.getRelacion().isEsPrincipal());

        content.getChildren().addAll(
                new Label("Etiqueta:"),
                txtEtiquetaEdit,
                chkPrincipalEdit
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String nuevaEtiqueta = txtEtiquetaEdit.getText().trim();
            boolean nuevoPrincipal = chkPrincipalEdit.isSelected();

            if (nuevaEtiqueta.isEmpty()) {
                nuevaEtiqueta = "Dirección";
            }

            try {
                PersonaDireccion relacion = seleccionada.getRelacion();
                relacion.setEtiqueta(nuevaEtiqueta);
                relacion.setEsPrincipal(nuevoPrincipal);

                if (direccionService.actualizarRelacionPersonaDireccion(relacion)) {
                    mostrarMensaje("Éxito", "Relación actualizada correctamente", Alert.AlertType.INFORMATION);
                    cargarPersonasCompartidas(direccionSeleccionada.getId());
                } else {
                    mostrarMensaje("Error", "No se pudo actualizar la relación", Alert.AlertType.ERROR);
                }
            } catch (IllegalArgumentException e) {
                mostrarMensaje("Error", e.getMessage(), Alert.AlertType.WARNING);
            }
        }
    }

    @FXML
    private void buscarDirecciones() {
        String textoBusqueda = txtBuscarDireccion.getText().trim();
        System.out.println("Buscando direcciones: " + textoBusqueda);

        ObservableList<Direccion> resultados = direccionService.buscarDirecciones(textoBusqueda);
        System.out.println("Direcciones encontradas: " + resultados.size());

        tablaDirecciones.setItems(resultados);
        tablaDirecciones.refresh();
    }

    @FXML
    private void mostrarSoloCompartidas() {
        ObservableList<Direccion> todasDirecciones = direccionService.obtenerTodasDirecciones();
        ObservableList<Direccion> compartidas = FXCollections.observableArrayList();

        for (Direccion direccion : todasDirecciones) {
            int cantidadPersonas = personaDireccionDAO.contarPersonasPorDireccion(direccion.getId());
            if (cantidadPersonas > 1) {
                compartidas.add(direccion);
            }
        }

        tablaDirecciones.setItems(compartidas);
        System.out.println("Mostrando " + compartidas.size() + " direcciones compartidas");
    }

    @FXML
    private void volverAPersonas() {
        // Aquí implementarías la navegación de vuelta a la vista principal
        // Por ejemplo, usando el sistema de navegación de tu aplicación
        System.out.println("Volviendo a la vista de personas...");
        // App.setRoot("primary"); // Si usas el sistema de navegación del App.java original
    }

    private boolean validarCampos() {
        if ((txtCalle.getText() == null || txtCalle.getText().trim().isEmpty()) &&
                (txtCiudad.getText() == null || txtCiudad.getText().trim().isEmpty())) {
            mostrarMensaje("Error de validación", "Debe especificar al menos la calle o la ciudad", Alert.AlertType.WARNING);
            return false;
        }

        if (txtCalle.getText() != null && txtCalle.getText().length() > 200) {
            mostrarMensaje("Error de validación", "La calle no puede exceder 200 caracteres", Alert.AlertType.WARNING);
            txtCalle.requestFocus();
            return false;
        }

        if (txtCiudad.getText() != null && txtCiudad.getText().length() > 100) {
            mostrarMensaje("Error de validación", "La ciudad no puede exceder 100 caracteres", Alert.AlertType.WARNING);
            txtCiudad.requestFocus();
            return false;
        }

        if (txtEstado.getText() != null && txtEstado.getText().length() > 100) {
            mostrarMensaje("Error de validación", "El estado no puede exceder 100 caracteres", Alert.AlertType.WARNING);
            txtEstado.requestFocus();
            return false;
        }

        if (txtCodigoPostal.getText() != null && txtCodigoPostal.getText().length() > 20) {
            mostrarMensaje("Error de validación", "El código postal no puede exceder 20 caracteres", Alert.AlertType.WARNING);
            txtCodigoPostal.requestFocus();
            return false;
        }

        if (txtPais.getText() != null && txtPais.getText().length() > 100) {
            mostrarMensaje("Error de validación", "El país no puede exceder 100 caracteres", Alert.AlertType.WARNING);
            txtPais.requestFocus();
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