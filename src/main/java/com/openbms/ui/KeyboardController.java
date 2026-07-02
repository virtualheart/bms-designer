package com.openbms.ui;

import javafx.scene.input.KeyEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import com.openbms.model.BmsField;
import com.openbms.service.FieldService;

/**
 * Pure keyboard-navigation/editing logic for the design canvas: arrow-key
 * movement, delete, copy/paste, undo/redo, and Tab/Shift+Tab selection
 * cycling. Has no JavaFX rendering or canvas dependency — it mutates the
 * field list and selection, then notifies the caller via onChange so the
 * caller decides how/when to redraw.
 *
 * This replaces duplicated, buggy keyboard handling that used to live
 * directly inside MainView and CanvasController:
 *  - the old guard `if (selected != null && !overlaps(...)) return;` was
 *    inverted and swallowed every keypress in the common (non-overlapping)
 *    case; that guard is gone entirely here.
 *  - arrow-key moves and paste now check for overlap and revert, matching
 *    the same rule already enforced by mouse drag.
 */
public class KeyboardController {

    /** Caps memory growth on long sessions; oldest history is dropped once exceeded. */
    private static final int MAX_UNDO_DEPTH = 100;

    private final FieldService fieldService;
    private final Deque<List<BmsField>> undoStack;
    private final Deque<List<BmsField>> redoStack;

    private List<BmsField> fields;
    private BmsField selectedField;
    private BmsField clipboardField;

    private int rows;
    private int cols;

    /** Invoked after any change so the caller can redraw. */
    private Consumer<BmsField> onChange = f -> {};

    public KeyboardController(FieldService fieldService,
                              List<BmsField> fields,
                              int rows,
                              int cols) {
        this.fieldService = fieldService;
        this.fields = fields;
        this.rows = rows;
        this.cols = cols;
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
    }

    public void setOnChange(Consumer<BmsField> onChange) {
        this.onChange = onChange != null ? onChange : f -> {};
    }

    public void setFields(List<BmsField> fields) {
        this.fields = fields;
        this.selectedField = null;
        undoStack.clear();
        redoStack.clear();
    }

    public void setGridSize(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    public BmsField getSelectedField() {
        return selectedField;
    }

    public void setSelectedField(BmsField field) {
        this.selectedField = field;
    }

    public void handleKeyPress(KeyEvent e) {

        switch (e.getCode()) {

            case TAB:
                cycleSelection(!e.isShiftDown());
                e.consume();
                onChange.accept(selectedField);
                return;

            case Z:
                if (e.isControlDown()) undo();
                return;

            case Y:
                if (e.isControlDown()) redo();
                return;

            default:
                break;
        }

        if (selectedField == null) return;

        switch (e.getCode()) {

            case DELETE:
                saveState();
                fields.remove(selectedField);
                selectedField = null;
                break;

            case UP:
                saveState();
                moveField(selectedField.getCol(), Math.max(1, selectedField.getRow() - 1));
                break;

            case DOWN:
                saveState();
                moveField(selectedField.getCol(), Math.min(rows, selectedField.getRow() + 1));
                break;

            case LEFT:
                saveState();
                moveField(Math.max(1, selectedField.getCol() - 1), selectedField.getRow());
                break;

            case RIGHT:
                saveState();
                moveField(Math.min(cols - selectedField.getLength() + 1, selectedField.getCol() + 1),
                        selectedField.getRow());
                break;

            case C:
                if (e.isControlDown()) {
                    clipboardField = fieldService.copyExact(selectedField);
                }
                return;

            case V:
                if (e.isControlDown() && clipboardField != null) {
                    pasteClipboard();
                }
                return;

            default:
                return;
        }

        onChange.accept(selectedField);
    }

    private void pasteClipboard() {
        BmsField copy = fieldService.cloneField(clipboardField, fields);
        int targetCol = Math.min(cols - copy.getLength() + 1, clipboardField.getCol() + 1);
        copy.setRow(clipboardField.getRow());
        copy.setCol(targetCol);

        if (fieldService.overlaps(copy, fields)) {
            return; // can't place here; silently ignore like a no-op paste
        }

        saveState();
        fields.add(copy);
        selectedField = copy;
        onChange.accept(selectedField);
    }

    private void moveField(int newCol, int newRow) {
        int oldCol = selectedField.getCol();
        int oldRow = selectedField.getRow();
        selectedField.setCol(newCol);
        selectedField.setRow(newRow);
        if (fieldService.overlaps(selectedField, fields)) {
            selectedField.setCol(oldCol);
            selectedField.setRow(oldRow);
        }
    }

    /** Moves selection to the next (or previous) field in row/col order. */
    private void cycleSelection(boolean forward) {
        if (fields.isEmpty()) {
            selectedField = null;
            return;
        }

        List<BmsField> ordered = new ArrayList<>(fields);
        ordered.sort((a, b) -> {
            int rowCompare = Integer.compare(a.getRow(), b.getRow());
            return rowCompare != 0 ? rowCompare : Integer.compare(a.getCol(), b.getCol());
        });

        if (selectedField == null) {
            selectedField = forward ? ordered.get(0) : ordered.get(ordered.size() - 1);
            return;
        }

        int idx = ordered.indexOf(selectedField);
        if (idx == -1) {
            selectedField = ordered.get(0);
            return;
        }

        int nextIdx = forward
                ? (idx + 1) % ordered.size()
                : (idx - 1 + ordered.size()) % ordered.size();

        selectedField = ordered.get(nextIdx);
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        pushBounded(redoStack, cloneFields(fields));
        fields.clear();
        fields.addAll(undoStack.pop());
        selectedField = null;
        onChange.accept(null);
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        pushBounded(undoStack, cloneFields(fields));
        fields.clear();
        fields.addAll(redoStack.pop());
        selectedField = null;
        onChange.accept(null);
    }

    public void saveState() {
        pushBounded(undoStack, cloneFields(fields));
        redoStack.clear();
    }

    /**
     * Pushes an undo snapshot of the current field list, but with
     * currentField's entry replaced by preEditSnapshot. Needed because
     * FieldDialog mutates a field object by reference before this is
     * called, so the live list no longer holds the pre-edit values by the
     * time we'd normally snapshot it.
     */
    public void saveStateWithReplacement(BmsField currentField, BmsField preEditSnapshot) {
        List<BmsField> snapshot = new ArrayList<>();
        for (BmsField f : fields) {
            snapshot.add(f == currentField ? preEditSnapshot : fieldService.copyExact(f));
        }
        pushBounded(undoStack, snapshot);
        redoStack.clear();
    }

    /** Pushes a snapshot, evicting the oldest entry first if at capacity. */
    private void pushBounded(Deque<List<BmsField>> stack, List<BmsField> snapshot) {
        if (stack.size() >= MAX_UNDO_DEPTH) {
            stack.removeLast();
        }
        stack.push(snapshot);
    }

    private List<BmsField> cloneFields(List<BmsField> list) {
        List<BmsField> copy = new ArrayList<>();
        for (BmsField f : list) {
            copy.add(fieldService.copyExact(f));
        }
        return copy;
    }
}
