/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.agenda;

/**
 *
 * @author Luisg
 */
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Clase modelo que representa una Persona
 */
public class Persona {
    private final IntegerProperty id;
    private final StringProperty nombre;
    private final StringProperty direccion;
    private final ObservableList<Telefono> telefonos;

    public Persona() {
        this(0, "", "");
    }

    public Persona(String nombre, String direccion) {
        this(0, nombre, direccion);
    }

    public Persona(int id, String nombre, String direccion) {
        this.id = new SimpleIntegerProperty(id);
        this.nombre = new SimpleStringProperty(nombre != null ? nombre : "");
        this.direccion = new SimpleStringProperty(direccion != null ? direccion : "");
        this.telefonos = FXCollections.observableArrayList();
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

    // Getters y Setters para Dirección
    public String getDireccion() {
        return direccion.get();
    }

    public void setDireccion(String direccion) {
        this.direccion.set(direccion != null ? direccion : "");
    }

    public StringProperty direccionProperty() {
        return direccion;
    }

    // Métodos para manejar teléfonos
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

    /**
     * Obtiene los teléfonos como cadena separada por comas
     * Útil para mostrar en la tabla
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

    @Override
    public String toString() {
        return "Persona{" +
                "id=" + getId() +
                ", nombre='" + getNombre() + '\'' +
                ", direccion='" + getDireccion() + '\'' +
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