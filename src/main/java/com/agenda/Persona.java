/*
 * Clase modelo que representa una Persona - Versión actualizada con múltiples direcciones
 */
package com.agenda;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Map;

/**
 * Clase modelo que representa una Persona con soporte para múltiples direcciones
 */
public class Persona {
    private final IntegerProperty id;
    private final StringProperty nombre;
    private final ObservableList<Telefono> telefonos;
    private final ObservableList<Direccion> direcciones;
    private Map<Direccion, PersonaDireccion> relacionesDireccion; // Información adicional de cada dirección

    public Persona(String trim, String trimmed) {
        this(0, "");
    }

    public Persona(String nombre) {
        this(0, nombre);
    }

    public Persona(int id, String nombre) {
        this.id = new SimpleIntegerProperty(id);
        this.nombre = new SimpleStringProperty(nombre != null ? nombre : "");
        this.telefonos = FXCollections.observableArrayList();
        this.direcciones = FXCollections.observableArrayList();
        this.relacionesDireccion = FXCollections.observableMap(FXCollections.observableHashMap());
    }

    // Constructor de compatibilidad con el código anterior
    public Persona(int id, String nombre, String direccionAntigua) {
        this(id, nombre);
        // Si se proporciona una dirección en formato string (compatibilidad),
        // se puede convertir después usando el servicio
    }

    // Getters y Setters para ID
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    // Getters y Setters para Nombre
    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre != null ? nombre : "");
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    // Métodos para manejar teléfonos (sin cambios)
    public ObservableList<Telefono> getTelefonos() {
        return telefonos;
    }

    public void addTelefono(Telefono telefono) {
        if (telefono != null) {
            this.telefonos.add(telefono);
        }
    }

    public void removeTelefono(Telefono telefono) {
        this.telefonos.remove(telefono);
    }

    public void clearTelefonos() {
        this.telefonos.clear();
    }

    // Métodos para manejar direcciones
    public ObservableList<Direccion> getDirecciones() {
        return direcciones;
    }

    public void addDireccion(Direccion direccion) {
        if (direccion != null && !this.direcciones.contains(direccion)) {
            this.direcciones.add(direccion);
        }
    }

    public void addDireccion(Direccion direccion, PersonaDireccion relacion) {
        addDireccion(direccion);
        if (direccion != null && relacion != null) {
            this.relacionesDireccion.put(direccion, relacion);
        }
    }

    public void removeDireccion(Direccion direccion) {
        this.direcciones.remove(direccion);
        this.relacionesDireccion.remove(direccion);
    }

    public void clearDirecciones() {
        this.direcciones.clear();
        this.relacionesDireccion.clear();
    }

    // Métodos para manejar las relaciones de direcciones
    public Map<Direccion, PersonaDireccion> getRelacionesDireccion() {
        return relacionesDireccion;
    }

    public void setRelacionesDireccion(Map<Direccion, PersonaDireccion> relacionesDireccion) {
        this.relacionesDireccion = relacionesDireccion;

        // Actualizar la lista de direcciones
        this.direcciones.clear();
        this.direcciones.addAll(relacionesDireccion.keySet());
    }

    public PersonaDireccion getRelacionDireccion(Direccion direccion) {
        return relacionesDireccion.get(direccion);
    }

    /**
     * Obtiene la dirección principal de la persona
     */
    public Direccion getDireccionPrincipal() {
        for (Map.Entry<Direccion, PersonaDireccion> entry : relacionesDireccion.entrySet()) {
            if (entry.getValue().isEsPrincipal()) {
                return entry.getKey();
            }
        }
        // Si no hay principal, devolver la primera dirección si existe
        return direcciones.isEmpty() ? null : direcciones.get(0);
    }

    /**
     * Obtiene todas las direcciones como cadena separada por comas
     * Para compatibilidad con la UI existente
     */
    public String getDireccionesAsString() {
        if (direcciones.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < direcciones.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }

            Direccion dir = direcciones.get(i);
            PersonaDireccion relacion = relacionesDireccion.get(dir);

            if (relacion != null && !relacion.getEtiqueta().trim().isEmpty()) {
                sb.append(relacion.getEtiqueta()).append(": ");
            }

            sb.append(dir.getDireccionResumida());

            if (relacion != null && relacion.isEsPrincipal()) {
                sb.append(" (Principal)");
            }
        }
        return sb.toString();
    }

    /**
     * Método de compatibilidad - obtiene la dirección principal como string
     */
    public String getDireccion() {
        Direccion principal = getDireccionPrincipal();
        return principal != null ? principal.getDireccionCompleta() : "";
    }

    /**
     * Método de compatibilidad - establece una dirección desde string
     * (para migración de datos existentes)
     */
    public void setDireccion(String direccionString) {
        // Este método se mantiene para compatibilidad pero debería usarse
        // el nuevo sistema de direcciones múltiples
        if (direccionString != null && !direccionString.trim().isEmpty()) {
            // Crear una dirección simple desde el string
            Direccion dir = new Direccion(direccionString, "", "", "", "");
            PersonaDireccion relacion = new PersonaDireccion(getId(), dir.getId(), "Principal", true);
            addDireccion(dir, relacion);
        }
    }

    /**
     * Property para la dirección principal (compatibilidad con TableView)
     */
    public StringProperty direccionProperty() {
        return new SimpleStringProperty(getDireccion());
    }

    /**
     * Obtiene los teléfonos como cadena separada por comas
     * (sin cambios del código original)
     */
    public String getTelefonosAsString() {
        if (telefonos.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < telefonos.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(telefonos.get(i).getTelefono());
        }
        return sb.toString();
    }

    /**
     * Cuenta el total de direcciones
     */
    public int getTotalDirecciones() {
        return direcciones.size();
    }

    /**
     * Verifica si tiene alguna dirección
     */
    public boolean tieneDirecciones() {
        return !direcciones.isEmpty();
    }

    /**
     * Obtiene una descripción resumida de las direcciones para mostrar en tablas
     */
    public String getDescripcionDirecciones() {
        if (direcciones.isEmpty()) {
            return "Sin direcciones";
        } else if (direcciones.size() == 1) {
            return direcciones.get(0).getDireccionResumida();
        } else {
            Direccion principal = getDireccionPrincipal();
            return (principal != null ? principal.getDireccionResumida() : direcciones.get(0).getDireccionResumida())
                    + " (+" + (direcciones.size() - 1) + " más)";
        }
    }

    @Override
    public String toString() {
        return "Persona{" +
                "id=" + getId() +
                ", nombre='" + getNombre() + '\'' +
                ", direcciones=" + direcciones.size() +
                ", telefonos=" + telefonos.size() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Persona persona = (Persona) obj;
        return getId() == persona.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }
}