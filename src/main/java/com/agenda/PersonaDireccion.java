/*
 * Clase que representa la relación entre Persona y Dirección
 */
package com.agenda;

import javafx.beans.property.*;

/**
 * Clase que representa la relación muchos-a-muchos entre Persona y Dirección
 */
public class PersonaDireccion {
    private final IntegerProperty id;
    private final IntegerProperty personaId;
    private final IntegerProperty direccionId;
    private final StringProperty etiqueta; // ej: "Casa", "Trabajo", "Oficina", etc.
    private final BooleanProperty esPrincipal; // indica si es la dirección principal

    public PersonaDireccion() {
        this(0, 0, 0, "", false);
    }

    public PersonaDireccion(int personaId, int direccionId, String etiqueta, boolean esPrincipal) {
        this(0, personaId, direccionId, etiqueta, esPrincipal);
    }

    public PersonaDireccion(int id, int personaId, int direccionId, String etiqueta, boolean esPrincipal) {
        this.id = new SimpleIntegerProperty(id);
        this.personaId = new SimpleIntegerProperty(personaId);
        this.direccionId = new SimpleIntegerProperty(direccionId);
        this.etiqueta = new SimpleStringProperty(etiqueta != null ? etiqueta : "");
        this.esPrincipal = new SimpleBooleanProperty(esPrincipal);
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

    // Getters y Setters para DireccionId
    public int getDireccionId() {
        return direccionId.get();
    }

    public void setDireccionId(int direccionId) {
        this.direccionId.set(direccionId);
    }

    public IntegerProperty direccionIdProperty() {
        return direccionId;
    }

    // Getters y Setters para Etiqueta
    public String getEtiqueta() {
        return etiqueta.get();
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta.set(etiqueta != null ? etiqueta : "");
    }

    public StringProperty etiquetaProperty() {
        return etiqueta;
    }

    // Getters y Setters para EsPrincipal
    public boolean isEsPrincipal() {
        return esPrincipal.get();
    }

    public void setEsPrincipal(boolean esPrincipal) {
        this.esPrincipal.set(esPrincipal);
    }

    public BooleanProperty esPrincipalProperty() {
        return esPrincipal;
    }

    @Override
    public String toString() {
        return getEtiqueta() + (isEsPrincipal() ? " (Principal)" : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PersonaDireccion that = (PersonaDireccion) obj;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }
}
