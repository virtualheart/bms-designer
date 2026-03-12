package ui;

import javafx.scene.layout.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.geometry.*;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import model.BmsField;
import service.BmsGenerator;
import java.util.*;
import javafx.stage.FileChooser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;

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

    private List<BmsField> fields = new ArrayList<>();
    private BmsField selectedField = null;

    private double dragOffsetX, dragOffsetY;
    private Tooltip currentTooltip = null;
    private VBox fieldPalette;
    
    private boolean snapEnabled = true;
    private boolean resizing = false;
    
    private BmsField clipboardField = null;
    
    public MainView() {
        root = new BorderPane();
        setupCanvas();
        setupControls();
        setupPalette();
        setupMenu();
    }

    // =====================================================
    // Canvas
    // =====================================================

    private void setupCanvas() {

        canvas = new Canvas(cols * CELL_WIDTH, rows * CELL_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);

        drawGrid();

        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleDrag);
        canvas.setOnMouseReleased(e -> selectedField = null);
        canvas.setOnMouseMoved(this::handleTooltip);
        canvas.setOnKeyPressed(this::handleKeyPress);

        // Ruler canvas
        // Canvas hRuler = new Canvas(cols * CELL_WIDTH, 20);
        // Canvas vRuler = new Canvas(40, rows * CELL_HEIGHT);

        // drawHorizontalRuler(hRuler.getGraphicsContext2D());
        // drawVerticalRuler(vRuler.getGraphicsContext2D());

        // StackPane canvasPane = new StackPane(canvas);

        // BorderPane canvasWithRulers = new BorderPane();
        // canvasWithRulers.setTop(hRuler);
        // canvasWithRulers.setLeft(vRuler);
        // canvasWithRulers.setCenter(canvasPane);

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
                            editOrAddFieldDialog(
                                    selectedField,
                                    f.getRow(),
                                    f.getCol()));

                    MenuItem deleteItem = new MenuItem("Delete Field");
                    deleteItem.setOnAction(ev -> {
                        fields.remove(selectedField);
                        selectedField = null;
                        redrawFields();
                    });

                    new ContextMenu(editItem, deleteItem)
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
            editOrAddFieldDialog(null, row, col);
        }
    }

    private void handleDrag(MouseEvent e) {

        if (selectedField == null) return;

        if (resizing) {

            int startX = (selectedField.getCol() - 1) * CELL_WIDTH;
            int newLength =
                    (int)((e.getX() - startX) / CELL_WIDTH);

            if (newLength > 0 &&
                    selectedField.getCol() + newLength - 1 <= cols) {

                int oldLength = selectedField.getLength();
                selectedField.setLength(newLength);

                if (overlaps(selectedField)) {
                    selectedField.setLength(oldLength);
                } else {
                    redrawFields();
                }
            }

            return;
        }

        int newCol =
                (int)((e.getX() - dragOffsetX) / CELL_WIDTH) + 1;

        int newRow =
                (int)((e.getY() - dragOffsetY) / CELL_HEIGHT) + 1;

        if (snapEnabled) {
            newCol = Math.round(newCol);
            newRow = Math.round(newRow);
        }

        newCol = Math.max(1,
                Math.min(cols - selectedField.getLength() + 1, newCol));
        newRow = Math.max(1, Math.min(rows, newRow));

        selectedField.setCol(newCol);
        selectedField.setRow(newRow);

        if (!overlaps(selectedField))
            redrawFields();
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

    // =====================================================
    // Dialog
    // =====================================================
    private void editOrAddFieldDialog(BmsField field, int row, int col) {

        boolean isNew = (field == null);
        BmsField f = isNew ? new BmsField() : field;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Add Field" : "Edit Field");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(f.getName());

        if (isNew) {
            String autoName = getAutoIncrementFieldName();
            nameField.setText(autoName);
        }

        ChoiceBox<BmsField.FieldType> typeBox = new ChoiceBox<>();
        typeBox.getItems().addAll(BmsField.FieldType.values());
        typeBox.setValue(f.getFieldType());

        TextField initialField = new TextField(f.getInitialValue());

        TextField lengthField = new TextField(String.valueOf(f.getLength()));

        ChoiceBox<BmsField.BmsColor> colorBox = new ChoiceBox<>();
        colorBox.getItems().addAll(BmsField.BmsColor.values());
        colorBox.setValue(f.getColor());

        ChoiceBox<BmsField.BmsColor> bgColorBox = new ChoiceBox<>();
        bgColorBox.getItems().addAll(BmsField.BmsColor.values());
        bgColorBox.setValue(f.getBgColor());

        ChoiceBox<BmsField.Protection> protectionBox = new ChoiceBox<>();
        protectionBox.getItems().addAll(BmsField.Protection.values());
        protectionBox.setValue(f.getProtection());

        ChoiceBox<BmsField.Intensity> intensityBox = new ChoiceBox<>();
        intensityBox.getItems().addAll(BmsField.Intensity.values());
        intensityBox.setValue(f.getIntensity());

        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Type:"), typeBox);
        grid.addRow(2, new Label("Length:"), lengthField);
        grid.addRow(3, new Label("Text Color:"), colorBox);
        grid.addRow(4, new Label("Background:"), bgColorBox);
        grid.addRow(5, new Label("Protection:"), protectionBox);
        grid.addRow(6, new Label("Intensity:"), intensityBox);
        grid.addRow(7, new Label("Initial:"), initialField);

        dialog.getDialogPane().setContent(grid);

        initialField.textProperty().addListener((observable, oldValue, newValue) -> {
            lengthField.setText(String.valueOf(newValue.length()));
        });

        // Handle OK button click
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                String fieldName = nameField.getText().trim().toUpperCase();
                int len = Integer.parseInt(lengthField.getText());
                String initVal = initialField.getText();

                if (len <= 0) {
                    showError("Length must be positive number.");
                    event.consume();
                    return;
                }

                if (!isValidFieldName(fieldName)) {
                    showError("Invalid field name.\n" +
                            "1–30 chars.\n" +
                            "A-Z 0-9 @ # $ - _\n" +
                            "Must start with A-Z or @#$");
                    event.consume();
                    return;
                }

                if (fieldNameExists(fieldName, f)) {
                    showError("Field name already exists in this map.");
                    event.consume();
                    return;
                }

                if (initVal.length() > len) {
                    showError("Initial value cannot exceed field length (" + len + ").");
                    event.consume();
                    return;
                }

                // Assigning field properties
                f.setName(fieldName);
                f.setLength(len);
                f.setFieldType(typeBox.getValue());
                f.setColor(colorBox.getValue());
                f.setBgColor(bgColorBox.getValue());
                BmsField.Protection selectedProtection = protectionBox.getValue();
                f.setProtection(selectedProtection);
                f.setIntensity(intensityBox.getValue());
                f.setInitialValue(initVal);
                f.setRow(row);
                f.setCol(col);


                boolean userExplicitlyChangedProtection =
                        protectionBox.getValue() != f.getProtection();

                if (f.getFieldType() == BmsField.FieldType.OUTPUT
                        && !initVal.isBlank()
                        && selectedProtection == BmsField.Protection.PROT) {

                    f.setProtection(BmsField.Protection.ASKIP);
                }

                if (f.isLikelyLabel() && !userExplicitlyChangedProtection) {
                    f.setProtection(BmsField.Protection.ASKIP);
                }

                if (overlaps(f)) {
                    showError("Field overlaps another field.");
                    event.consume();
                    return;
                }

                if (isNew) fields.add(f);

                redrawFields();

            } catch (NumberFormatException ex) {
                showError("Length must be a positive number.");
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private String getAutoIncrementFieldName() {
        int index = 1;
        String baseName = "FIELD";
        while (fieldNameExists(baseName + index, null)) {
            index++;
        }
        return baseName + index;
    }

    private boolean fieldNameExists(String fieldName, BmsField currentField) {
        for (BmsField f : fields) {
            if (!f.equals(currentField) && f.getName().equalsIgnoreCase(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidFieldName(String name) {
        return name.matches("[A-Za-z@#$][A-Za-z0-9@#$-_]{0,29}");
    }
    // =====================================================
    // Palette
    // =====================================================

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

        fieldPalette.getChildren()
                .addAll(label, inputBtn, outputBtn, inoutBtn);

        root.setLeft(fieldPalette);
    }

    // =====================================================
    // Helpers
    // =====================================================
    private void drawGrid() {

        // Dark phosphor background
        gc.setFill(Color.web("#0C0C0C"));
        gc.fillRect(0, 0,
                canvas.getWidth(),
                canvas.getHeight());

        // Subtle grid
        gc.setStroke(Color.web("#003300"));

        for (int r = 0; r <= rows; r++)
            gc.strokeLine(0, r * CELL_HEIGHT,
                    cols * CELL_WIDTH,
                    r * CELL_HEIGHT);

        for (int c = 0; c <= cols; c++)
            gc.strokeLine(c * CELL_WIDTH, 0,
                    c * CELL_WIDTH,
                    rows * CELL_HEIGHT);
    }

    private void redrawFields() {
        drawGrid();

        gc.setFont(javafx.scene.text.Font.font(
                "Monospaced",
                javafx.scene.text.FontWeight.BOLD,
                14));

        for (BmsField f : fields) {

            int x = (f.getCol() - 1) * CELL_WIDTH;
            int y = (f.getRow() - 1) * CELL_HEIGHT;
            int w = f.getLength() * CELL_WIDTH;

            gc.setFill(toFxColor(f.getBgColor()));  
            gc.fillRect(x, y, w, CELL_HEIGHT);  

            gc.setFill(toFxColor(f.getColor()));  
            gc.fillText(f.getInitialValue(), x + 2, y + CELL_HEIGHT - 5);  

            gc.setFill(Color.RED);  
            gc.fillRect(x + w - 4, y, 4, CELL_HEIGHT);  

        }
    }

    private boolean overlaps(BmsField field) {

        for (BmsField f : fields) {

            if (f == field) continue;

            if (f.getRow() == field.getRow()) {

                int start1 = f.getCol();
                int end1 = f.getCol() + f.getLength() - 1;

                int start2 = field.getCol();
                int end2 = field.getCol() + field.getLength() - 1;

                if (start1 <= end2 && start2 <= end1)
                    return true;
            }
        }
        return false;
    }

    private BmsField cloneField(BmsField f) {

        BmsField copy = new BmsField();

        copy.setName(copy.getName() + "_1");
        copy.setFieldType(f.getFieldType());
        copy.setLength(f.getLength());
        copy.setColor(f.getColor());
        copy.setProtection(f.getProtection());
        copy.setIntensity(f.getIntensity());
        copy.setInitialValue(f.getInitialValue());
        copy.setRow(f.getRow());
        copy.setCol(f.getCol());

        return copy;
    }

    private boolean isValidMapName(String name) {

        if (name == null) return false;

        name = name.trim().toUpperCase();

        if (!name.matches("^[A-Z@#$][A-Z0-9@#$]{0,6}$"))
            return false;

        return true;
    }

    private void handleKeyPress(KeyEvent e) {

        if (selectedField == null) return;
        System.out.print("null");

        switch (e.getCode()) {

            case DELETE:
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
                if (e.isControlDown() && selectedField != null)
                    clipboardField = cloneField(selectedField);
                break;

            case V:
                if (e.isControlDown() && clipboardField != null) {
                    BmsField copy = cloneField(clipboardField);
                    copy.setCol(copy.getCol() + 1);
                    fields.add(copy);
                    redrawFields();
                }
                break;
            default:
                return;
        }

        if (!overlaps(selectedField))
            redrawFields();
    }

    private void setupControls() {
        Button exportButton = new Button("Generate BMS & Copybook");
        exportButton.getStyleClass().add("primary-button"); // Optional
        exportButton.setOnAction(e -> {
            exportBms();
        });

        ToggleButton snapToggle = new ToggleButton("Snap To Grid");
        snapToggle.setSelected(true);
        snapToggle.selectedProperty().addListener((obs, oldV, newV) -> snapEnabled = newV);

        HBox controls = new HBox(10, exportButton, snapToggle);
        controls.setPadding(new Insets(10));
        root.setBottom(controls);
    }

    // private void drawHorizontalRuler(GraphicsContext gc) {
    //     gc.setFill(Color.web("#333"));
    //     gc.fillRect(0, 0, cols * CELL_WIDTH, 20);  // Background for ruler
    //     gc.setFill(Color.LIGHTGRAY);
    //     gc.setFont(javafx.scene.text.Font.font("Monospaced", 10));

    //     for (int c = 1; c <= cols; c++) {
    //         if (c % 5 == 0 || c == 1) { // Show every 5th column
    //             String text = String.valueOf(c);
    //             gc.fillText(text, (c - 1) * CELL_WIDTH + 2, 15);  // Column numbers
    //         }
    //     }
    // }

    // private void drawVerticalRuler(GraphicsContext gc) {
    //     gc.setFill(Color.web("#333"));
    //     gc.fillRect(0, 0, 40, rows * CELL_HEIGHT);  // Background for ruler
    //     gc.setFill(Color.LIGHTGRAY);
    //     gc.setFont(javafx.scene.text.Font.font("Monospaced", 10));

    //     for (int r = 1; r <= rows; r++) {
    //         if (r % 2 == 0 || r == 1) { // Show every 2nd row
    //             String text = String.format("%02d", r);
    //             gc.fillText(text, 2, r * CELL_HEIGHT - 5);  // Row numbers
    //         }
    //     }
    // }

    private void setupMenu() {

        MenuBar menuBar = new MenuBar();

        Menu settingsMenu = new Menu("Settings");

        MenuItem modifyGrid = new MenuItem("Modify Rows/Columns");
        modifyGrid.setOnAction(e -> showGridSizeDialog());

        settingsMenu.getItems().add(modifyGrid);

        Menu aboutMenu = new Menu("About");
        MenuItem aboutItem = new MenuItem("About BMS Map Editor");
        aboutItem.setOnAction(e -> showAboutDialog());

        aboutMenu.getItems().add(aboutItem);

        // Add menus to menu bar
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
                        resetCanvas();  // Resize and reset the canvas
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Invalid grid size").showAndWait();
                    }
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR, "Invalid grid format").showAndWait();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid format. Use 'rows x cols' (e.g., 24x80)").showAndWait();
            }
        });
    }

    private void resetCanvas() {
        canvas.setWidth(cols * CELL_WIDTH);
        canvas.setHeight(rows * CELL_HEIGHT);
        drawGrid();  
        redrawFields();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About BMS Map Editor");
        alert.setHeaderText("BMS Map Editor v1.0");
        alert.setContentText("This is a tool to design BMS maps, " +
                             "used for creating terminal interface layouts in COBOL.");

        alert.showAndWait();
    }

    private void exportBms() {

        if (fields.isEmpty()) {
            showWarning("No fields to export!");
            return;
        }

        Optional<ExportConfig> configOpt = showExportConfigDialog();
        if (configOpt.isEmpty()) return;

        ExportConfig config = configOpt.get();

        if (!isValidMapName(config.mapName())) {
            showError("Invalid map name!");
            return;
        }

        String bmsText = BmsGenerator.generate(
                config.mapName(),
                rows,
                cols,
                fields,
                config.tioapfx(),
                config.ctrl(),
                config.line(),
                config.column(),
                config.includePreview()
        );

        String copyBookText = BmsGenerator.generateCopybook(
                config.mapName(),
                fields,
                true
        );

        showResultDialog(config.mapName(), bmsText, copyBookText);
    }

    private Color toFxColor(BmsField.BmsColor c) {
        switch (c) {
            case RED: return Color.RED;
            case GREEN: return Color.LIME;
            case BLUE: return Color.BLUE;
            case YELLOW: return Color.YELLOW;
            case TURQUOISE: return Color.CYAN;
            case PINK: return Color.HOTPINK;
            case NEUTRAL:
            default: return Color.WHITE;
        }
    }

    private record ExportConfig(
            String mapName,
            String tioapfx,
            String ctrl,
            String line,
            String column,
            boolean includePreview
    ) {}

    private Optional<ExportConfig> showExportConfigDialog() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("BMS Export Configuration");
        dialog.setHeaderText("Enter Map Export Details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField mapNameField = new TextField("MYMAP");
        TextField tioapfxField = new TextField("YES");
        TextField ctrlField = new TextField("FREEKB,FRSET");
        TextField lineField = new TextField("1");
        TextField columnField = new TextField("1");

        CheckBox previewCheckBox = new CheckBox("Include Preview Section");
        previewCheckBox.setSelected(true);

        grid.addRow(0, new Label("Map Name:"), mapNameField);
        grid.addRow(1, new Label("TIOAPFX:"), tioapfxField);
        grid.addRow(2, new Label("CTRL:"), ctrlField);
        grid.addRow(3, new Label("LINE:"), lineField);
        grid.addRow(4, new Label("COLUMN:"), columnField);
        grid.addRow(5, previewCheckBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        return dialog.showAndWait().filter(b -> b == ButtonType.OK)
                .map(b -> new ExportConfig(
                        mapNameField.getText().trim(),
                        tioapfxField.getText().trim().toUpperCase(),
                        ctrlField.getText().trim(),
                        lineField.getText().trim(),
                        columnField.getText().trim(),
                        previewCheckBox.isSelected()
                ));
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