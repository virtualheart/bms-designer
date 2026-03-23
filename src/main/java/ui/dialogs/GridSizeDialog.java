package ui.dialogs;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.stage.Window;

public class GridSizeDialog {

    public static void showGridSizeDialog(int currentRows,
                                          int currentCols,
                                          Window owner,
                                          GridSizeListener listener) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modify Grid Size");
        dialog.setHeaderText("Enter new grid size (rows x columns):");

        // Input field
        TextField sizeField = new TextField(currentRows + "x" + currentCols);
        sizeField.setPromptText("e.g., 24x80");

        VBox content = new VBox(10, new Label("Grid size:"), sizeField);
        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String input = sizeField.getText().trim();
                String[] parts = input.split("x");

                if (parts.length != 2) {
                    showError("Invalid format. Use 'rows x cols' (e.g., 24x80).");
                    return null;
                }

                try {
                    int rows = Integer.parseInt(parts[0].trim());
                    int cols = Integer.parseInt(parts[1].trim());

                    if (rows <= 0 || cols <= 0) {
                        showError("Rows and columns must be positive numbers.");
                        return null;
                    }

                    if (listener != null) {
                        listener.onGridSizeChanged(rows, cols);
                    }

                } catch (NumberFormatException e) {
                    showError("Rows and columns must be numeric.");
                }
            }
            return null;
        });

        dialog.initOwner(owner);
        dialog.showAndWait();
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Listener interface to notify MainView or other caller
    public interface GridSizeListener {
        void onGridSizeChanged(int rows, int cols);
    }
}