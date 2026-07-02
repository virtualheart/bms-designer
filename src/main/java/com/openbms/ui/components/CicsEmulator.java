package com.openbms.ui.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.openbms.model.AppSettings;
import com.openbms.model.BmsField;

/**
 * Renders an interactive, read-only-where-protected 3270-style emulation of
 * a BMS map: black screen, monospaced text, field colors taken from each
 * BmsField's COLOR/highlight, a blinking cursor on the active entry field,
 * and Tab/Shift+Tab cycling between unprotected fields only (protected/ASKIP
 * fields are display-only, exactly like a real CICS terminal).
 *
 * This is a visual/interaction demo, not a CICS runtime: there is no actual
 * transaction processing. "Enter" simply validates typed values against
 * each field's declared length and reports back via onSubmit.
 */
public class CicsEmulator {

    private final Canvas canvas;
    private final int cellWidth;
    private final int cellHeight;

    private List<BmsField> sourceFields = new ArrayList<>();
    private final Map<BmsField, StringBuilder> entryValues = new LinkedHashMap<>();
    private List<BmsField> entryOrder = new ArrayList<>();

    private int rows;
    private int cols;
    private BmsField activeField;
    private int cursorPos;

    private boolean cursorBlinkOn = true;
    private AppSettings.EmulatorPalette palette = AppSettings.EmulatorPalette.GREEN_ON_BLACK;

    private Runnable onSubmit = () -> {};

    public CicsEmulator(Canvas canvas, int cellWidth, int cellHeight) {
        this.canvas = canvas;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;

        canvas.setFocusTraversable(true);
        canvas.setOnMousePressed(this::handleMouseClick);
        canvas.setOnKeyPressed(this::handleKeyPress);
        canvas.setOnKeyTyped(this::handleKeyTyped);
    }

    /** Sets the color scheme used for the next draw() call. Defaults to classic green-on-black. */
    public void setPalette(AppSettings.EmulatorPalette palette) {
        this.palette = palette != null ? palette : AppSettings.EmulatorPalette.GREEN_ON_BLACK;
    }

    public void setOnSubmit(Runnable onSubmit) {
        this.onSubmit = onSubmit != null ? onSubmit : () -> {};
    }

    /**
     * Loads a map definition into the emulator. Field objects are not
     * mutated; their initial values seed the screen exactly as DFHMDF's
     * INITIAL= would, and only unprotected fields become editable entry
     * points.
     */
    public void load(List<BmsField> fields, int rows, int cols) {
        this.sourceFields = new ArrayList<>(fields);
        this.rows = rows;
        this.cols = cols;

        canvas.setWidth(cols * cellWidth);
        canvas.setHeight(rows * cellHeight);

        entryValues.clear();
        entryOrder = new ArrayList<>();

        for (BmsField f : sourceFields) {
            if (f.isEntryField()) {
                entryOrder.add(f);
            }
        }
        entryOrder.sort((a, b) -> {
            int rowCompare = Integer.compare(a.getRow(), b.getRow());
            return rowCompare != 0 ? rowCompare : Integer.compare(a.getCol(), b.getCol());
        });

        for (BmsField f : entryOrder) {
            entryValues.put(f, new StringBuilder(blankPad(f.getInitialValue(), f.getLength())));
        }

        activeField = entryOrder.isEmpty() ? null : entryOrder.get(0);
        cursorPos = 0;
        draw();
    }

    /** Resets all entry fields back to their original INITIAL= values. */
    public void reset() {
        for (BmsField f : entryOrder) {
            entryValues.put(f, new StringBuilder(blankPad(f.getInitialValue(), f.getLength())));
        }
        activeField = entryOrder.isEmpty() ? null : entryOrder.get(0);
        cursorPos = 0;
        draw();
    }

    /** Toggles the cursor blink phase; call from a Timeline to animate. */
    public void tickCursorBlink() {
        cursorBlinkOn = !cursorBlinkOn;
        draw();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    /** Returns a snapshot of what the operator typed, field name -> value (trimmed). */
    public Map<String, String> collectValues() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<BmsField, StringBuilder> entry : entryValues.entrySet()) {
            result.put(entry.getKey().getName(), entry.getValue().toString().stripTrailing());
        }
        return result;
    }

    private String blankPad(String value, int length) {
        String v = value == null ? "" : value;
        if (v.length() > length) v = v.substring(0, length);
        StringBuilder sb = new StringBuilder(v);
        while (sb.length() < length) sb.append(' ');
        return sb.toString();
    }

    // ===== Input handling =====

    private void handleMouseClick(MouseEvent e) {
        canvas.requestFocus();
        for (BmsField f : entryOrder) {
            int x = (f.getCol() - 1) * cellWidth;
            int y = (f.getRow() - 1) * cellHeight;
            int w = f.getLength() * cellWidth;

            if (e.getX() >= x && e.getX() <= x + w
                    && e.getY() >= y && e.getY() <= y + cellHeight) {

                activeField = f;
                int clickedChar = (int) ((e.getX() - x) / cellWidth);
                cursorPos = Math.max(0, Math.min(f.getLength() - 1, clickedChar));
                draw();
                return;
            }
        }
    }

    private void handleKeyPress(KeyEvent e) {

        if (e.getCode() == KeyCode.TAB) {
            cycleField(!e.isShiftDown());
            e.consume();
            draw();
            return;
        }

        if (activeField == null) return;

        StringBuilder value = entryValues.get(activeField);
        if (value == null) return;

        switch (e.getCode()) {

            case ENTER:
                onSubmit.run();
                e.consume();
                return;

            case BACK_SPACE:
                if (cursorPos > 0) {
                    cursorPos--;
                    value.setCharAt(cursorPos, ' ');
                }
                e.consume();
                break;

            case DELETE:
                if (cursorPos < value.length()) {
                    value.setCharAt(cursorPos, ' ');
                }
                e.consume();
                break;

            case LEFT:
                cursorPos = Math.max(0, cursorPos - 1);
                e.consume();
                break;

            case RIGHT:
                cursorPos = Math.min(activeField.getLength() - 1, cursorPos + 1);
                e.consume();
                break;

            case HOME:
                cursorPos = 0;
                e.consume();
                break;

            case END:
                cursorPos = activeField.getLength() - 1;
                e.consume();
                break;

            default:
                // Printable character entry is handled in handleKeyTyped,
                // not here: KeyEvent.getText() on KEY_PRESSED is unreliable
                // for printable characters in JavaFX (it's designed for
                // KEY_TYPED). Routing character input through KEY_PRESSED
                // silently dropped every typed character.
                return;
        }

        draw();
    }

    private void handleKeyTyped(KeyEvent e) {

        if (activeField == null) return;

        StringBuilder value = entryValues.get(activeField);
        if (value == null) return;

        String typed = e.getCharacter();
        if (typed == null || typed.length() != 1) return;

        char ch = typed.charAt(0);
        if (!isPrintable(ch)) return;

        if (activeField.getProtection() == BmsField.Protection.NUM && !Character.isDigit(ch)) {
            e.consume();
            return;
        }

        if (cursorPos < value.length()) {
            value.setCharAt(cursorPos, ch);
            cursorPos = Math.min(activeField.getLength() - 1, cursorPos + 1);
        }

        e.consume();
        draw();
    }

    private boolean isPrintable(char c) {
        return c >= 32 && c != 127;
    }

    private void cycleField(boolean forward) {
        if (entryOrder.isEmpty()) {
            activeField = null;
            return;
        }
        if (activeField == null) {
            activeField = forward ? entryOrder.get(0) : entryOrder.get(entryOrder.size() - 1);
            cursorPos = 0;
            return;
        }
        int idx = entryOrder.indexOf(activeField);
        int nextIdx = forward
                ? (idx + 1) % entryOrder.size()
                : (idx - 1 + entryOrder.size()) % entryOrder.size();
        activeField = entryOrder.get(nextIdx);
        cursorPos = 0;
    }

    // ===== Rendering =====

    public void draw() {

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(backgroundColor());
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 14));

        for (BmsField f : sourceFields) {

            int x = (f.getCol() - 1) * cellWidth;
            int y = (f.getRow() - 1) * cellHeight;

            String text = f.isEntryField()
                    ? entryValues.getOrDefault(f, new StringBuilder(blankPad(f.getInitialValue(), f.getLength()))).toString()
                    : blankPad(f.getInitialValue(), f.getLength());

            gc.setFill(toFxColor(f.getColor()));

            for (int i = 0; i < text.length(); i++) {
                double cx = x + i * cellWidth;
                gc.fillText(String.valueOf(text.charAt(i)), cx + 1, y + cellHeight - 5);
            }

            if (f.getHighlight() == BmsField.Highlight.UNDERLINE) {
                gc.setStroke(toFxColor(f.getColor()));
                gc.strokeLine(x, y + cellHeight - 2, x + f.getLength() * cellWidth, y + cellHeight - 2);
            }

            if (f == activeField) {
                gc.setStroke(activeOutlineColor());
                gc.strokeRect(x, y, f.getLength() * cellWidth, cellHeight);
            }
        }

        // Blinking block cursor on the active field's current position
        if (activeField != null && cursorBlinkOn) {
            int x = (activeField.getCol() - 1) * cellWidth + cursorPos * cellWidth;
            int y = (activeField.getRow() - 1) * cellHeight;
            gc.setFill(cursorColor());
            gc.fillRect(x, y, cellWidth, cellHeight);
        }
    }

    private Color backgroundColor() {
        return palette == AppSettings.EmulatorPalette.DARK_ON_LIGHT ? Color.web("#F5F5F0") : Color.BLACK;
    }

    private Color activeOutlineColor() {
        return palette == AppSettings.EmulatorPalette.DARK_ON_LIGHT ? Color.web("#AAAAAA") : Color.web("#444444");
    }

    private Color cursorColor() {
        return switch (palette) {
            case DARK_ON_LIGHT   -> Color.rgb(0, 0, 0, 0.25);
            case AMBER_ON_BLACK  -> Color.rgb(255, 176, 0, 0.45);
            case WHITE_ON_BLACK  -> Color.rgb(255, 255, 255, 0.45);
            case GREEN_ON_BLACK  -> Color.rgb(0, 255, 0, 0.45);
        };
    }

    private Color toFxColor(BmsField.BmsColor c) {

        // DARK_ON_LIGHT inverts the usual bright-on-black 3270 scheme: render
        // everything as a single dark, readable foreground rather than
        // fighting a light background with neon colors designed for black.
        if (palette == AppSettings.EmulatorPalette.DARK_ON_LIGHT) {
            return Color.web("#1A1A1A");
        }

        // AMBER_ON_BLACK and WHITE_ON_BLACK are monochrome terminal styles:
        // every field renders in the one palette color regardless of its
        // configured BmsColor, matching how real amber/white 3270 terminals
        // had no per-field color capability.
        if (palette == AppSettings.EmulatorPalette.AMBER_ON_BLACK) {
            return Color.web("#FFB000");
        }
        if (palette == AppSettings.EmulatorPalette.WHITE_ON_BLACK) {
            return Color.web("#E8E8E8");
        }

        if (c == null) return Color.web("#33FF33");
        return switch (c) {
            case RED       -> Color.web("#FF5555");
            case GREEN     -> Color.web("#33FF33");
            case BLUE      -> Color.web("#5599FF");
            case YELLOW    -> Color.YELLOW;
            case TURQUOISE -> Color.CYAN;
            case PINK      -> Color.HOTPINK;
            case NEUTRAL   -> Color.WHITE;
        };
    }
}
