package com.diwan.medical.service;

import com.diwan.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Medicines registered in Saudi Arabia, from SFDA's public drugs list (built by scripts/sfda-medicines.py and
 * shipped with the service). Searched in memory: a few thousand rows. It helps typing names; a medicine that is
 * not on the list can still be entered by hand.
 */
@Service
public class MedicineCatalog {

    private static final Logger log = LoggerFactory.getLogger(MedicineCatalog.class);
    static final int MAX_RESULTS = 20;
    public static final String SOURCE = "SFDA registered drugs list";

    public record Medicine(String trade, String scientific, String strength, String form) {
        private boolean matches(String q) {
            return trade.toUpperCase(Locale.ROOT).contains(q) || scientific.toUpperCase(Locale.ROOT).contains(q);
        }

        /** Trade names starting with the query first, then scientific names starting with it, then the rest. */
        private int rank(String q) {
            return trade.toUpperCase(Locale.ROOT).startsWith(q) ? 0 : scientific.toUpperCase(Locale.ROOT).startsWith(q) ? 1 : 2;
        }
    }

    private final List<Medicine> medicines;

    public MedicineCatalog(@Value("${diwan.medical.medicines:classpath:medicines/sfda-drugs.csv}") Resource file) {
        this.medicines = load(file);
        log.info("[Medical] medicine catalogue: {} medicines", medicines.size());
    }

    static List<Medicine> load(Resource file) {
        if (!file.exists()) {
            return List.of();
        }
        List<Medicine> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> header = parse(r.readLine());
            int trade = header.indexOf("trade"), scientific = header.indexOf("scientific"),
                    strength = header.indexOf("strength"), form = header.indexOf("form");
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                List<String> c = parse(line);
                if (c.size() == header.size() && !c.get(trade).isBlank()) {
                    out.add(new Medicine(c.get(trade), c.get(scientific), c.get(strength), c.get(form)));
                }
            }
        } catch (IOException | RuntimeException e) {
            log.warn("[Medical] medicine catalogue not loaded: {}", e.getMessage());
            return List.of();
        }
        return List.copyOf(out);
    }

    /** One CSV line with optional double quotes ("" inside quotes is a quote). */
    static List<String> parse(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; line != null && i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(ch);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    public List<Medicine> search(String query) {
        String q = query == null ? "" : query.trim().toUpperCase(Locale.ROOT);
        if (q.length() < 2 || q.length() > 60) {
            throw ApiException.badRequest("medical.medicine.query");
        }
        return medicines.stream().filter(m -> m.matches(q))
                .sorted(Comparator.comparingInt((Medicine m) -> m.rank(q)).thenComparing(Medicine::trade))
                .limit(MAX_RESULTS).toList();
    }

    public int size() {
        return medicines.size();
    }
}
