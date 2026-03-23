package com.openbms.ui.dialogs;

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

    public FieldDialog(FieldService fieldService, List<BmsField> fields) {
        this.fieldService = fieldService;
        this.fields = fields;
    }

    /** Shows a dialog to add or edit a field */
    public Optional<BmsField> showDialog(BmsField field, int row, int col) {

        boolean isNew = (field == null);
        BmsField f = isNew ? new BmsField() : field;

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

                // Assign values
                fFinal.setName(fieldName);
                fFinal.setLength(len);
                fFinal.setFieldType(typeBox.getValue());
                fFinal.setColor(colorBox.getValue());
                fFinal.setBgColor(bgColorBox.getValue());
                fFinal.setProtection(protectionBox.getValue());
                fFinal.setIntensity(intensityBox.getValue());
                fFinal.setInitialValue(initVal);
                fFinal.setRow(row);
                fFinal.setCol(col);

                if (isNew) fields.add(fFinal);

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

    private String generateAutoName() {
        int index = 1;
        String base = "FIELD";

        while (true) {
            final int current = index;
            if (fields.stream().noneMatch(f -> f.getName().equalsIgnoreCase(base + current))) {
                return base + current;
            }
            index++;
        }
    }
}