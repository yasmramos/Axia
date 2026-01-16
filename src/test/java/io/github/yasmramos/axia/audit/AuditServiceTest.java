package io.github.yasmramos.axia.audit;

import io.github.yasmramos.axia.EmbeddedPostgresExtension;
import io.github.yasmramos.axia.model.AuditLog;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuditService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditServiceTest {

    private static AuditService auditService;

    @BeforeAll
    static void setUp() {
        auditService = new AuditService();
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(AuditLog.class);
    }

    @Test
    @Order(1)
    @DisplayName("Should log create action")
    void testLogCreate() {
        auditService.logCreate("Account", 1L, "{\"code\":\"1000\"}", "admin");
        
        List<AuditLog> logs = auditService.findByEntity("Account", 1L);
        
        assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        assertEquals("Account", log.getEntityType());
        assertEquals(1L, log.getEntityId());
        assertEquals(AuditLog.Action.CREATE, log.getAction());
        assertEquals("admin", log.getUserName());
        assertNull(log.getOldValue());
        assertNotNull(log.getNewValue());
    }

    @Test
    @Order(2)
    @DisplayName("Should log update action")
    void testLogUpdate() {
        auditService.logUpdate("Account", 1L, "{\"name\":\"Old\"}", "{\"name\":\"New\"}", "admin");
        
        List<AuditLog> logs = auditService.findByEntity("Account", 1L);
        
        assertTrue(logs.size() >= 2);
        AuditLog log = logs.get(0); // Most recent
        assertEquals(AuditLog.Action.UPDATE, log.getAction());
        assertNotNull(log.getOldValue());
        assertNotNull(log.getNewValue());
    }

    @Test
    @Order(3)
    @DisplayName("Should log delete action")
    void testLogDelete() {
        auditService.logDelete("Customer", 5L, "{\"name\":\"Deleted\"}", "admin");
        
        List<AuditLog> logs = auditService.findByEntity("Customer", 5L);
        
        assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        assertEquals(AuditLog.Action.DELETE, log.getAction());
        assertNotNull(log.getOldValue());
        assertNull(log.getNewValue());
    }

    @Test
    @Order(4)
    @DisplayName("Should find logs by user")
    void testFindByUser() {
        auditService.logCreate("Invoice", 10L, "{}", "testuser");
        
        List<AuditLog> logs = auditService.findByUser("testuser");
        
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().allMatch(l -> "testuser".equals(l.getUserName())));
    }

    @Test
    @Order(5)
    @DisplayName("Should find logs by time range")
    void testFindByTimeRange() {
        Instant from = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant to = Instant.now().plus(1, ChronoUnit.HOURS);
        
        List<AuditLog> logs = auditService.findByTimeRange(from, to);
        
        assertFalse(logs.isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("Should find logs by entity")
    void testFindByEntity() {
        List<AuditLog> logs = auditService.findByEntity("Account", 1L);
        
        assertFalse(logs.isEmpty());
        assertTrue(logs.stream().allMatch(l -> "Account".equals(l.getEntityType()) && l.getEntityId().equals(1L)));
    }
}
