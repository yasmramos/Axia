package io.github.yasmramos.axia.audit;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.ebean.DB;
import io.github.yasmramos.axia.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Service for managing audit logs.
 * Tracks all changes to entities for compliance and security.
 *
 * @author Yasmany Ramos Garcia
 */
@Component
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    /**
     * Logs a create action.
     *
     * @param entityType type of entity created
     * @param entityId ID of the entity
     * @param newValue JSON representation of new entity
     * @param userName user who performed the action
     */
    public void logCreate(String entityType, Long entityId, String newValue, String userName) {
        log(entityType, entityId, AuditLog.Action.CREATE, null, newValue, userName, null);
    }

    /**
     * Logs an update action.
     *
     * @param entityType type of entity updated
     * @param entityId ID of the entity
     * @param oldValue JSON representation of old entity state
     * @param newValue JSON representation of new entity state
     * @param userName user who performed the action
     */
    public void logUpdate(String entityType, Long entityId, String oldValue, String newValue, String userName) {
        log(entityType, entityId, AuditLog.Action.UPDATE, oldValue, newValue, userName, null);
    }

    /**
     * Logs a delete action.
     *
     * @param entityType type of entity deleted
     * @param entityId ID of the entity
     * @param oldValue JSON representation of deleted entity
     * @param userName user who performed the action
     */
    public void logDelete(String entityType, Long entityId, String oldValue, String userName) {
        log(entityType, entityId, AuditLog.Action.DELETE, oldValue, null, userName, null);
    }

    private void log(String entityType, Long entityId, AuditLog.Action action, 
                     String oldValue, String newValue, String userName, String ipAddress) {
        logger.debug("Logging audit: {} {} on {} id={}", userName, action, entityType, entityId);

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setUserName(userName);
        auditLog.setIpAddress(ipAddress);

        DB.save(auditLog);
    }

    /**
     * Finds all audit logs for a specific entity.
     *
     * @param entityType type of entity
     * @param entityId ID of the entity
     * @return list of audit logs ordered by creation time descending
     */
    public List<AuditLog> findByEntity(String entityType, Long entityId) {
        logger.debug("Finding audit logs for {} id={}", entityType, entityId);
        return DB.find(AuditLog.class)
                .where()
                .eq("entityType", entityType)
                .eq("entityId", entityId)
                .orderBy().desc("createdAt")
                .findList();
    }

    /**
     * Finds all audit logs by user.
     *
     * @param userName the username
     * @return list of audit logs
     */
    public List<AuditLog> findByUser(String userName) {
        logger.debug("Finding audit logs for user: {}", userName);
        return DB.find(AuditLog.class)
                .where()
                .eq("userName", userName)
                .orderBy().desc("createdAt")
                .findList();
    }

    /**
     * Finds all audit logs within a time range.
     *
     * @param from start time
     * @param to end time
     * @return list of audit logs
     */
    public List<AuditLog> findByTimeRange(Instant from, Instant to) {
        logger.debug("Finding audit logs from {} to {}", from, to);
        return DB.find(AuditLog.class)
                .where()
                .ge("createdAt", from)
                .le("createdAt", to)
                .orderBy().desc("createdAt")
                .findList();
    }
}
