/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.agenda;

/**
 * Configuración de la base de datos
 * @author Luisg
 */
public class DatabaseConfig {
    public static final String URL = "jdbc:mysql://localhost:3306/agenda?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    public static final String USERNAME = "root";
    public static final String PASSWORD = "minecraft";
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    
    // Configuración para pruebas
    public static final String TEST_URL = "jdbc:mysql://localhost:3306/agenda_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    public static final String TEST_USERNAME = "root";
    public static final String TEST_PASSWORD = "minecraft";
}