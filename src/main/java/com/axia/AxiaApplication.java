package com.axia;

import com.axia.config.DatabaseConfig;
import com.axia.service.AccountService;
import com.axia.service.FiscalYearService;

/**
 * Axia - Sistema Contable
 * 
 * Punto de entrada principal para inicialización de la base de datos
 * y configuración inicial del sistema.
 */
public class AxiaApplication {

    public static void initialize() {
        // Inicializar la conexión a la base de datos
        DatabaseConfig.getDatabase();

        // Inicializar datos por defecto
        AccountService accountService = new AccountService();
        accountService.initializeDefaultAccounts();

        FiscalYearService fiscalYearService = new FiscalYearService();
        fiscalYearService.initializeCurrentYear();
    }

    public static void shutdown() {
        DatabaseConfig.shutdown();
    }

    public static void main(String[] args) {
        System.out.println("Inicializando Axia - Sistema Contable...");
        
        try {
            initialize();
            System.out.println("Sistema inicializado correctamente.");
            System.out.println("Plan de cuentas y año fiscal creados.");
        } catch (Exception e) {
            System.err.println("Error al inicializar: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }
}
