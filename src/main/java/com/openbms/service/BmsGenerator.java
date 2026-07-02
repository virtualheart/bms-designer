package com.openbms.service;

import com.openbms.model.BmsField;

import java.util.*;


public class BmsGenerator {
    
    private static final int MAX_SOURCE_COL = 71;

    // PUBLIC API
    public static String generate(
            String mapName,
            int rows,
            int cols,
            List<BmsField> fields,
            String tioapfx,
            String ctrl,
            String line,
            String column,
            boolean includePreview) {

        validateMapName(mapName);

        StringBuilder sb = new StringBuilder();
        String uMap = mapName.toUpperCase();

        if (includePreview) {
            appendPreview(sb, rows, cols, fields);
        }

        appendMapSet(sb, uMap, tioapfx, ctrl);
        appendMap(sb, rows, cols, line, column);
        appendFields(sb, fields);
        appendStopperField(sb, rows, cols);
        appendFinal(sb, uMap);

        return sb.toString();
    }

    // MAPSET
    private static void appendMapSet(StringBuilder sb,
                                     String mapName,
                                     String tioapfx,
                                     String ctrl) {

        List<String> params = new ArrayList<>();
        params.add("TYPE=&SYSPARM");
        params.add("MODE=INOUT");
        params.add("LANG=COBOL");
        params.add("MAPATTS=(COLOR,HILIGHT)");
        params.add("DSATTS=(COLOR,HILIGHT)");
        params.add("TIOAPFX=" + (tioapfx == null ? "YES" : tioapfx));
        params.add("CTRL=(" + (ctrl == null ? "FREEKB,FRSET" : ctrl) + ")");

        appendMacro(sb, mapName, "DFHMSD", params);
    }

    // MAP
    private static void appendMap(StringBuilder sb,
                                  int rows,
                                  int cols,
                                  String line,
                                  String column) {

        List<String> params = new ArrayList<>();
        params.add("SIZE=(" + rows + "," + cols + ")");
        params.add("LINE=" + (line == null ? "1" : line));
        params.add("COLUMN=" + (column == null ? "1" : column));

        appendMacro(sb, "MAP1", "DFHMDI", params);
    }

    // FIELDS
    private static void appendFields(StringBuilder sb,
                                     List<BmsField> fields) {

        List<BmsField> sorted = new ArrayList<>(fields);
        sorted.sort(Comparator
                .comparingInt(BmsField::getRow)
                .thenComparingInt(BmsField::getCol));

        for (BmsField f : sorted) {

            validateField(f);

            List<String> params = new ArrayList<>();
            params.add(String.format("POS=(%02d,%02d)", f.getRow(), f.getCol()));
            params.add("LENGTH=" + f.getLength());

            if (f.getInitialValue() != null && !f.getInitialValue().isEmpty()) {
                params.add("INITIAL='" + truncate(f.getInitialValue(), f.getLength()) + "'");
            }

            String attrb = f.buildAttrbString();
            if (attrb != null && !attrb.isBlank()) {
                params.add("ATTRB=(" + attrb + ")");
            }

            if (f.buildColor() != null) {
                params.add("COLOR=" + f.buildColor());
            }

            if (f.buildHighlight() != null) {
                params.add("HILIGHT=" + f.buildHighlight());
            }

            appendMacro(sb, safeName(f.getName()), "DFHMDF", params);
        }
    }

    // STOPPER FIELD
    private static void appendStopperField(StringBuilder sb,
                                           int rows,
                                           int cols) {

        List<String> params = Arrays.asList(
                String.format("POS=(%02d,%02d)", rows, cols),
                "LENGTH=1",
                "ATTRB=(ASKIP)"
        );

        appendMacro(sb, "ZZZSTOP", "DFHMDF", params);
    }

    // FINAL
    private static void appendFinal(StringBuilder sb, String mapName) {
        sb.append(mapName).append(" DFHMSD TYPE=FINAL\n");
        sb.append("         END\n");
    }

    // COPYBOOK GENERATOR
    public static String generateCopybook(String mapName,
                                          List<BmsField> fields,
                                          boolean tioapfx) {

        if (fields == null) {
            throw new IllegalArgumentException("Fields list cannot be null");
        }

        StringBuilder sb = new StringBuilder();
        String uMap = mapName.toUpperCase();

        List<BmsField> sortedFields = new ArrayList<>(fields);
        sortedFields.sort(Comparator
                .comparingInt(BmsField::getRow)
                .thenComparingInt(BmsField::getCol));

        // INPUT MAP
        sb.append("       01  ").append(uMap).append("I.\n");

        if (tioapfx) {
            sb.append("           02  FILLER          PIC X(12).\n");
        }

        for (BmsField f : sortedFields) {

            String name = safeName(f.getName());

            sb.append("           02  ").append(name).append("L  PIC S9(4) COMP.\n");
            sb.append("           02  ").append(name).append("F  PIC X.\n");
            sb.append("           02  ").append(name).append("I  PIC X(")
              .append(f.getLength()).append(").\n");
        }

        // OUTPUT MAP
        sb.append("       01  ").append(uMap)
          .append("O REDEFINES ").append(uMap).append("I.\n");

        if (tioapfx) {
            sb.append("           02  FILLER          PIC X(12).\n");
        }

        for (BmsField f : sortedFields) {

            sb.append("           02  FILLER          PIC X(3).\n");
            sb.append("           02  ").append(safeName(f.getName()))
              .append("O  PIC X(").append(f.getLength()).append(").\n");
        }

        return sb.toString();
    }

    // MACRO WRITER (Assembler Safe)
    private static void appendMacro(StringBuilder sb,
                                    String label,
                                    String macro,
                                    List<String> params) {

        String firstLine = String.format("%-8s %s %s,",
                label,
                macro,
                params.get(0));

        writeLine(sb, firstLine);

        for (int i = 1; i < params.size(); i++) {

            boolean last = (i == params.size() - 1);
            String line = String.format("%-9s%s%s",
                    "",
                    params.get(i),
                    last ? "" : ",");

            writeLine(sb, line);
        }
    }

    // FORMATTER
    private static void writeLine(StringBuilder sb, String line) {

        if (line.endsWith(",")) {
            sb.append(String.format("%-71sX%n", line));
        } else {
            sb.append(line).append("\n");
        }
    }

    // VALIDATION
    private static void validateMapName(String name) {
        if (name == null || !name.matches("^[A-Za-z][A-Za-z0-9]{0,7}$")) {
            throw new IllegalArgumentException(
                    "Invalid BMS Map Name: must start with a letter and be 1-8 alphanumeric characters."
            );
        }
    }

    private static void validateField(BmsField f) {
        if (f.getRow() < 1 || f.getCol() < 1)
            throw new IllegalArgumentException("Invalid field position");

        if (f.getLength() < 1)
            throw new IllegalArgumentException("Invalid field length");
    }

    private static String safeName(String name) {
        name = name.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (name.length() > 8) {
            name = name.substring(0, 8);
        }
        return name;
    }

    private static String truncate(String value, int length) {
        return value.length() > length
                ? value.substring(0, length)
                : value;
    }

    // PREVIEW
    private static void appendPreview(StringBuilder sb,
                                      int rows,
                                      int cols,
                                      List<BmsField> fields) {

        sb.append("/* BMS MAP PREVIEW\n");

        char[][] grid = new char[rows][cols];
        for (int r = 0; r < rows; r++)
            Arrays.fill(grid[r], ' ');

        for (BmsField f : fields) {

            int r = f.getRow() - 1;
            int c = f.getCol() - 1;

            String label = safeName(f.getName());

            for (int i = 0; i < f.getLength() && c + i < cols; i++) {
                grid[r][c + i] =
                        i < label.length() ? label.charAt(i) : '-';
            }
        }

        for (int r = 0; r < rows; r++) {
            sb.append("/* ").append(new String(grid[r])).append("\n");
        }

        sb.append("*/\n");
    }
}
