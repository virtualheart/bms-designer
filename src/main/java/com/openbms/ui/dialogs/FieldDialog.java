package com.openbms.ui.dialogs;

import com.openbms.model.AppSettings;
import com.openbms.model.BmsField;
import com.openbms.service.FieldService;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import java.util.List;
import java.util.Optional;

public class FieldDialog {

    private final FieldService fieldService;
    private final List<BmsField> fields;
    private final int gridRows;
    private final int gridCols;
    private final AppSettings settings;

    public FieldDialog(FieldService fieldService, List<BmsField> fields, int gridRows, int gridCols) {
        this(fieldService, fields, gridRows, gridCols, null);
    }

    public FieldDialog(FieldService fieldService, List<BmsField> fields, int gridRows, int gridCols, AppSettings settings) {
        this.fieldService = fieldService;
        this.fields = fields;
        this.gridRows = gridRows;
        this.gridCols = gridCols;
        this.settings = settings;
    }

    /** Shows a dialog to add a brand new field, defaulting its type. */
    public Optional<BmsField> showDialog(BmsField.FieldType initialType, int row, int col) {
        return showDialog(null, initialType, row, col);
    }

    /** Shows a dialog to edit an existing field. */
    public Optional<BmsField> showDialog(BmsField field, int row, int col) {
        return showDialog(field, null, row, col);
    }

    /**
     * Shows a dialog to add (field == null) or edit (field != null) a field.
     * initialType, when non-null, pre-selects the Type dropdown for a new
     * field (used by the palette's INPUT/OUTPUT/INOUT buttons) without
     * making the caller construct a BmsField itself - constructing one
     * would make this method think it's editing rather than adding, since
     * "is this a new field" is decided purely by field == null.
     */
    private Optional<BmsField> showDialog(BmsField field, BmsField.FieldType initialType, int row, int col) {

        boolean isNew = (field == null);
        BmsField f = isNew ? new BmsField() : field;
        if (isNew && initialType != null) {
            f.setFieldType(initialType);
        }
        if (isNew && settings != null) {
            f.setColor(settings.getDefaultColor());
            f.setIntensity(settings.getDefaultIntensity());
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Add Field" : "Edit Field");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField(f.getName());
        if (isNew) nameField.setText(generateAutoName());

        ChoiceBox<BmsField.FieldType> typeBox = new ChoiceBox<>();
        typeBox.getItems().addAll(BmsField.FieldType.values());
        typeBox.setValue(f.getFieldType());

        Spinner<Integer> rowSpinner = new Spinner<>(1, gridRows, clamp(row, 1, gridRows));
        rowSpinner.setEditable(true);
        Spinner<Integer> colSpinner = new Spinner<>(1, gridCols, clamp(col, 1, gridCols));
        colSpinner.setEditable(true);

        TextField initialField = new TextField(f.getInitialValue());
        TextField lengthField = new TextField(String.valueOf(f.getLength()));

        Label overflowWarning = new Label();
        overflowWarning.setStyle("-fx-text-fill: #b00020;");
        overflowWarning.setWrapText(true);

        Runnable updateOverflowWarning = () -> {
            try {
                int len = Integer.parseInt(lengthField.getText());
                int endCol = colSpinner.getValue() + len - 1;
                overflowWarning.setText(endCol > gridCols
                        ? "Warning: field extends to column " + endCol + ", past the grid width (" + gridCols + ")."
                        : "");
            } catch (NumberFormatException ex) {
                overflowWarning.setText("");
            }
        };

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
        grid.addRow(2, new Label("Row:"), rowSpinner);
        grid.addRow(3, new Label("Column:"), colSpinner);
        grid.addRow(4, new Label("Length:"), lengthField);
        grid.addRow(5, new Label("Text Color:"), colorBox);
        grid.addRow(6, new Label("Background:"), bgColorBox);
        grid.addRow(7, new Label("Protection:"), protectionBox);
        grid.addRow(8, new Label("Intensity:"), intensityBox);
        grid.addRow(9, new Label("Initial:"), initialField);
        grid.add(overflowWarning, 0, 10, 2, 1);

        dialog.getDialogPane().setContent(grid);

        lengthField.textProperty().addListener((obs, oldVal, newVal) -> updateOverflowWarning.run());
        colSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateOverflowWarning.run());
        updateOverflowWarning.run();

        initialField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                int currentLength = Integer.parseInt(lengthField.getText());
                int initialLength = newValue.length();

                if (initialLength > currentLength) {
                    lengthField.setText(String.valueOf(initialLength));
                }

            } catch (NumberFormatException e) {
                lengthField.setText(String.valueOf(newValue.length()));
            }
        });
        
        // Make f effectively final for lambda
        BmsField fFinal = f;

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                String fieldName = nameField.getText().trim().toUpperCase();
                int len = Integer.parseInt(lengthField.getText());
                String initVal = initialField.getText();
                int chosenRow = rowSpinner.getValue();
                int chosenCol = colSpinner.getValue();

                if (len <= 0) {
                    showError("Length must be positive number.");
                    event.consume();
                    return;
                }

                if (!fieldService.isValidFieldName(fieldName)) {
                    showError("Invalid field name.");
                    event.consume();
                    return;
                }

                if (fieldService.fieldNameExists(fieldName, fFinal, fields)) {
                    showError("Field name already exists.");
                    event.consume();
                    return;
                }

                if (initVal.length() > len) {
                    showError("Initial value cannot exceed field length.");
                    event.consume();
                    return;
                }

                if (chosenCol + len - 1 > gridCols) {
                    showError("Field extends past the grid width (column " + gridCols + ").");
                    event.consume();
                    return;
                }

                // Validate against overlap using a probe with the proposed
                // geometry, excluding the field being edited from the check.
                BmsField probe = new BmsField();
                probe.setRow(chosenRow);
                probe.setCol(chosenCol);
                probe.setLength(len);
                List<BmsField> others = fields.stream()
                        .filter(other -> other != fFinal)
                        .toList();
                if (fieldService.overlaps(probe, others)) {
                    showError("Field would overlap another field at this position/length.");
                    event.consume();
                    return;
                }

                // Assign values
                fFinal.setName(fieldName);
                fFinal.setLength(len);
                fFinal.setFieldType(typeBox.getValue());
                fFinal.setColor(colorBox.getValue());
                fFinal.setBgColor(bgColorBox.getValue());
                fFinal.setProtection(protectionBox.getValue());
                fFinal.setIntensity(intensityBox.getValue());
                fFinal.setInitialValue(initVal);
                fFinal.setRow(chosenRow);
                fFinal.setCol(chosenCol);

                // Note: this dialog does NOT add the new field to the shared
                // fields list itself. The caller (CanvasController.addField)
                // is the single place responsible for that, so there is
                // exactly one path that mutates the list and exactly one
                // path that records undo state for "add field".

            } catch (NumberFormatException ex) {
                showError("Length must be a number.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK ? Optional.of(fFinal) : Optional.empty();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String generateAutoName() {
        return fieldService.generateAutoName(fields);
    }
}