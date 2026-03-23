package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import model.BmsField;
import ui.components.CanvasRenderer;

import service.ExportService;
import model.ExportConfig;
import ui.dialogs.ExportDialog;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CanvasController {

    private Canvas canvas;

    private List<BmsField> fields = new ArrayList<>();
    private ExportService exportService = new ExportService();
    private BmsField selectedField;

    private CanvasRenderer renderer;

    private boolean resizing = false;
    private boolean snapEnabled = true;

    private double dragOffsetX;
    private double dragOffsetY;

    private Stack<List<BmsField>> undoStack = new Stack<>();
    private Stack<List<BmsField>> redoStack = new Stack<>();

    private BmsField clipboardField;

    private int rows;
    private int cols;

    public CanvasController(Canvas canvas, int rows, int cols, CanvasRenderer renderer) {
        this.canvas = canvas;
        this.rows = rows;
        this.cols = cols;
        this.renderer = renderer;

        registerEvents();
    }

    private void registerEvents() {
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleDrag);
        canvas.setOnMouseReleased(e -> resizing = false);
        canvas.setOnKeyPressed(this::handleKeyPress);
        canvas.setFocusTraversable(true);
    }

    private void handleMousePressed(MouseEvent e) {
        for (BmsField f : fields) {
            int x = (f.getCol() - 1) * renderer.getCellWidth();
            int y = (f.getRow() - 1) * renderer.getCellHeight();
            int w = f.getLength() * renderer.getCellWidth();

            if (e.getX() >= x && e.getX() <= x + w
                    && e.getY() >= y && e.getY() <= y + renderer.getCellHeight()) {

                selectedField = f;
                dragOffsetX = e.getX() - (f.getCol() - 1) * renderer.getCellWidth();
                dragOffsetY = e.getY() - (f.getRow() - 1) * renderer.getCellHeight();

                renderer.drawFields(canvas, fields, rows, cols, selectedField);
                return;
            }
        }

        selectedField = null;
        renderer.drawFields(canvas, fields, rows, cols, selectedField);
    }

    private void handleDrag(MouseEvent e) {
        if (selectedField == null) return;

        if (resizing) {
            int startX = (selectedField.getCol() - 1) * renderer.getCellWidth();
            int newLength = (int) ((e.getX() - startX) / renderer.getCellWidth());

            if (newLength > 0 && selectedField.getCol() + newLength - 1 <= cols) {
                int oldLength = selectedField.getLength();
                selectedField.setLength(newLength);

                if (renderer.overlaps(selectedField, fields)) {
                    selectedField.setLength(oldLength);
                } else {
                    renderer.drawFields(canvas, fields, rows, cols, selectedField);
                }
            }
            return;
        }

        int newCol;
        int newRow;

        if (snapEnabled) {
            newCol = (int) ((e.getX() - dragOffsetX) / renderer.getCellWidth()) + 1;
            newRow = (int) ((e.getY() - dragOffsetY) / renderer.getCellHeight()) + 1;
        } else {
            newCol = (int) (e.getX() / renderer.getCellWidth()) + 1;
            newRow = (int) (e.getY() / renderer.getCellHeight()) + 1;
        }

        newCol = Math.max(1, Math.min(cols - selectedField.getLength() + 1, newCol));
        newRow = Math.max(1, Math.min(rows, newRow));

        int oldCol = selectedField.getCol();
        int oldRow = selectedField.getRow();

        selectedField.setCol(newCol);
        selectedField.setRow(newRow);

        if (renderer.overlaps(selectedField, fields)) {
            selectedField.setCol(oldCol);
            selectedField.setRow(oldRow);
        } else {
            renderer.drawFields(canvas, fields, rows, cols, selectedField);
        }
    }

    private void handleKeyPress(KeyEvent e) {
        if (selectedField == null) return;

        switch (e.getCode()) {
            case DELETE:
                saveState();
                fields.remove(selectedField);
                selectedField = null;
                renderer.drawFields(canvas, fields, rows, cols, null);
                return;

            case UP:
                moveField(selectedField.getCol(), Math.max(1, selectedField.getRow() - 1));
                break;

            case DOWN:
                moveField(selectedField.getCol(), Math.min(rows, selectedField.getRow() + 1));
                break;

            case LEFT:
                moveField(Math.max(1, selectedField.getCol() - 1), selectedField.getRow());
                break;

            case RIGHT:
                moveField(Math.min(cols - selectedField.getLength() + 1, selectedField.getCol() + 1),
                        selectedField.getRow());
                break;

            case C:
                if (e.isControlDown())
                    clipboardField = cloneField(selectedField);
                return;

            case V:
                if (e.isControlDown() && clipboardField != null) {
                    BmsField copy = cloneField(clipboardField);
                    copy.setCol(Math.min(cols - copy.getLength() + 1, copy.getCol() + 1));
                    if (!renderer.overlaps(copy, fields)) {
                        saveState();
                        fields.add(copy);
                        renderer.drawFields(canvas, fields, rows, cols, selectedField);
                    }
                }
                return;

            case Z:
                if (e.isControlDown() && !undoStack.isEmpty()) {
                    redoStack.push(cloneFields(fields));
                    fields.clear();
                    fields.addAll(undoStack.pop());
                    selectedField = null;
                    renderer.drawFields(canvas, fields, rows, cols, null);
                }
                return;

            case Y:
                if (e.isControlDown() && !redoStack.isEmpty()) {
                    undoStack.push(cloneFields(fields));
                    fields.clear();
                    fields.addAll(redoStack.pop());
                    selectedField = null;
                    renderer.drawFields(canvas, fields, rows, cols, null);
                }
                return;

            default:
                return;
        }

        renderer.drawFields(canvas, fields, rows, cols, selectedField);
    }

    private void moveField(int newCol, int newRow) {
        int oldCol = selectedField.getCol();
        int oldRow = selectedField.getRow();
        selectedField.setCol(newCol);
        selectedField.setRow(newRow);
        if (renderer.overlaps(selectedField, fields)) {
            selectedField.setCol(oldCol);
            selectedField.setRow(oldRow);
        }
    }

    private void saveState() {
        undoStack.push(cloneFields(fields));
        redoStack.clear();
    }

    private BmsField cloneField(BmsField f) {
        BmsField copy = new BmsField();
        copy.setName(f.getName());
        copy.setFieldType(f.getFieldType());
        copy.setLength(f.getLength());
        copy.setColor(f.getColor());
        copy.setBgColor(f.getBgColor());
        copy.setProtection(f.getProtection());
        copy.setIntensity(f.getIntensity());
        copy.setInitialValue(f.getInitialValue());
        copy.setRow(f.getRow());
        copy.setCol(f.getCol());
        return copy;
    }

    private List<BmsField> cloneFields(List<BmsField> list) {
        List<BmsField> copy = new ArrayList<>();
        for (BmsField f : list) copy.add(cloneField(f));
        return copy;
    }

    // Public API

    public List<BmsField> getFields() { return fields; }

    public BmsField getSelectedField() { return selectedField; }

    public void setFields(List<BmsField> fields) {
        this.fields = fields;
        selectedField = null;
        renderer.drawFields(canvas, this.fields, rows, cols, null);
    }

    public void setGridSize(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        renderer.drawFields(canvas, fields, rows, cols, selectedField);
    }

    public void addField(BmsField field) {
        if (!renderer.overlaps(field, fields)) {
            saveState();
            fields.add(field);
            renderer.drawFields(canvas, fields, rows, cols, field);
            selectedField = field;
        }
    }

    public boolean hasFields() {
        return !fields.isEmpty();
    }

    public void exportMap(javafx.stage.Window owner) {

        if (fields.isEmpty()) {
            System.out.println("No fields to export");
            return;
        }

        ExportConfig config = ExportDialog.showExportDialog(owner);

        if (config == null) return;

        String bms =
                exportService.generateBms(config, rows, cols, fields);

        String copybook =
                exportService.generateCopybook(config.mapName(), fields);

        showResultDialog(config.mapName(), bms, copybook);
    }

    private void showResultDialog(String mapName,
                                  String bmsText,
                                  String copyBookText) {

        TabPane tabPane = new TabPane();

        tabPane.getTabs().add(
                createExportTab("BMS", bmsText, "*.bms", mapName + ".bms")
        );

        tabPane.getTabs().add(
                createExportTab("Copybook", copyBookText, "*.cpy", mapName + ".cpy")
        );

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Export Result");
        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private Tab createExportTab(String title,
                                String content,
                                String extensionPattern,
                                String defaultFileName) {

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        Button copyButton = new Button("Copy to Clipboard");
        copyButton.setOnAction(e -> copyToClipboard(content));

        Button exportButton = new Button("Export to File");
        exportButton.setOnAction(e ->
                exportToFile(content, extensionPattern, defaultFileName)
        );

        VBox box = new VBox(5, textArea, copyButton, exportButton);
        box.setPadding(new Insets(5));

        Tab tab = new Tab(title, box);
        tab.setClosable(false);

        return tab;
    }

    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void exportToFile(String content,
                              String extensionPattern,
                              String defaultFileName) {

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        extensionPattern + " Files",
                        extensionPattern
                )
        );
        chooser.setInitialFileName(defaultFileName);

        File file = chooser.showSaveDialog(null);

        if (file == null) return;

        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(file))) {

            writer.write(content);

        } catch (IOException ex) {
            showError("Error saving file: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

}
