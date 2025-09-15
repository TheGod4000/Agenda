package com.agenda;

import java.sql.Connection;
import java.util.function.Function;

public interface ITransactionManager {
    <T> T executeInTransaction(Function<Connection, T> operation) throws RuntimeException;
    void executeInTransaction(TransactionOperation operation) throws RuntimeException;

    @FunctionalInterface
    interface TransactionOperation {
        void execute(Connection connection) throws Exception;
    }
}