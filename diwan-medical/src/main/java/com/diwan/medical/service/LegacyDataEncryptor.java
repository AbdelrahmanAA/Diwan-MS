package com.diwan.medical.service;

import com.diwan.common.crypto.FieldEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Encrypts text stored before the columns were encrypted. Runs at every start and does nothing once all values
 * are encrypted. Plain JDBC on purpose: Hibernate would see the decrypted values as unchanged and write nothing.
 * Each update checks the old value, so it cannot overwrite a change made meanwhile.
 */
@Component
public class LegacyDataEncryptor implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyDataEncryptor.class);

    static final Map<String, String[]> COLUMNS = Map.of(
            "medical_medications", new String[]{"name", "dosage", "frequency", "notes"},
            "medical_allergies", new String[]{"allergen", "reaction", "notes"},
            "medical_blood_type", new String[]{"blood_type", "notes"},
            "medical_surgeries", new String[]{"surgery_name", "hospital", "surgeon", "notes"},
            "medical_chronic_diseases", new String[]{"disease_name", "current_treatment", "notes"},
            "medical_lab_results", new String[]{"test_name", "result", "unit", "reference_range", "notes"},
            "medical_vaccines", new String[]{"vaccine_name", "notes"},
            "medical_family_history", new String[]{"condition_name", "relation", "notes"});

    private final JdbcTemplate jdbc;
    private final FieldEncryptor encryptor;

    public LegacyDataEncryptor(JdbcTemplate jdbc, FieldEncryptor encryptor) {
        this.jdbc = jdbc;
        this.encryptor = encryptor;
    }

    @Override
    public void run(ApplicationArguments args) {
        int total = 0;
        for (var table : COLUMNS.entrySet()) {
            for (String column : table.getValue()) {
                total += encrypt(table.getKey(), column);
            }
        }
        if (total > 0) {
            log.info("[Medical] encrypted {} stored values", total);
        }
    }

    private int encrypt(String table, String column) {
        String select = "select id, " + column + " from " + table + " where " + column + " is not null and " + column
                + " not like 'enc:%'";
        String update = "update " + table + " set " + column + " = ? where id = ? and " + column + " = ?";
        int[] done = {0};
        jdbc.query(select, row -> {
            String plain = row.getString(2);
            done[0] += jdbc.update(update, encryptor.encrypt(plain), row.getLong(1), plain);
        });
        return done[0];
    }
}
