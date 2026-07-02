package com.openbms.service;

import com.openbms.model.BmsField;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a previously generated BMS map source (DFHMSD/DFHMDI/DFHMDF macros)
 * back into an in-memory grid size and list of BmsField objects.
 *
 * This is the inverse of {@link BmsGenerator}. It is intentionally tolerant:
 * continuation lines (ending in "X" in column 72, IBM HLASM style) are
 * stitched back together before parsing, and unrecognized macros/params are
 * simply skipped rather than causing a hard failure.
 */
public class BmsParser {

    private static final Pattern SIZE_PATTERN =
            Pattern.compile("SIZE=\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");

    private static final Pattern POS_PATTERN =
            Pattern.compile("POS=\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");

    private static final Pattern LENGTH_PATTERN =
            Pattern.compile("LENGTH=(\\d+)");

    private static final Pattern INITIAL_PATTERN =
            Pattern.compile("INITIAL='([^']*)'");

    private static final Pattern ATTRB_PATTERN =
            Pattern.compile("ATTRB=\\(([^)]*)\\)");

    private static final Pattern COLOR_PATTERN =
            Pattern.compile("COLOR=([A-Z]+)");

    private static final Pattern HILIGHT_PATTERN =
            Pattern.compile("HILIGHT=([A-Z]+)");

    /** Result of a parse: grid dimensions plus the recovered fields. */
    public static class ParseResult {
        public final int rows;
        public final int cols;
        public final List<BmsField> fields;

        public ParseResult(int rows, int cols, List<BmsField> fields) {
            this.rows = rows;
            this.cols = cols;
            this.fields = fields;
        }
    }

    public ParseResult parse(String bmsSource) {

        if (bmsSource == null || bmsSource.isBlank()) {
            throw new IllegalArgumentException("BMS source is empty.");
        }

        List<String> macroBlocks = joinContinuationsAndSplitMacros(bmsSource);

        int rows = 24;
        int cols = 80;
        List<BmsField> fields = new ArrayList<>();

        for (String block : macroBlocks) {

            String upper = block.toUpperCase();

            if (upper.contains("DFHMDI")) {
                Matcher m = SIZE_PATTERN.matcher(block);
                if (m.find()) {
                    rows = Integer.parseInt(m.group(1));
                    cols = Integer.parseInt(m.group(2));
                }
                continue;
            }

            if (upper.contains("DFHMDF")) {
                BmsField field = parseFieldMacro(block);
                // Skip the stopper field (1-char ASKIP placed at the bottom-right corner)
                if (field != null && !isStopperField(block)) {
                    fields.add(field);
                }
                continue;
            }

            // DFHMSD and other macros (mapset header/trailer) carry no field data.
        }

        if (fields.isEmpty() && macroBlocks.stream().noneMatch(b -> b.toUpperCase().contains("DFHMDF"))) {
            throw new IllegalArgumentException("No DFHMDF field definitions found in file.");
        }

        return new ParseResult(rows, cols, fields);
    }

    // ===== Macro reconstruction =====

    /**
     * Strips the BMS preview comment block, removes the trailing continuation
     * marker ("X" sitting in column 72) joining wrapped lines back together,
     * then splits the resulting text into one string per macro statement
     * (a label/blank + macro name + its comma-separated parameter list).
     */
    private List<String> joinContinuationsAndSplitMacros(String source) {

        StringBuilder joined = new StringBuilder();
        boolean inPreview = false;

        for (String rawLine : source.split("\\r?\\n")) {

            String line = rawLine;

            if (line.startsWith("/*")) {
                inPreview = !line.trim().equals("*/");
                continue;
            }
            if (inPreview) {
                if (line.trim().equals("*/")) inPreview = false;
                continue;
            }
            if (line.trim().equals("*/")) continue;

            // Continuation marker: line padded to col 71 with 'X' at col 72.
            if (line.length() >= 72 && line.charAt(71) == 'X'
                    && line.substring(0, 71).trim().endsWith(",")) {
                joined.append(line.substring(0, 71).stripTrailing()).append(' ');
            } else if (line.length() >= 1 && line.trim().equals("X")) {
                // stray continuation marker line, ignore
            } else {
                joined.append(line.stripTrailing()).append('\n');
            }
        }

        // Now split into macro statements. A new macro/label starts at the
        // beginning of a line (column 1) with a non-space character, or is a
        // continuation line (starts with spaces) belonging to the previous
        // macro until the previous statement's params are terminated.
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : joined.toString().split("\n")) {
            if (line.isBlank()) continue;

            boolean startsNewLabel = !line.startsWith(" ");

            if (startsNewLabel && current.length() > 0) {
                statements.add(current.toString());
                current.setLength(0);
            }
            current.append(line).append(' ');
        }
        if (current.length() > 0) statements.add(current.toString());

        return statements;
    }

    // ===== Field parsing =====

    private boolean isStopperField(String block) {
        return block.toUpperCase().contains("ZZZSTOP");
    }

    private BmsField parseFieldMacro(String block) {

        Matcher posMatcher = POS_PATTERN.matcher(block);
        if (!posMatcher.find()) {
            return null; // malformed field, skip
        }

        int row = Integer.parseInt(posMatcher.group(1));
        int col = Integer.parseInt(posMatcher.group(2));

        int length = 1;
        Matcher lenMatcher = LENGTH_PATTERN.matcher(block);
        if (lenMatcher.find()) {
            length = Integer.parseInt(lenMatcher.group(1));
        }

        String name = extractLabel(block);

        BmsField field = new BmsField();
        field.setName(name == null || name.isBlank() ? "FIELD" : name);
        field.setRow(row);
        field.setCol(col);
        field.setLength(length);

        Matcher initialMatcher = INITIAL_PATTERN.matcher(block);
        if (initialMatcher.find()) {
            field.setInitialValue(initialMatcher.group(1));
        }

        BmsField.Protection protection = BmsField.Protection.UNPROT;
        BmsField.Intensity intensity = BmsField.Intensity.NORM;

        Matcher attrbMatcher = ATTRB_PATTERN.matcher(block);
        if (attrbMatcher.find()) {
            for (String token : attrbMatcher.group(1).split(",")) {
                String t = token.trim();
                try {
                    protection = BmsField.Protection.valueOf(t);
                } catch (IllegalArgumentException ignoredProtection) {
                    try {
                        intensity = BmsField.Intensity.valueOf(t);
                    } catch (IllegalArgumentException ignoredIntensity) {
                        // ASKIP / NUM combos or unrecognized tokens are skipped;
                        // protection/intensity enums cover the common cases.
                    }
                }
            }
        }
        field.setProtection(protection);
        field.setIntensity(intensity);

        // Infer the field type from the resolved protection so behaviors
        // (e.g. autoResolveProtection on later edits) stay consistent.
        if (protection == BmsField.Protection.PROT) {
            field.setFieldType(BmsField.FieldType.OUTPUT);
            field.setProtection(protection);
        } else {
            field.setFieldType(BmsField.FieldType.INPUT);
            field.setProtection(protection);
        }

        Matcher colorMatcher = COLOR_PATTERN.matcher(block);
        if (colorMatcher.find()) {
            try {
                field.setColor(BmsField.BmsColor.valueOf(colorMatcher.group(1)));
            } catch (IllegalArgumentException ignored) {
                field.setColor(BmsField.BmsColor.NEUTRAL);
            }
        }

        Matcher hilightMatcher = HILIGHT_PATTERN.matcher(block);
        if (hilightMatcher.find()) {
            try {
                field.setHighlight(BmsField.Highlight.valueOf(hilightMatcher.group(1)));
            } catch (IllegalArgumentException ignored) {
                field.setHighlight(BmsField.Highlight.NONE);
            }
        }

        return field;
    }

    /** Pulls the label token (field name) preceding the DFHMDF macro keyword. */
    private String extractLabel(String block) {
        String trimmed = block.trim();
        int macroIdx = trimmed.toUpperCase().indexOf("DFHMDF");
        if (macroIdx <= 0) return null;

        String label = trimmed.substring(0, macroIdx).trim();
        return label.isEmpty() ? null : label;
    }
}
