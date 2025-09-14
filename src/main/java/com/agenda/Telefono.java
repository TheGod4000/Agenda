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

/**
 * Clase modelo que representa un Teléfono
 */
public class Telefono {
    private final IntegerProperty id;
    private final IntegerProperty personaId;
    private final StringProperty telefono;

    public Telefono() {
        this(0, 0, "");
    }

    public Telefono(int personaId, String telefono) {
        this(0, personaId, telefono);
    }

    public Telefono(int id, int personaId, String telefono) {
        this.id = new SimpleIntegerProperty(id);
        this.personaId = new SimpleIntegerProperty(personaId);
        this.telefono = new SimpleStringProperty(telefono != null ? telefono : "");
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

    // Getters y Setters para PersonaId
    public int getPersonaId() {
        return personaId.get();
    }

    public void setPersonaId(int personaId) {
        this.personaId.set(personaId);
    }

    public IntegerProperty personaIdProperty() {
        return personaId;
    }

    // Getters y Setters para Telefono
    public String getTelefono() {
        return telefono.get();
    }

    public void setTelefono(String telefono) {
        this.telefono.set(telefono != null ? telefono : "");
    }

    public StringProperty telefonoProperty() {
        return telefono;
    }

    @Override
    public String toString() {
        return getTelefono();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Telefono telefono = (Telefono) obj;
        return getId() == telefono.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }
}