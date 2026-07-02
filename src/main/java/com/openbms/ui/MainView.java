package com.openbms.ui;

import javafx.scene.layout.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.geometry.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;

import com.openbms.model.*;
import com.openbms.service.*;
import com.openbms.ui.components.*;
import com.openbms.ui.dialogs.*;

/**
 * Thin coordinator for the designer window. Owns the layout (canvas center,
 * palette left, controls bottom, menu top) and wires together:
 *  - CanvasController: mouse interaction + field lifecycle on the canvas
 *  - MenuController: the menu bar and its actions
 *  - FieldDialog / ExportDialog / GridSizeDialog / RunDialog: modal flows
 *
 * Field movement, deletion, copy/paste, undo/redo, and Tab-navigation are
 * NOT implemented here — they live in KeyboardController (used internally
 * by CanvasController) so there is exactly one implementation of each,
 * instead of the three slightly-different copies that existed before this
 * refactor (one inline in the old MainView with an inverted guard bug that
 * swallowed almost every keypress, one in the old CanvasController that was
 * built but never actually wired to the canvas, and one in the dead
 * KeyboardController class).
 */
public class MainView {

    private static final int DEFAULT_ROWS = 24;
    private static final int DEFAULT_COLS = 80;
    private static final int CELL_WIDTH = 10;
    private static final int CELL_HEIGHT = 20;

    private int rows = DEFAULT_ROWS;
    private int cols = DEFAULT_COLS;

    private final BorderPane root;
    private Canvas canvas;
    private CanvasController canvasController;
    private MenuController menuController;

    private List<BmsField> fields = new ArrayList<>();
    private Tooltip currentTooltip = null;
    private VBox fieldPalette;

    private final FieldService fieldService = new FieldService();
    private final ExportService exportService = new ExportService();
    private final CanvasRenderer renderer = new CanvasRenderer(CELL_WIDTH, CELL_HEIGHT);
    private final AppSettings appSettings = new AppSettings();

    public MainView() {
        root = new BorderPane();
        setupCanvas();
        setupControls();
        setupPalette();
        setupMenu();
    }

    // ===== Canvas =====

    private void setupCanvas() {

        canvas = new Canvas(cols * CELL_WIDTH, rows * CELL_HEIGHT);
        canvas.setFocusTraversable(true);

        canvasController = new CanvasController(canvas, rows, cols, renderer, fieldService, fields);
        canvasController.setSettings(appSettings);

        canvasController.setOnEditRequest(this::editOrAddFieldDialog);

        canvasController.setOnChange(f -> updateTooltipFollowUp());

        canvas.setOnMouseMoved(this::handleTooltip);

        canvasController.redraw();
        root.setCenter(new StackPane(canvas));
    }

    private void updateTooltipFollowUp() {
        // Placeholder hook for future status-bar updates on selection change.
    }

    private void handleTooltip(MouseEvent e) {

        BmsField hovered = null;

        for (BmsField f : fields) {
            int fx = (f.getCol() - 1) * CELL_WIDTH;
            int fy = (f.getRow() - 1) * CELL_HEIGHT;
            int fw = f.getLength() * CELL_WIDTH;

            if (e.getX() >= fx && e.getX() <= fx + fw
                    && e.getY() >= fy && e.getY() <= fy + CELL_HEIGHT) {
                hovered = f;
                break;
            }
        }

        if (hovered != null) {
            if (currentTooltip == null) {
                currentTooltip = new Tooltip();
                Tooltip.install(canvas, currentTooltip);
            }
            currentTooltip.setText(
                    hovered.getName() +
                            " [" + hovered.getFieldType() + "] " +
                            "Pos=(" + hovered.getRow() +
                            "," + hovered.getCol() + ")"
            );
        } else if (currentTooltip != null) {
            Tooltip.uninstall(canvas, currentTooltip);
            currentTooltip = null;
        }
    }

    // ===== Palette =====

    private void setupPalette() {

        fieldPalette = new VBox(10);
        fieldPalette.setPadding(new Insets(10));
        fieldPalette.setStyle("-fx-background-color:#222;");

        Label label = new Label("Field Palette");
        label.setTextFill(Color.LIGHTGREEN);

        Button inputBtn = new Button("INPUT");
        Button outputBtn = new Button("OUTPUT");
        Button inoutBtn = new Button("INOUT");

        inputBtn.setOnAction(e -> startNewField(BmsField.FieldType.INPUT));
        outputBtn.setOnAction(e -> startNewField(BmsField.FieldType.OUTPUT));
        inoutBtn.setOnAction(e -> startNewField(BmsField.FieldType.INOUT));

        inputBtn.setMaxWidth(Double.MAX_VALUE);
        outputBtn.setMaxWidth(Double.MAX_VALUE);
        inoutBtn.setMaxWidth(Double.MAX_VALUE);

        fieldPalette.getChildren().addAll(label, inputBtn, outputBtn, inoutBtn);
        root.setLeft(fieldPalette);
    }

    private void startNewField(BmsField.FieldType type) {
        FieldDialog dialog = new FieldDialog(fieldService, fields, rows, cols, appSettings);
        dialog.showDialog(type, 1, 1).ifPresent(canvasController::addField);
    }

    // ===== Controls (bottom bar) =====

    private void setupControls() {

        Button exportButton = new Button("Generate BMS & Copybook");
        exportButton.getStyleClass().add("primary-button");
        exportButton.setOnAction(e -> exportMap());

        Button importButton = new Button("Import BMS Map");
        importButton.setOnAction(e -> importBmsMap());

        Button runButton = new Button("Run Screen");
        runButton.setOnAction(e -> runScreen());

        ToggleButton snapToggle = new ToggleButton("Snap To Grid");
        snapToggle.setSelected(true);
        snapToggle.selectedProperty().addListener((obs, oldV, newV) ->
                canvasController.setSnapEnabled(newV));

        HBox controls = new HBox(10, exportButton, importButton, runButton, snapToggle);
        controls.setPadding(new Insets(10));

        root.setBottom(controls);
    }

    private void exportMap() {

        if (!canvasController.hasFields()) {
            showWarning("No fields to export!");
            return;
        }

        ExportConfig config = ExportDialog.showExportDialog(root.getScene().getWindow());
        if (config == null) return;

        try {
            String bms = exportService.generateBms(config, rows, cols, fields);
            String copybook = exportService.generateCopybook(config.mapName(), fields);
            showResultDialog(config.mapName(), bms, copybook);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    // ===== Menu =====

    private void setupMenu() {

        menuController = new MenuController(
                () -> root.getScene() != null ? root.getScene().getWindow() : null,
                appSettings
        );

        menuController.setOnGridSizeChanged((newRows, newCols) -> {
            this.rows = newRows;
            this.cols = newCols;
            canvasController.setGridSize(newRows, newCols);
        });

        menuController.setOnImport(this::importBmsMap);
        menuController.setOnRun(this::runScreen);

        MenuBar menuBar = menuController.createMenu(() -> new int[]{ this.rows, this.cols });
        root.setTop(menuBar);
    }

    // ===== Run (CICS Emulator) =====

    private void runScreen() {

        if (!canvasController.hasFields()) {
            showWarning("No fields on this map yet — add some fields before running the screen.");
            return;
        }

        RunDialog.show(root.getScene().getWindow(), fields, rows, cols, appSettings);
    }

    // ===== Import =====

    private void importBmsMap() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import BMS Map");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("BMS Files", "*.bms")
        );

        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) return;

        try {
            String content = exportService.readFile(file);
            BmsParser.ParseResult result = exportService.importBms(content);

            canvasController.saveState();

            this.rows = result.rows;
            this.cols = result.cols;
            this.fields = result.fields;

            canvasController.setGridSize(this.rows, this.cols);
            canvasController.setFields(this.fields);

        } catch (IllegalArgumentException ex) {
            showError("Could not import file: " + ex.getMessage());
        } catch (IOException ex) {
            showError("Error reading file: " + ex.getMessage());
        }
    }

    // ===== Result / export-to-file dialog =====

    private void showResultDialog(String mapName, String bmsText, String copyBookText) {

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createExportTab("BMS", bmsText, "*.bms", mapName + ".bms"));
        tabPane.getTabs().add(createExportTab("Copybook", copyBookText, "*.cpy", mapName + ".cpy"));

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Export Result");
        dialog.getDialogPane().setContent(tabPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private Tab createExportTab(String title, String content, String extensionPattern, String defaultFileName) {

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        Button copyButton = new Button("Copy to Clipboard");
        copyButton.setOnAction(e -> copyToClipboard(content));

        Button exportButton = new Button("Export to File");
        exportButton.setOnAction(e -> exportToFile(content, extensionPattern, defaultFileName));

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

    private void exportToFile(String content, String extensionPattern, String defaultFileName) {

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(extensionPattern + " Files", extensionPattern)
        );
        chooser.setInitialFileName(defaultFileName);

        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;

        try {
            exportService.exportToFile(content, file);
        } catch (IOException ex) {
            showError("Error saving file: " + ex.getMessage());
        }
    }

    // ===== Field dialog helper =====

    private void editOrAddFieldDialog(BmsField field, int row, int col) {
        boolean isNew = (field == null) || !fields.contains(field);
        FieldDialog dialog = new FieldDialog(fieldService, fields, rows, cols, appSettings);

        // FieldDialog mutates `field` by reference before returning, so any
        // undo snapshot must be taken from a copy made BEFORE the dialog
        // runs - otherwise the "pre-edit" snapshot would actually contain
        // the post-edit values and undo would be a no-op.
        BmsField preEditSnapshot = (!isNew) ? fieldService.copyExact(field) : null;

        dialog.showDialog(field, row, col).ifPresent(f -> {
            if (isNew && !fields.contains(f)) {
                canvasController.addField(f); // addField saves undo state itself
            } else {
                canvasController.saveStateWithReplacement(field, preEditSnapshot);
                canvasController.redraw();
            }
        });
    }

    // ===== Alerts =====

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }
}
