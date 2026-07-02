package com.openbms.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;

import java.util.function.Supplier;

import com.openbms.model.AppSettings;
import com.openbms.ui.dialogs.AboutDialog;
import com.openbms.ui.dialogs.AppSettingsDialog;
import com.openbms.ui.dialogs.GridSizeDialog;

/**
 * Builds and owns the application MenuBar. Grid-size changes and import
 * requests are reported back to the caller (MainView) via callbacks rather
 * than this class reaching into MainView's internals directly.
 *
 * The owner window is resolved lazily via a Supplier because MainView
 * builds its menu before the Scene/Window exist (Main.java attaches the
 * Scene only after MainView's constructor returns).
 *
 * Replaces the previous MenuController, which built a menu that was never
 * attached to the scene (MainView built its own duplicate inline instead),
 * and whose dialog actions only printed to stdout rather than doing
 * anything.
 */
public class MenuController {

    private final Supplier<Window> ownerSupplier;
    private final AppSettings appSettings;

    private GridSizeDialog.GridSizeListener gridSizeListener = (r, c) -> {};
    private Runnable onImport = () -> {};
    private Runnable onRun = () -> {};

    public MenuController(Supplier<Window> ownerSupplier, AppSettings appSettings) {
        this.ownerSupplier = ownerSupplier != null ? ownerSupplier : () -> null;
        this.appSettings = appSettings;
    }

    public void setOnGridSizeChanged(GridSizeDialog.GridSizeListener listener) {
        this.gridSizeListener = listener != null ? listener : (r, c) -> {};
    }

    public void setOnImport(Runnable onImport) {
        this.onImport = onImport != null ? onImport : () -> {};
    }

    public void setOnRun(Runnable onRun) {
        this.onRun = onRun != null ? onRun : () -> {};
    }

    public MenuBar createMenu(Supplier<int[]> currentGridSize) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem importItem = new MenuItem("Import BMS Map...");
        importItem.setOnAction(e -> onImport.run());
        fileMenu.getItems().add(importItem);

        Menu settingsMenu = new Menu("Settings");
        MenuItem modifyGrid = new MenuItem("Modify Rows/Columns");
        modifyGrid.setOnAction(e -> {
            int[] size = currentGridSize.get();
            GridSizeDialog.showGridSizeDialog(size[0], size[1], ownerSupplier.get(), gridSizeListener);
        });
        settingsMenu.getItems().add(modifyGrid);

        MenuItem preferencesItem = new MenuItem("Designer Settings...");
        preferencesItem.setOnAction(e -> AppSettingsDialog.show(ownerSupplier.get(), appSettings));
        settingsMenu.getItems().add(preferencesItem);

        Menu runMenu = new Menu("Run");
        MenuItem runItem = new MenuItem("Run Screen (CICS Emulator)");
        runItem.setOnAction(e -> onRun.run());
        runMenu.getItems().add(runItem);

        Menu aboutMenu = new Menu("About");
        MenuItem aboutItem = new MenuItem("About BMS Map Editor");
        aboutItem.setOnAction(e -> AboutDialog.show());
        aboutMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, settingsMenu, runMenu, aboutMenu);
        return menuBar;
    }
}
