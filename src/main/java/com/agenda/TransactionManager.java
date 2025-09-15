// TransactionManager.java - Implementación del manejo de transacciones
package com.agenda;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Clase responsable únicamente del manejo de transacciones de base de datos
 * Aplica el Principio de Responsabilidad Única (SRP)
 */
public class TransactionManager implements ITransactionManager {
    private final DatabaseConnection dbConnection;

    public TransactionManager() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public TransactionManager(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Ejecuta una operación dentro de una transacción y retorna un valor
     */
    @Override
    public <T> T executeInTransaction(Function<Connection, T> operation) throws RuntimeException {
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                T result = operation.apply(conn);
                conn.commit();
                return result;

            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Error en transacción: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo obtener la conexión para la transacción: " + e.getMessage(), e);
        }
    }

    /**
     * Ejecuta una operación dentro de una transacción sin retornar valor
     */
    @Override
    public void executeInTransaction(TransactionOperation operation) throws RuntimeException {
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                operation.execute(conn);
                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Error en transacción: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo obtener la conexión para la transacción: " + e.getMessage(), e);
        }
    }
}