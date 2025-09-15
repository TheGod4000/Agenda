package com.agenda;

import javafx.beans.property.*;

/**
 * Clase modelo que representa una Dirección que puede ser compartida por múltiples personas
 */
public class Direccion {
    private final IntegerProperty id;
    private final StringProperty calle;
    private final StringProperty ciudad;
    private final StringProperty estado;
    private final StringProperty codigoPostal;
    private final StringProperty pais;

    public Direccion() {
        this(0, "", "", "", "", "");
    }

    public Direccion(String calle, String ciudad, String estado, String codigoPostal, String pais) {
        this(0, calle, ciudad, estado, codigoPostal, pais);
    }

    public Direccion(int id, String calle, String ciudad, String estado, String codigoPostal, String pais) {
        this.id = new SimpleIntegerProperty(id);
        this.calle = new SimpleStringProperty(calle != null ? calle : "");
        this.ciudad = new SimpleStringProperty(ciudad != null ? ciudad : "");
        this.estado = new SimpleStringProperty(estado != null ? estado : "");
        this.codigoPostal = new SimpleStringProperty(codigoPostal != null ? codigoPostal : "");
        this.pais = new SimpleStringProperty(pais != null ? pais : "");
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

    // Getters y Setters para Calle
    public String getCalle() {
        return calle.get();
    }

    public void setCalle(String calle) {
        this.calle.set(calle != null ? calle : "");
    }

    public StringProperty calleProperty() {
        return calle;
    }

    // Getters y Setters para Ciudad
    public String getCiudad() {
        return ciudad.get();
    }

    public void setCiudad(String ciudad) {
        this.ciudad.set(ciudad != null ? ciudad : "");
    }

    public StringProperty ciudadProperty() {
        return ciudad;
    }

    // Getters y Setters para Estado
    public String getEstado() {
        return estado.get();
    }

    public void setEstado(String estado) {
        this.estado.set(estado != null ? estado : "");
    }

    public StringProperty estadoProperty() {
        return estado;
    }

    // Getters y Setters para Código Postal
    public String getCodigoPostal() {
        return codigoPostal.get();
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal.set(codigoPostal != null ? codigoPostal : "");
    }

    public StringProperty codigoPostalProperty() {
        return codigoPostal;
    }

    // Getters y Setters para País
    public String getPais() {
        return pais.get();
    }

    public void setPais(String pais) {
        this.pais.set(pais != null ? pais : "");
    }

    public StringProperty paisProperty() {
        return pais;
    }

    /**
     * Devuelve la dirección completa como una cadena formateada
     */
    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder();

        if (!getCalle().trim().isEmpty()) {
            sb.append(getCalle());
        }

        if (!getCiudad().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(getCiudad());
        }

        if (!getEstado().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(getEstado());
        }

        if (!getCodigoPostal().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(getCodigoPostal());
        }

        if (!getPais().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(getPais());
        }

        return sb.toString();
    }

    /**
     * Devuelve una versión resumida de la dirección para mostrar en tablas
     */
    public String getDireccionResumida() {
        StringBuilder sb = new StringBuilder();

        if (!getCalle().trim().isEmpty()) {
            sb.append(getCalle());
            if (getCalle().length() > 30) {
                sb.setLength(30);
                sb.append("...");
            }
        }

        if (!getCiudad().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(getCiudad());
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getDireccionCompleta();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Direccion direccion = (Direccion) obj;
        return getId() == direccion.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }

    /**
     * Verifica si esta dirección es similar a otra (mismo contenido, diferente ID)
     */
    public boolean esSimilarA(Direccion otra) {
        if (otra == null) return false;

        return getCalle().equalsIgnoreCase(otra.getCalle()) &&
                getCiudad().equalsIgnoreCase(otra.getCiudad()) &&
                getEstado().equalsIgnoreCase(otra.getEstado()) &&
                getCodigoPostal().equals(otra.getCodigoPostal()) &&
                getPais().equalsIgnoreCase(otra.getPais());
    }
}