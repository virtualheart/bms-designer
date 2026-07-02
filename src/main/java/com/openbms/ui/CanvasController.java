package com.openbms.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.openbms.model.AppSettings;
import com.openbms.model.BmsField;
import com.openbms.service.FieldService;
import com.openbms.ui.components.CanvasRenderer;

/**
 * Owns the design canvas: field selection, drag-to-move, drag-to-resize, and
 * the right-click context menu (edit / clone / delete). Keyboard navigation
 * and editing is delegated to KeyboardController so the two don't duplicate
 * overlap-checking and undo/redo logic.
 *
 * Replaces the previous CanvasController, which was constructed by MainView
 * but then had every one of its canvas event handlers immediately
 * overwritten by MainView's own duplicate handlers — making the whole object
 * dead weight. This version is the one actually driving the canvas.
 */
public class CanvasController {

    private final Canvas canvas;
    private final CanvasRenderer renderer;
    private final FieldService fieldService;
    private final KeyboardController keyboardController;

    private List<BmsField> fields;
    private BmsField selectedField;
    private AppSettings settings;

    private boolean resizing = false;
    private boolean snapEnabled = true;
    private boolean dragStateSaved = false;

    private double dragOffsetX;
    private double dragOffsetY;

    private int rows;
    private int cols;

    /** Invoked whenever fields/selection change, so MainView can refresh UI chrome. */
    private Consumer<BmsField> onChange = f -> {};

    /** Invoked when the user double-clicks a field or an empty cell, to open the field editor. */
    private FieldEditRequestListener onEditRequest = (field, row, col) -> {};

    public interface FieldEditRequestListener {
        void onEditRequest(BmsField field, int row, int col);
    }

    public CanvasController(Canvas canvas,
                            int rows,
                            int cols,
                            CanvasRenderer renderer,
                            FieldService fieldService,
                            List<BmsField> fields) {
        this.canvas = canvas;
        this.rows = rows;
        this.cols = cols;
        this.renderer = renderer;
        this.fieldService = fieldService;
        this.fields = fields;
        this.keyboardController = new KeyboardController(fieldService, fields, rows, cols);

        keyboardController.setOnChange(f -> {
            this.selectedField = f;
            redraw();
            onChange.accept(f);
        });

        registerEvents();
    }

    private void registerEvents() {
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleDrag);
        canvas.setOnMouseReleased(e -> {
            resizing = false;
            dragStateSaved = false;
        });
        canvas.setOnKeyPressed(this::handleKeyPress);
        canvas.setFocusTraversable(true);
    }

    public void setOnChange(Consumer<BmsField> onChange) {
        this.onChange = onChange != null ? onChange : f -> {};
    }

    public void setOnEditRequest(FieldEditRequestListener listener) {
        this.onEditRequest = listener != null ? listener : (f, r, c) -> {};
    }

    public void setSnapEnabled(boolean snapEnabled) {
        this.snapEnabled = snapEnabled;
    }

    public void setSettings(AppSettings settings) {
        this.settings = settings;
    }

    // ===== Mouse handling =====

    private void handleMousePressed(MouseEvent e) {

        canvas.requestFocus();
        resizing = false;
        dragStateSaved = false;
        BmsField hit = findFieldAt(e.getX(), e.getY());

        if (hit == null) {
            selectedField = null;
            redraw();

            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                int col = (int) (e.getX() / renderer.getCellWidth()) + 1;
                int row = (int) (e.getY() / renderer.getCellHeight()) + 1;
                onEditRequest.onEditRequest(null, row, col);
            }
            return;
        }

        selectedField = hit;
        keyboardController.setSelectedField(hit);

        int fx = (hit.getCol() - 1) * renderer.getCellWidth();
        int fy = (hit.getRow() - 1) * renderer.getCellHeight();
        int fw = hit.getLength() * renderer.getCellWidth();

        // Resize handle: right edge, 6px hot zone
        if (e.getX() >= fx + fw - 6 && e.getX() <= fx + fw) {
            resizing = true;
        } else {
            dragOffsetX = e.getX() - fx;
            dragOffsetY = e.getY() - fy;
        }

        if (e.getButton() == MouseButton.SECONDARY) {
            showContextMenu(hit, e.getScreenX(), e.getScreenY());
        } else if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
            onEditRequest.onEditRequest(hit, hit.getRow(), hit.getCol());
        }

        redraw();
        onChange.accept(selectedField);
    }

    private void showContextMenu(BmsField field, double screenX, double screenY) {

        MenuItem editItem = new MenuItem("Edit Field");
        editItem.setOnAction(ev -> onEditRequest.onEditRequest(field, field.getRow(), field.getCol()));

        MenuItem cloneItem = new MenuItem("Clone Field");
        cloneItem.setOnAction(ev -> cloneSelected());

        MenuItem deleteItem = new MenuItem("Delete Field");
        deleteItem.setOnAction(ev -> deleteSelected());

        new ContextMenu(editItem, cloneItem, deleteItem).show(canvas, screenX, screenY);
    }

    private void handleDrag(MouseEvent e) {
        if (selectedField == null) return;

        if (!dragStateSaved) {
            keyboardController.saveState();
            dragStateSaved = true;
        }

        if (resizing) {
            resizeSelected(e.getX());
            return;
        }

        moveSelectedTo(e.getX(), e.getY());
    }

    private void resizeSelected(double mouseX) {
        int startX = (selectedField.getCol() - 1) * renderer.getCellWidth();
        int newLength = (int) ((mouseX - startX) / renderer.getCellWidth());

        if (newLength <= 0 || selectedField.getCol() + newLength - 1 > cols) return;

        int oldLength = selectedField.getLength();
        selectedField.setLength(newLength);

        if (fieldService.overlaps(selectedField, fields)) {
            selectedField.setLength(oldLength);
        } else {
            redraw();
        }
    }

    private void moveSelectedTo(double mouseX, double mouseY) {
        int newCol;
        int newRow;

        if (snapEnabled) {
            newCol = (int) ((mouseX - dragOffsetX) / renderer.getCellWidth()) + 1;
            newRow = (int) ((mouseY - dragOffsetY) / renderer.getCellHeight()) + 1;
            newCol = snapToIncrement(newCol);
            newRow = snapToIncrement(newRow);
        } else {
            newCol = (int) (mouseX / renderer.getCellWidth()) + 1;
            newRow = (int) (mouseY / renderer.getCellHeight()) + 1;
        }

        newCol = Math.max(1, Math.min(cols - selectedField.getLength() + 1, newCol));
        newRow = Math.max(1, Math.min(rows, newRow));

        int oldCol = selectedField.getCol();
        int oldRow = selectedField.getRow();

        selectedField.setCol(newCol);
        selectedField.setRow(newRow);

        if (fieldService.overlaps(selectedField, fields)) {
            selectedField.setCol(oldCol);
            selectedField.setRow(oldRow);
        } else {
            redraw();
        }
    }

    /** Rounds a 1-based grid coordinate to the nearest multiple of the configured snap size. */
    private int snapToIncrement(int coordinate) {
        int snapSize = settings != null ? settings.getSnapSize() : 1;
        if (snapSize <= 1) return coordinate;
        int zeroBased = coordinate - 1;
        int rounded = Math.round((float) zeroBased / snapSize) * snapSize;
        return rounded + 1;
    }

    private BmsField findFieldAt(double x, double y) {
        for (BmsField f : fields) {
            if (renderer.isInside(f, x, y)) {
                return f;
            }
        }
        return null;
    }

    // ===== Keyboard delegation =====

    private void handleKeyPress(KeyEvent e) {
        keyboardController.handleKeyPress(e);
        // KeyboardController's onChange callback (wired in the constructor)
        // already syncs selectedField and redraws; nothing further needed here.
    }

    // ===== Field lifecycle =====

    public void addField(BmsField field) {
        if (fieldService.overlaps(field, fields)) return;
        keyboardController.saveState();
        fields.add(field);
        selectedField = field;
        keyboardController.setSelectedField(field);
        redraw();
        onChange.accept(selectedField);
    }

    public void cloneSelected() {
        if (selectedField == null) return;
        BmsField copy = fieldService.cloneField(selectedField, fields);
        copy.setRow(Math.min(rows, selectedField.getRow() + 1));
        if (fieldService.overlaps(copy, fields)) return;

        keyboardController.saveState();
        fields.add(copy);
        selectedField = copy;
        keyboardController.setSelectedField(copy);
        redraw();
        onChange.accept(selectedField);
    }

    public void deleteSelected() {
        if (selectedField == null) return;
        keyboardController.saveState();
        fields.remove(selectedField);
        selectedField = null;
        keyboardController.setSelectedField(null);
        redraw();
        onChange.accept(null);
    }

    public void saveState() {
        keyboardController.saveState();
    }

    public void saveStateWithReplacement(BmsField currentField, BmsField preEditSnapshot) {
        keyboardController.saveStateWithReplacement(currentField, preEditSnapshot);
    }

    // ===== State sync =====

    public List<BmsField> getFields() {
        return fields;
    }

    public BmsField getSelectedField() {
        return selectedField;
    }

    public void setFields(List<BmsField> fields) {
        this.fields = fields;
        this.selectedField = null;
        keyboardController.setFields(fields);
        redraw();
    }

    public void setGridSize(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        keyboardController.setGridSize(rows, cols);
        canvas.setWidth(cols * renderer.getCellWidth());
        canvas.setHeight(rows * renderer.getCellHeight());
        redraw();
    }

    public boolean hasFields() {
        return !fields.isEmpty();
    }

    public void redraw() {
        renderer.drawFields(canvas, fields, rows, cols, selectedField);
    }
}
