package com.openbms;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.openbms.ui.MainView;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        
        MainView view = new MainView();
        
        // Create the scene with MainView and set the window size
        Scene scene = new Scene(view.getRoot(), 1000, 600);  
        stage.setScene(scene); 

        // Set the window title
        stage.setTitle("OpenBMS Designer");
        
        // Set the window icon
        // stage.getIcons().add(new Image("file:src/resources/logo.png"));
        
        // Set the window size behavior
        // stage.setMinWidth(500); // Minimum width of the window
        // stage.setMinHeight(300); // Minimum height of the window
        // stage.setMaximized(true); // Start maximized
        // stage.setFullScreen(true); // Uncomment to start in fullscreen

        // Handle window close request
        stage.setOnCloseRequest(event -> {
            System.out.println("Window is closing");
            // Optional: Save data or confirm before closing
        });
        
        // Show the stage
        stage.show();  
    }

    public static void main(String[] args) {
        launch();  
    }
}