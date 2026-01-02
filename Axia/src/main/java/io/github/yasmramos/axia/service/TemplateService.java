package io.github.yasmramos.axia.service;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.ebean.DB;
import io.github.yasmramos.axia.model.JournalEntry;
import io.github.yasmramos.axia.model.JournalEntryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing journal entry templates.
 * Enables quick creation of recurring entries.
 *
 * @author Yasmany Ramos Garcia
 */
@Component
public class TemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);

    private final JournalEntryService journalEntryService;

    @Inject
    public TemplateService(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    /**
     * Saves or updates a template.
     *
     * @param template the template to save
     */
    public void save(JournalEntryTemplate template) {
        logger.info("Saving template: {}", template.getName());
        DB.save(template);
    }

    /**
     * Finds a template by ID.
     *
     * @param id the template ID
     * @return optional containing the template if found
     */
    public Optional<JournalEntryTemplate> findById(Long id) {
        logger.debug("Finding template by id: {}", id);
        return Optional.ofNullable(DB.find(JournalEntryTemplate.class, id));
    }

    /**
     * Gets all active templates.
     *
     * @return list of active templates
     */
    public List<JournalEntryTemplate> findAllActive() {
        logger.debug("Finding all active templates");
        return DB.find(JournalEntryTemplate.class)
                .where()
                .eq("active", true)
                .orderBy().asc("name")
                .findList();
    }

    /**
     * Searches templates by name.
     *
     * @param name the name to search
     * @return list of matching templates
     */
    public List<JournalEntryTemplate> searchByName(String name) {
        logger.debug("Searching templates by name: {}", name);
        return DB.find(JournalEntryTemplate.class)
                .where()
                .ilike("name", "%" + name + "%")
                .eq("active", true)
                .findList();
    }

    /**
     * Creates a journal entry from a template.
     *
     * @param templateId the template ID
     * @param entryDate the date for the entry
     * @param amount the amount (overrides default if provided)
     * @param description optional description override
     * @return created journal entry
     */
    public JournalEntry createFromTemplate(Long templateId, LocalDate entryDate, 
                                            BigDecimal amount, String description) {
        logger.info("Creating entry from template {} for date {}", templateId, entryDate);

        Optional<JournalEntryTemplate> template = findById(templateId);
        if (template.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }

        JournalEntryTemplate t = template.get();
        BigDecimal entryAmount = amount != null ? amount : t.getDefaultAmount();
        String entryDescription = description != null ? description : t.getDescription();
        String reference = "TPL-" + templateId;

        // Create journal entry with lines
        JournalEntry entry = journalEntryService.create(entryDate, entryDescription, reference);

        // Add debit and credit lines
        if (t.getDebitAccount() != null) {
            journalEntryService.addLine(entry, t.getDebitAccount(), entryAmount, BigDecimal.ZERO, null);
        }
        if (t.getCreditAccount() != null) {
            journalEntryService.addLine(entry, t.getCreditAccount(), BigDecimal.ZERO, entryAmount, null);
        }

        logger.info("Created entry {} from template", entry.getId());
        return entry;
    }

    /**
     * Deletes a template by ID.
     *
     * @param id the template ID
     * @return true if deleted
     */
    public boolean delete(Long id) {
        logger.info("Deleting template: {}", id);
        return DB.delete(JournalEntryTemplate.class, id) > 0;
    }

    /**
     * Deactivates a template.
     *
     * @param id the template ID
     * @return true if deactivated
     */
    public boolean deactivate(Long id) {
        logger.info("Deactivating template: {}", id);
        Optional<JournalEntryTemplate> template = findById(id);
        if (template.isPresent()) {
            template.get().setActive(false);
            DB.save(template.get());
            return true;
        }
        return false;
    }
}
