package com.openbms.ui;

import javafx.scene.control.*;
import javafx.stage.Window;

public class MenuController {

    private Window parentWindow;

    public MenuController(Window parentWindow) {
        this.parentWindow = parentWindow;
    }

    public MenuBar createMenu() {
        MenuBar menuBar = new MenuBar();

        // Settings menu
        Menu settingsMenu = new Menu("Settings");
        MenuItem modifyGrid = new MenuItem("Modify Rows/Columns");
        modifyGrid.setOnAction(e -> showGridSizeDialog());
        settingsMenu.getItems().add(modifyGrid);

        // About menu
        Menu aboutMenu = new Menu("About");
        MenuItem aboutItem = new MenuItem("About BMS Map Editor");
        aboutItem.setOnAction(e -> showAboutDialog());
        aboutMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(settingsMenu, aboutMenu);
        return menuBar;
    }

    private void showGridSizeDialog() {
        TextInputDialog dialog = new TextInputDialog("24x80");
        dialog.setTitle("Modify Grid Size");
        dialog.setHeaderText("Enter new grid size:");
        dialog.setContentText("Grid size (e.g., 24x80):");
        dialog.showAndWait().ifPresent(input -> {
            // Validate and update grid (call MainView or CanvasController)
            System.out.println("New grid input: " + input);
        });
    }

    private void showAboutDialog() {
        System.out.println("Showing About Dialog");
        // AboutDialog.show();
    }
}