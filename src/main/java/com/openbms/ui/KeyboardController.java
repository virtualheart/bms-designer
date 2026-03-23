package com.openbms.ui;

import javafx.scene.input.KeyEvent;
import javafx.scene.canvas.Canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.openbms.model.BmsField;
import com.openbms.ui.components.CanvasRenderer;

public class KeyboardController {

    private List<BmsField> fields;
    private BmsField selectedField;

    private Stack<List<BmsField>> undoStack;
    private Stack<List<BmsField>> redoStack;

    private int rows;
    private int cols;

    private CanvasRenderer renderer;
    private Canvas canvas;

    public KeyboardController(List<BmsField> fields,
                              BmsField selectedField,
                              Stack<List<BmsField>> undoStack,
                              Stack<List<BmsField>> redoStack,
                              int rows,
                              int cols,
                              CanvasRenderer renderer,
                              Canvas canvas) {

        this.fields = fields;
        this.selectedField = selectedField;
        this.undoStack = undoStack;
        this.redoStack = redoStack;
        this.rows = rows;
        this.cols = cols;
        this.renderer = renderer;
        this.canvas = canvas;
    }

    public void handleKeyPress(KeyEvent e) {
        if (selectedField == null) return;

        switch (e.getCode()) {

            case DELETE:
                saveState();
                fields.remove(selectedField);
                selectedField = null;
                break;

            case UP:
                selectedField.setRow(Math.max(1, selectedField.getRow() - 1));
                break;

            case DOWN:
                selectedField.setRow(Math.min(rows, selectedField.getRow() + 1));
                break;

            case LEFT:
                selectedField.setCol(Math.max(1, selectedField.getCol() - 1));
                break;

            case RIGHT:
                selectedField.setCol(Math.min(cols - selectedField.getLength() + 1,
                        selectedField.getCol() + 1));
                break;

            case Z:
                if (e.isControlDown() && !undoStack.isEmpty()) {
                    redoStack.push(cloneFields(fields));
                    fields.clear();
                    fields.addAll(undoStack.pop());
                    selectedField = null;
                }
                break;

            case Y:
                if (e.isControlDown() && !redoStack.isEmpty()) {
                    undoStack.push(cloneFields(fields));
                    fields.clear();
                    fields.addAll(redoStack.pop());
                    selectedField = null;
                }
                break;

            default:
                return;
        }

        renderer.drawFields(canvas, fields, rows, cols, selectedField);
    }

    public BmsField getSelectedField() {
        return selectedField;
    }

    public void setSelectedField(BmsField field) {
        this.selectedField = field;
    }

    private void saveState() {
        undoStack.push(cloneFields(fields));
    }

    private List<BmsField> cloneFields(List<BmsField> list) {
        List<BmsField> copy = new ArrayList<>();
        for (BmsField f : list) {
            BmsField clone = new BmsField();
            clone.setName(f.getName());
            clone.setFieldType(f.getFieldType());
            clone.setLength(f.getLength());
            clone.setColor(f.getColor());
            clone.setBgColor(f.getBgColor());
            clone.setProtection(f.getProtection());
            clone.setIntensity(f.getIntensity());
            clone.setInitialValue(f.getInitialValue());
            clone.setRow(f.getRow());
            clone.setCol(f.getCol());
            copy.add(clone);
        }
        return copy;
    }
}
