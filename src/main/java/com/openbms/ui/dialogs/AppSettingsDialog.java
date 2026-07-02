package com.openbms.ui.dialogs;

import com.openbms.model.AppSettings;
import com.openbms.model.BmsField;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * Lets the user configure designer-wide preferences: the color/intensity new
 * fields start with, how many grid cells a drag snaps by, and which color
 * palette the CICS emulator renders with. Changes apply immediately (the
 * dialog writes straight into the shared AppSettings instance) rather than
 * requiring an explicit "Apply" step, since none of these settings are
 * destructive or hard to change back.
 */
public class AppSettingsDialog {

    public static void show(Window owner, AppSettings settings) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Designer Settings");
        dialog.setHeaderText("Defaults & Preferences");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));

        ChoiceBox<BmsField.BmsColor> defaultColorBox = new ChoiceBox<>();
        defaultColorBox.getItems().addAll(BmsField.BmsColor.values());
        defaultColorBox.setValue(settings.getDefaultColor());
        defaultColorBox.valueProperty().addListener((obs, oldVal, newVal) -> settings.setDefaultColor(newVal));

        ChoiceBox<BmsField.Intensity> defaultIntensityBox = new ChoiceBox<>();
        defaultIntensityBox.getItems().addAll(BmsField.Intensity.values());
        defaultIntensityBox.setValue(settings.getDefaultIntensity());
        defaultIntensityBox.valueProperty().addListener((obs, oldVal, newVal) -> settings.setDefaultIntensity(newVal));

        Spinner<Integer> snapSizeSpinner = new Spinner<>(1, 10, settings.getSnapSize());
        snapSizeSpinner.setEditable(true);
        snapSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> settings.setSnapSize(newVal));

        ChoiceBox<AppSettings.EmulatorPalette> paletteBox = new ChoiceBox<>();
        paletteBox.getItems().addAll(AppSettings.EmulatorPalette.values());
        paletteBox.setValue(settings.getEmulatorPalette());
        paletteBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(AppSettings.EmulatorPalette p) {
                if (p == null) return "";
                return switch (p) {
                    case GREEN_ON_BLACK -> "Green on Black (classic 3270)";
                    case AMBER_ON_BLACK -> "Amber on Black";
                    case WHITE_ON_BLACK -> "White on Black";
                    case DARK_ON_LIGHT -> "Dark on Light";
                };
            }

            @Override
            public AppSettings.EmulatorPalette fromString(String s) {
                return settings.getEmulatorPalette();
            }
        });
        paletteBox.valueProperty().addListener((obs, oldVal, newVal) -> settings.setEmulatorPalette(newVal));

        grid.addRow(0, new Label("Default field color:"), defaultColorBox);
        grid.addRow(1, new Label("Default intensity:"), defaultIntensityBox);
        grid.addRow(2, new Label("Snap size (cells):"), snapSizeSpinner);
        grid.addRow(3, new Label("Emulator palette:"), paletteBox);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }
}
