package com.openbms.ui.dialogs;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

import com.openbms.model.AppSettings;
import com.openbms.model.BmsField;
import com.openbms.ui.components.CicsEmulator;

/**
 * "Run Screen" window: hosts an interactive CicsEmulator so the operator can
 * click into unprotected fields, type values, Tab between them, and press
 * Enter to see the captured values — a lightweight stand-in for actually
 * running the map under CICS.
 */
public class RunDialog {

    private static final int CELL_WIDTH = 10;
    private static final int CELL_HEIGHT = 20;

    public static void show(Window owner, List<BmsField> fields, int rows, int cols, AppSettings settings) {

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("Run Screen — CICS Emulator (Demo)");

        Canvas canvas = new Canvas(cols * CELL_WIDTH, rows * CELL_HEIGHT);
        CicsEmulator emulator = new CicsEmulator(canvas, CELL_WIDTH, CELL_HEIGHT);
        if (settings != null) {
            emulator.setPalette(settings.getEmulatorPalette());
        }
        emulator.load(fields, rows, cols);

        // Live-update if the user opens Designer Settings and changes the
        // palette while a Run window is already open, instead of requiring
        // them to close and reopen it to see the new colors. Removed again
        // on close so repeated Run Screen openings don't accumulate stale
        // listeners pointing at already-closed emulator instances.
        javafx.beans.value.ChangeListener<AppSettings.EmulatorPalette> paletteListener = null;
        if (settings != null) {
            paletteListener = (obs, oldVal, newVal) -> {
                emulator.setPalette(newVal);
                emulator.draw();
            };
            settings.emulatorPaletteProperty().addListener(paletteListener);
        }
        final javafx.beans.value.ChangeListener<AppSettings.EmulatorPalette> paletteListenerFinal = paletteListener;

        Label hint = new Label(
                "Click a field to edit it  •  Tab / Shift+Tab to move between fields  •  Enter to submit"
        );
        hint.setStyle("-fx-text-fill: #33FF33; -fx-font-family: monospace;");

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            emulator.reset();
            canvas.requestFocus();
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> stage.close());

        HBox controls = new HBox(10, resetButton, closeButton);
        controls.setPadding(new Insets(8));

        StackPane canvasHolder = new StackPane(canvas);
        canvasHolder.setStyle("-fx-background-color: black; -fx-padding: 10;");

        VBox top = new VBox(6, hint, canvasHolder);
        top.setPadding(new Insets(10));

        emulator.setOnSubmit(() -> showSubmittedValues(stage, emulator));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setBottom(controls);

        Scene scene = new Scene(root, Math.max(640, cols * CELL_WIDTH + 40), rows * CELL_HEIGHT + 160);
        stage.setScene(scene);

        // Blink the cursor every 500ms so the active field is visibly findable.
        Timeline blink = new Timeline(
                new KeyFrame(Duration.millis(500), e -> emulator.tickCursorBlink())
        );
        blink.setCycleCount(Timeline.INDEFINITE);
        blink.play();
        stage.setOnHidden(e -> {
            blink.stop();
            if (settings != null && paletteListenerFinal != null) {
                settings.emulatorPaletteProperty().removeListener(paletteListenerFinal);
            }
        });

        stage.show();
        canvas.requestFocus();
    }

    private static void showSubmittedValues(Window owner, CicsEmulator emulator) {

        Map<String, String> values = emulator.collectValues();

        StringBuilder sb = new StringBuilder();
        if (values.isEmpty()) {
            sb.append("(No input fields on this map.)");
        } else {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                sb.append(entry.getKey()).append(" = \"").append(entry.getValue()).append("\"\n");
            }
        }

        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setPrefRowCount(Math.min(12, values.size() + 2));
        area.setStyle("-fx-font-family: monospace;");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("Submitted Values");
        alert.setHeaderText("Captured field values (simulated AID=ENTER)");
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}
