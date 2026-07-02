package com.openbms.ui.dialogs;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.stage.*;
import com.openbms.model.ExportConfig;
import com.openbms.service.ValidationService;

public class ExportDialog {

    public static ExportConfig showExportDialog(Window owner) {

        ValidationService validationService = new ValidationService();

        Dialog<ExportConfig> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Export Map");
        dialog.setHeaderText("Enter Export Details");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));

        // Map Name auto uppercase
        TextField mapNameField = new TextField("MYMAP");
        forceUppercase(mapNameField);

        // TIOAPFX Dropdown
        ComboBox<String> tioapfxBox = new ComboBox<>();
        tioapfxBox.getItems().addAll("YES", "NO");
        tioapfxBox.setValue("YES");

        // CTRL Dropdown (editable)
        ComboBox<String> ctrlBox = new ComboBox<>();
        ctrlBox.getItems().addAll(
                "FREEKB,FRSET",
                "FREEKB",
                "FRSET"
        );
        ctrlBox.setValue("FREEKB,FRSET");
        ctrlBox.setEditable(true);
        forceUppercase(ctrlBox.getEditor());

        // LINE & COLUMN (numbers only-ish)
        TextField lineField = new TextField("1");
        TextField columnField = new TextField("1");

        // Preview checkbox
        CheckBox previewCheckBox = new CheckBox("Include Preview Section");

        // Layout
        grid.addRow(0, new Label("Map Name:"), mapNameField);
        grid.addRow(1, new Label("TIOAPFX:"), tioapfxBox);
        grid.addRow(2, new Label("CTRL:"), ctrlBox);
        grid.addRow(3, new Label("LINE:"), lineField);
        grid.addRow(4, new Label("COLUMN:"), columnField);
        grid.addRow(5, previewCheckBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String mapName = mapNameField.getText().trim();
            if (!validationService.isValidMapName(mapName)) {
                showError("Invalid map name. Use 1-7 letters/digits, starting with a letter (e.g. MYMAP).");
                event.consume();
                return;
            }

            try {
                Integer.parseInt(lineField.getText().trim());
                Integer.parseInt(columnField.getText().trim());
            } catch (NumberFormatException ex) {
                showError("LINE and COLUMN must be numeric.");
                event.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new ExportConfig(
                        mapNameField.getText().trim().toUpperCase(),
                        tioapfxBox.getValue(),
                        ctrlBox.getEditor().getText().trim(),
                        lineField.getText().trim(),
                        columnField.getText().trim(),
                        previewCheckBox.isSelected()
                );
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private static void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    // Utility to force uppercase input
    private static void forceUppercase(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(newVal.toUpperCase())) {
                field.setText(newVal.toUpperCase());
            }
        });
    }
}