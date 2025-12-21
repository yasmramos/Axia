module io.github.yasmramos.axia {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    
    requires io.ebean.api;
    requires io.ebean.core;
    requires io.ebean.datasource;
    requires java.sql;
    requires static jakarta.persistence;
    
    requires org.slf4j;
    requires com.zaxxer.hikari;
    requires org.postgresql.jdbc;
    
    opens io.github.yasmramos.axia to javafx.fxml;
    opens io.github.yasmramos.axia.ui.controller to javafx.fxml;
    opens io.github.yasmramos.axia.model to io.ebean.api, io.ebean.core;
    
    exports io.github.yasmramos.axia;
    exports io.github.yasmramos.axia.ui.controller;
    exports io.github.yasmramos.axia.model;
    exports io.github.yasmramos.axia.service;
    exports io.github.yasmramos.axia.repository;
}
