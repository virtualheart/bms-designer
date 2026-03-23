package com.openbms.ui.dialogs;

import javafx.scene.control.Alert;

public class AboutDialog {

    public static void show() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About BMS Map Editor");
        alert.setHeaderText("BMS Map Editor v2.0");
        alert.setContentText(
                "Tool for designing BMS maps for COBOL CICS."
        );

        alert.showAndWait();
    }
}