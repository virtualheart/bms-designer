package ui;

import javafx.scene.layout.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.geometry.*;
import javafx.event.ActionEvent;
import javafx.scene.paint.Color;
import model.*;
import service.*;
import java.util.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import ui.components.*;
import ui.dialogs.AboutDialog;
import ui.dialogs.FieldDialog;
import ui.dialogs.ExportDialog;
import ui.CanvasController;

public class MainView {

    private static final int DEFAULT_ROWS = 24;
    private static final int DEFAULT_COLS = 80;
    private static final int CELL_WIDTH = 10;
    private static final int CELL_HEIGHT = 20;

    private int rows = DEFAULT_ROWS;
    private int cols = DEFAULT_COLS;

    private BorderPane root;
    private Canvas canvas;
    private GraphicsContext gc;
    private CanvasController canvasController;

    private List<BmsField> fields = new ArrayList<>();
    private BmsField selectedField = null;

    private double dragOffsetX, dragOffsetY;
    private Tooltip currentTooltip = null;
    private VBox fieldPalette;

    private boolean snapEnabled = true;
    private boolean resizing = false;

    private BmsField clipboardField = null;
    private final ValidationService validationService = new ValidationService();
    private final ExportService exportService = new ExportService();
    private final Deque<List<BmsField>> undoStack = new ArrayDeque<>();
    private final Deque<List<BmsField>> redoStack = new ArrayDeque<>();
    private final CanvasRenderer renderer = new CanvasRenderer(CELL_WIDTH, CELL_HEIGHT);

    public MainView() {
        root = new BorderPane();
        setupCanvas();
        setupControls();
        setupPalette();
        setupMenu();
    }

    // Canvas
    private void setupCanvas() {

        canvas = new Canvas(cols * CELL_WIDTH, rows * CELL_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);

        canvasController = new CanvasController(canvas, rows, cols, renderer);
        
        renderer.drawGrid(canvas, rows, cols);

        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleDrag);
        // canvas.setOnMouseReleased(e -> selectedField = null);
        canvas.setOnMouseReleased(e -> {
            resizing = false;
        });
        canvas.setOnMouseMoved(this::handleTooltip);
        canvas.setOnKeyPressed(this::handleKeyPress);

        root.setCenter(new StackPane(canvas));
    }

    private void handleMousePressed(MouseEvent e) {

        int x = (int) e.getX();
        int y = (int) e.getY();
        selectedField = null;
        resizing = false;

        for (BmsField f : fields) {

            int fx = (f.getCol() - 1) * CELL_WIDTH;
            int fy = (f.getRow() - 1) * CELL_HEIGHT;
            int fw = f.getLength() * CELL_WIDTH;
            int fh = CELL_HEIGHT;

            // Detect resize zone (right 6px border)
            if (x >= fx + fw - 6 && x <= fx + fw &&
                    y >= fy && y <= fy + fh) {

                selectedField = f;
                resizing = true;
                return;
            }

            // Normal select
            if (x >= fx && x <= fx + fw &&
                    y >= fy && y <= fy + fh) {

                selectedField = f;
                dragOffsetX = x - fx;
                dragOffsetY = y - fy;

                // Right-click menu
                if (e.getButton() == MouseButton.SECONDARY) {

                    MenuItem editItem = new MenuItem("Edit Field");
                    editItem.setOnAction(ev ->
                            editOrAddFieldDialog(selectedField,
                                    f.getRow(),
                                    f.getCol()));

                    MenuItem deleteItem = new MenuItem("Delete Field");
                    deleteItem.setOnAction(ev -> {
                        saveState();
                        fields.remove(selectedField);
                        selectedField = null;
                        renderer.drawFields(canvas, fields, rows, cols, selectedField);
                    });                    

                    MenuItem cloneItem = new MenuItem("Clone Field");
                    cloneItem.setOnAction(ev -> {
                        // 
                    });

                    // new ContextMenu(editItem, deleteItem)
                    //         .show(canvas, e.getScreenX(), e.getScreenY());
                    
                    cloneItem.setOnAction(ev -> {
                        BmsField copy = cloneField(selectedField);
                        fields.add(copy);
                        renderer.drawFields(canvas, fields, rows, cols, selectedField);
                    });

                    new ContextMenu(editItem, cloneItem, deleteItem)
                            .show(canvas, e.getScreenX(), e.getScreenY());
                }

                // Double click
                if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                    editOrAddFieldDialog(selectedField, f.getRow(), f.getCol());
                }

                return;
            }
        }

        // Add new field
        if (e.getButton() == MouseButton.PRIMARY) {
            int col = x / CELL_WIDTH + 1;
            int row = y / CELL_HEIGHT + 1;

            BmsField temp = new BmsField();
            temp.setRow(row);
            temp.setCol(col);
            temp.setLength(5);

            if (renderer.overlaps(temp, fields)) {
                showWarning("Cannot place field here. Overlapping detected.");
                return;
            }

            editOrAddFieldDialog(null, row, col);
        }
    }

    private void handleDrag(MouseEvent e) {

        if (selectedField == null) return;

        if (resizing) {

            int startX = (selectedField.getCol() - 1) * CELL_WIDTH;
            int newLength = (int) ((e.getX() - startX) / CELL_WIDTH);

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
            newCol = (int) ((e.getX() - dragOffsetX) / CELL_WIDTH) + 1;
            newRow = (int) ((e.getY() - dragOffsetY) / CELL_HEIGHT) + 1;

        } else {

            // Free movement (pixel based)
            newCol = (int) ((e.getX()) / CELL_WIDTH) + 1;
            newRow = (int) ((e.getY()) / CELL_HEIGHT) + 1;
        }        

        newCol = Math.max(1, Math.min(cols - selectedField.getLength() + 1, newCol));
        newRow = Math.max(1, Math.min(rows, newRow));

        selectedField.setCol(newCol);
        selectedField.setRow(newRow);

        if (!renderer.overlaps(selectedField, fields))
            renderer.drawFields(canvas, fields, rows, cols, selectedField);
    }

    private void handleTooltip(MouseEvent e) {

        int x = (int) e.getX();
        int y = (int) e.getY();
        BmsField hovered = null;

        for (BmsField f : fields) {

            int fx = (f.getCol() - 1) * CELL_WIDTH;
            int fy = (f.getRow() - 1) * CELL_HEIGHT;
            int fw = f.getLength() * CELL_WIDTH;

            if (x >= fx && x <= fx + fw &&
                    y >= fy && y <= fy + CELL_HEIGHT) {
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

    private String getAutoIncrementFieldName() {
        int index = 1;
        String baseName = "FIELD";
        while (validationService.fieldNameExists(baseName + index, null, fields)) {
            index++;
        }
        return baseName + index;
    }

    // Palette
    private void setupPalette() {

        fieldPalette = new VBox(10);
        fieldPalette.setPadding(new Insets(10));
        fieldPalette.setStyle("-fx-background-color:#222;");

        Label label = new Label("Field Palette");
        label.setTextFill(Color.LIGHTGREEN);

        Button inputBtn = new Button("INPUT");
        Button outputBtn = new Button("OUTPUT");
        Button inoutBtn = new Button("INOUT");

        inputBtn.setOnAction(e -> {
            BmsField f = new BmsField();
            f.setFieldType(BmsField.FieldType.INPUT);
            editOrAddFieldDialog(f, 1, 1);
        });

        outputBtn.setOnAction(e -> {
            BmsField f = new BmsField();
            f.setFieldType(BmsField.FieldType.OUTPUT);
            editOrAddFieldDialog(f, 1, 1);
        });

        inoutBtn.setOnAction(e -> {
            BmsField f = new BmsField();
            f.setFieldType(BmsField.FieldType.INOUT);
            editOrAddFieldDialog(f, 1, 1);
        });

        inputBtn.setMaxWidth(Double.MAX_VALUE);
        outputBtn.setMaxWidth(Double.MAX_VALUE);
        inoutBtn.setMaxWidth(Double.MAX_VALUE);

        fieldPalette.getChildren().addAll(label, inputBtn, outputBtn, inoutBtn);
        root.setLeft(fieldPalette);
    }

    // Keyboard
    private void handleKeyPress(KeyEvent e) {
        if (selectedField != null && !renderer.overlaps(selectedField, fields)) return;

        switch (e.getCode()) {
            case DELETE:
            System.out.println("delet");
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
                selectedField.setCol(
                        Math.min(cols - selectedField.getLength() + 1,
                                selectedField.getCol() + 1));
                break;

            case C:
                if (e.isControlDown())
                    clipboardField = cloneField(selectedField);
                break;

            case V:
                if (e.isControlDown() && clipboardField != null) {
                    BmsField copy = cloneField(clipboardField);
                    copy.setCol(copy.getCol() + 1);
                    fields.add(copy);
                    renderer.drawFields(canvas, fields, rows, cols, selectedField);
                }
                break;
            case Z:
                if (e.isControlDown() && !undoStack.isEmpty()) {
                    redoStack.push(new ArrayList<>(fields));
                    fields = new ArrayList<>(undoStack.pop());
                    renderer.drawFields(canvas, fields, rows, cols, selectedField);
                }
                break;

            case Y:
                if (e.isControlDown() && !redoStack.isEmpty()) {
                    undoStack.push(new ArrayList<>(fields));
                    fields = new ArrayList<>(redoStack.pop());
                    renderer.drawFields(canvas, fields, rows, cols, selectedField);
                }
                break;
            default:
                return;
        }

        if (!renderer.overlaps(selectedField, fields))
            renderer.drawFields(canvas, fields, rows, cols, selectedField);
    }

    // Controls & Menu
    private void setupControls() {

        Button exportButton = new Button("Generate BMS & Copybook");
        exportButton.getStyleClass().add("primary-button");

        exportButton.setOnAction(e -> {

            if (fields == null || fields.isEmpty()) {
                showWarning("No fields to export!");
                return;
            }

            ExportConfig config =
                    ExportDialog.showExportDialog(root.getScene().getWindow());

            if (config == null) return;

            String bms =
                    exportService.generateBms(config, rows, cols, fields);

            String copybook =
                    exportService.generateCopybook(config.mapName(), fields);

            showResultDialog(config.mapName(), bms, copybook);

        });        

        ToggleButton snapToggle = new ToggleButton("Snap To Grid");
        snapToggle.setSelected(true);
        
        snapToggle.selectedProperty().addListener((obs, oldV, newV) -> snapEnabled = newV);

        HBox controls = new HBox(10, exportButton, snapToggle);
        controls.setPadding(new Insets(10));

        root.setBottom(controls);
    }

    private void setupMenu() {

        MenuBar menuBar = new MenuBar();

        Menu settingsMenu = new Menu("Settings");
        MenuItem modifyGrid = new MenuItem("Modify Rows/Columns");
        modifyGrid.setOnAction(e -> showGridSizeDialog());
        settingsMenu.getItems().add(modifyGrid);

        Menu aboutMenu = new Menu("About");
        MenuItem aboutItem = new MenuItem("About BMS Map Editor");
        aboutItem.setOnAction(e -> AboutDialog.show());
        aboutMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(settingsMenu, aboutMenu);
        root.setTop(menuBar);
    }

    private void showGridSizeDialog() {
        TextInputDialog dialog = new TextInputDialog("24x80");
        dialog.setTitle("Modify Grid Size");
        dialog.setHeaderText("Enter new grid size:");
        dialog.setContentText("Grid size (e.g., 24x80):");

        dialog.showAndWait().ifPresent(input -> {
            String[] parts = input.split("x");
            if (parts.length == 2) {
                try {
                    int rows = Integer.parseInt(parts[0].trim());
                    int cols = Integer.parseInt(parts[1].trim());

                    if (rows > 0 && cols > 0) {
                        this.rows = rows;
                        this.cols = cols;
                        resetCanvas();
                    } else {
                        showError("Invalid grid size");
                    }
                } catch (NumberFormatException e) {
                    showError("Invalid grid format");
                }
            } else {
                showError("Invalid format. Use 'rows x cols' (e.g., 24x80)");
            }
        });
    }

    private void resetCanvas() {
        canvas.setWidth(cols * CELL_WIDTH);
        canvas.setHeight(rows * CELL_HEIGHT);
        renderer.drawGrid(canvas, rows, cols);
        renderer.drawFields(canvas, fields, rows, cols, selectedField);
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

    // Export Helpers
    private void exportToFile(String content,
                              String extensionPattern,
                              String defaultFileName) {

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

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }

    // Field Dialog Helper
    private void editOrAddFieldDialog(BmsField field, int row, int col) {
        FieldDialog dialog = new FieldDialog(new FieldService(), fields);
        dialog.showDialog(field, row, col)
              .ifPresent(f -> renderer.drawFields(canvas, fields, rows, cols, selectedField));
    }

    // Clone Field Helper
    private BmsField cloneField(BmsField original) {
        BmsField copy = new BmsField();
        copy.setName(original.getName() + "_COPY");
        copy.setCol(original.getCol());
        copy.setRow(Math.min(rows, original.getRow() + 1));
        copy.setLength(original.getLength());
        copy.setFieldType(original.getFieldType());
        copy.setColor(original.getColor());
        copy.setBgColor(original.getBgColor());
        copy.setProtection(original.getProtection());
        copy.setIntensity(original.getIntensity());
        copy.setInitialValue(original.getInitialValue());
        return copy;
    }

    private void saveState() {
        List<BmsField> snapshot = new ArrayList<>();
        for (BmsField f : fields) {
            snapshot.add(cloneField(f));
        }
        undoStack.push(snapshot);
    }
}