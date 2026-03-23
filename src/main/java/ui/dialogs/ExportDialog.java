package ui.dialogs;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.stage.*;
import model.ExportConfig;

public class ExportDialog {

    public static ExportConfig showExportDialog(Window owner) {

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

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new ExportConfig(
                        mapNameField.getText().trim(),
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

    // Utility to force uppercase input
    private static void forceUppercase(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(newVal.toUpperCase())) {
                field.setText(newVal.toUpperCase());
            }
        });
    }
}