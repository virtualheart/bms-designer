// package service;

// import model.BmsField;
// import java.util.List;

// public class BmsGenerator {

//     private static final int MAX_SOURCE_COL = 71;

//     /**
//      * Generate a CICS BMS map source compatible with IBM High-Level Assembler (HLASM).
//      */
//     public static String generate(String mapName, int rows, int cols, List<BmsField> fields,
//                                   String tioapfx, String ctrl, String line, String column, boolean includePreview) {

//         StringBuilder sb = new StringBuilder();

//         // ===== 1. PREVIEW SECTION =====
//         if (includePreview) {
//             sb.append("/* BMS Map Preview\n");
//             char[][] grid = new char[rows][cols];
//             for (int r = 0; r < rows; r++)
//                 for (int c = 0; c < cols; c++)
//                     grid[r][c] = ' ';

//             for (BmsField f : fields) {
//                 int r = Math.max(0, f.getRow() - 1);
//                 int c = Math.max(0, f.getCol() - 1);
//                 String label = (f.getName() == null || f.getName().isEmpty()) ? "FLD" : f.getName();
                
//                 for (int i = 0; i < f.getLength() && c + i < cols; i++) {
//                     grid[r][c + i] = (i < label.length()) ? label.charAt(i) : '-';
//                 }
//             }

//             for (int r = 0; r < rows; r++) {
//                 boolean empty = true;
//                 for (int c = 0; c < cols; c++) if (grid[r][c] != ' ') empty = false;
//                 if (empty) continue;
//                 sb.append("/* ").append(new String(grid[r])).append("\n");
//             }
//             sb.append("*/\n");
//         }

//         // ===== 2. MAPSET DEFINITION (DFHMSD) =====
//         appendFormatted(sb, mapName.toUpperCase() + " DFHMSD TYPE=&SYSPARM,");
//         appendFormatted(sb, "         MODE=INOUT,");
//         appendFormatted(sb, "         LANG=COBOL,");
//         appendFormatted(sb, "         MAPATTS=(COLOR,HILIGHT),");
//         appendFormatted(sb, "         DSATTS=(COLOR,HILIGHT),");
//         appendFormatted(sb, "         TIOAPFX=" + tioapfx + ",");
//         // Ensure CTRL handles parentheses correctly
//         String ctrlValue = ctrl.startsWith("(") ? ctrl : "(" + ctrl + ")";
//         appendFormatted(sb, "         CTRL=" + ctrlValue + ",");

//         // ===== 3. MAP DEFINITION (DFHMDI) =====
//         appendFormatted(sb, "MAP1     DFHMDI SIZE=(" + rows + "," + cols + "),");
//         appendFormatted(sb, "         LINE=" + line + ",");
//         // Crucial: Trailing comma here connects to the first field
//         appendFormatted(sb, "         COLUMN=" + column + ",");

//         // ===== 4. FIELD DEFINITIONS (DFHMDF) =====
//         for (int i = 0; i < fields.size(); i++) {
//             BmsField f = fields.get(i);
//             String fieldLabel = (f.getName() == null) ? "" : f.getName();
            
//             appendFormatted(sb, String.format(
//                     "%-8s DFHMDF POS=(%02d,%02d),LENGTH=%d,",
//                     fieldLabel, f.getRow(), f.getCol(), f.getLength()
//             ));

//             if (f.getInitialValue() != null && !f.getInitialValue().isEmpty()) {
//                 appendInitial(sb, f.getInitialValue(), f.getLength());
//             }

//             String attrb = f.getAttrb() != null ? f.getAttrb() : "PROT,NORM";
//             appendFormatted(sb, "         ATTRB=(" + attrb + "),");
            
//             // All fields end in comma to continue to the next or the dummy stopper
//             appendFormatted(sb, "         COLOR=" + f.getColor().name() + ",");
//         }

//         // ===== 5. STOPPER FIELD & CLOSING =====
//         appendFormatted(sb, "         DFHMDF POS=(" + rows + "," + (cols) + "),LENGTH=1,ATTRB=ASKIP");
//         appendFormatted(sb, mapName.toUpperCase() + " DFHMSD TYPE=FINAL");
//         sb.append("         END\n");

//         return sb.toString();
//     }

//     private static void appendFormatted(StringBuilder sb, String line) {
//         String formattedLine = line;
//         // If it starts with space, it's an attribute; ensure it starts at Col 10
//         if (line.startsWith(" ")) {
//             formattedLine = String.format("%-9s%s", "", line.trim());
//         }

//         if (formattedLine.trim().endsWith(",")) {
//             // Place 'X' exactly at Column 72
//             sb.append(String.format("%-71sX%n", formattedLine));
//         } else {
//             sb.append(formattedLine).append("\n");
//         }
//     }

//     private static void appendInitial(StringBuilder sb, String initial, int length) {
//         String value = initial.length() > length ? initial.substring(0, length) : initial;
//         String line = "         INITIAL='" + value + "',";

//         if (line.length() <= 71) {
//             sb.append(String.format("%-71sX%n", line));
//         } else {
//             // Break string: Split at Col 71, X at Col 72, Resume at Col 16
//             sb.append(line.substring(0, 71)).append("X\n");
//             String part2 = String.format("%-15s%s", "", line.substring(71));
//             if (part2.trim().endsWith(",")) {
//                 sb.append(String.format("%-71sX%n", part2));
//             } else {
//                 sb.append(part2).append("\n");
//             }
//         }
//     }
// }

// package service;

// import model.BmsField;
// import java.util.List;
// import java.util.Comparator;

// public class BmsGenerator {

//     private static final int MAX_SOURCE_COL = 71;

//     public static String generate(String mapName, int rows, int cols, List<BmsField> fields,
//                                   String tioapfx, String ctrl, String line, String column, boolean includePreview) {

//         StringBuilder sb = new StringBuilder();

//         // 1. PREVIEW SECTION
//         if (includePreview) {
//             sb.append("/* BMS Map Preview\n");
//             char[][] grid = new char[rows][cols];
//             for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) grid[r][c] = ' ';

//             for (BmsField f : fields) {
//                 int r = f.getRow() - 1;
//                 int c = f.getCol() - 1;
//                 String label = f.getName();
//                 for (int i = 0; i < f.getLength() && c + i < cols; i++) {
//                     grid[r][c + i] = (i < label.length()) ? label.charAt(i) : '-';
//                 }
//             }
//             for (int r = 0; r < rows; r++) {
//                 String rowStr = new String(grid[r]);
//                 if (!rowStr.trim().isEmpty()) sb.append("/* ").append(rowStr).append("\n");
//             }
//             sb.append("*/\n");
//         }

//         // 2. MAPSET (DFHMSD)
//         appendFormatted(sb, mapName.toUpperCase() + " DFHMSD TYPE=&SYSPARM,");
//         appendFormatted(sb, "         MODE=INOUT,");
//         appendFormatted(sb, "         LANG=COBOL,");
//         appendFormatted(sb, "         MAPATTS=(COLOR,HILIGHT),");
//         appendFormatted(sb, "         DSATTS=(COLOR,HILIGHT),");
//         appendFormatted(sb, "         TIOAPFX=" + (tioapfx != null ? tioapfx : "YES") + ",");
//         appendFormatted(sb, "         CTRL=(" + (ctrl != null ? ctrl : "FREEKB") + "),");

//         // 3. MAP (DFHMDI)
//         appendFormatted(sb, "MAP1     DFHMDI SIZE=(" + rows + "," + cols + "),");
//         appendFormatted(sb, "         LINE=" + line + ",");
//         appendFormatted(sb, "         COLUMN=" + column + ",");

//         // 4. FIELDS (DFHMDF)
//         for (int i = 0; i < fields.size(); i++) {
//             BmsField f = fields.get(i);
//             appendFormatted(sb, String.format("%-8s DFHMDF POS=(%02d,%02d),LENGTH=%d,",
//                     f.getName(), f.getRow(), f.getCol(), f.getLength()));

//             if (f.getInitialValue() != null && !f.getInitialValue().isEmpty()) {
//                 appendInitial(sb, f.getInitialValue(), f.getLength());
//             }

//             // Using your model's business logic
//             appendFormatted(sb, "         ATTRB=(" + f.buildAttrbString() + "),");
            
//             // Add comma only if not the absolute last line of the macro group
//             appendFormatted(sb, "         COLOR=" + f.getBmsColor() + ",");
//         }

//         // 5. STOPPER & FINAL
//         appendFormatted(sb, "         DFHMDF POS=(" + rows + "," + cols + "),LENGTH=1,ATTRB=ASKIP");
//         appendFormatted(sb, mapName.toUpperCase() + " DFHMSD TYPE=FINAL");
//         sb.append("         END\n");

//         return sb.toString();
//     }

//     private static void appendFormatted(StringBuilder sb, String line) {
//         String formatted = line.startsWith(" ") ? String.format("%-9s%s", "", line.trim()) : line;
//         if (formatted.trim().endsWith(",")) {
//             sb.append(String.format("%-71sX%n", formatted));
//         } else {
//             sb.append(formatted).append("\n");
//         }
//     }

//     private static void appendInitial(StringBuilder sb, String initial, int length) {
//         String val = initial.length() > length ? initial.substring(0, length) : initial;
//         String line = "         INITIAL='" + val + "',";
//         if (line.length() <= 71) {
//             sb.append(String.format("%-71sX%n", line));
//         } else {
//             sb.append(line.substring(0, 71)).append("X\n");
//             sb.append(String.format("%-71sX%n", String.format("%-15s%s", "", line.substring(71))));
//         }
//     }

//     public static String generateCopybook(String mapName, List<BmsField> fields) {
//         StringBuilder sb = new StringBuilder();
//         String uMap = mapName.toUpperCase();

//         // Sort fields by Row then Col so the Copybook matches the screen layout
//         fields.sort(Comparator.comparingInt(BmsField::getRow).thenComparingInt(BmsField::getCol));

//         // Input Map Definition
//         sb.append("       01  ").append(uMap).append("I.\n");
//         sb.append("           02  FILLER          PIC X(12).\n"); // TIOA Prefix

//         for (BmsField f : fields) {
//             String name = f.getName().toUpperCase();
//             if (name.isEmpty() || name.startsWith("FIELD")) continue; // Skip labels

//             sb.append("           02  ").append(name).append("L  PIC S9(4)  COMP.\n");
//             sb.append("           02  ").append(name).append("F  PIC X.\n");
//             sb.append("           02  ").append(name).append("I  PIC X(").append(f.getLength()).append(").\n");
//         }

//         // Output Map Definition (Redefines Input for memory efficiency)
//         sb.append("       01  ").append(uMap).append("O REDEFINES ").append(uMap).append("I.\n");
//         sb.append("           02  FILLER          PIC X(12).\n");
//         for (BmsField f : fields) {
//             String name = f.getName().toUpperCase();
//             if (name.isEmpty() || name.startsWith("FIELD")) continue;

//             sb.append("           02  FILLER          PIC X(3).\n"); // Skips L and F/A
//             sb.append("           02  ").append(name).append("O  PIC X(").append(f.getLength()).append(").\n");
//         }
//         return sb.toString();
//     }


// }

package com.openbms.service;

import com.openbms.model.BmsField;

import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.control.*;


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

        fields.sort(Comparator
                .comparingInt(BmsField::getRow)
                .thenComparingInt(BmsField::getCol));

        for (BmsField f : fields) {

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

        fields.sort(Comparator
                .comparingInt(BmsField::getRow)
                .thenComparingInt(BmsField::getCol));

        // INPUT MAP
        sb.append("       01  ").append(uMap).append("I.\n");

        if (tioapfx) {
            sb.append("           02  FILLER          PIC X(12).\n");
        }

        for (BmsField f : fields) {

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

        for (BmsField f : fields) {

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
        if (!name.matches("^[A-Za-z][A-Za-z0-9]{0,7}$")) {
            new Alert(Alert.AlertType.ERROR, "Invalid BMS Map Name").showAndWait();
            throw new IllegalArgumentException("Invalid BMS Map Name");
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