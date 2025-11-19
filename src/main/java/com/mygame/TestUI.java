package com.mygame;

import com.jme3.app.SimpleApplication;
import io.tlf.jme.jfx.JavaFxUI;

// ALL JavaFX-related imports are now contained here
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

/**
 * manages all JavaFX UI elements and integration for the game.
 */
public class TestUI {

    private final Main app;
    private int clickCount = 0;
    private Label statusLabel;
    
    public TestUI(Main app) {
        this.app = app;
    }

    /**
     * initializes the JavaFX UI system and sets up the test UI HUD.
     */
    public void initializeUI() {
        // initialize the JavaFX UI system for jMonkeyEngine (MUST be done once)
        JavaFxUI.initialize(app);
        
        // create the JavaFX controls in the JavaFX Thread
        // **all JavaFX setup MUST be run on the JavaFX thread!**
        JavaFxUI.getInstance().runInJavaFxThread(() -> {
            
            // create a main container (VBox) for the controls
            VBox controlsBox = new VBox(10); // 10px spacing
            controlsBox.setAlignment(Pos.CENTER);
            controlsBox.setLayoutX(10); // position it cleanly
            controlsBox.setLayoutY(10); 
            controlsBox.setPrefSize(200, 150);
            
            // apply some basic styling for visibility
            controlsBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-padding: 15; -fx-border-color: yellow; -fx-border-width: 2;");

            // create a Label to show the status
            statusLabel = new Label("JME-JFX UI Loaded.");
            statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            statusLabel.setTextFill(Color.YELLOW); // changed color for visibility
            
            // create a Button
            Button testButton = new Button("Raise Player");
            
            // set the buttons action listener
            testButton.setOnAction(event -> {
                clickCount++;
                // update the label text in the JavaFX thread
                statusLabel.setText("Player raised " + clickCount + " times!");
                
                // example of running a task back in the JME thread
                JavaFxUI.getInstance().runInJmeThread(() -> {
                    // access the game object via the Main app reference
                    app.getPlayerNode().move(0, 1.0f, 0); 
                    System.out.println("Player raised by 1.0f on JME thread.");
                });
            });
            
            // button to show a dialog
            Button dialogButton = new Button("Show Dialog");
            dialogButton.setOnAction(event -> {
                showSimpleDialog();
            });


            // add the controls to the container
            controlsBox.getChildren().addAll(statusLabel, testButton, dialogButton);

            // attach the main container to the JavaFX scene
            // the container is given an ID so we can refer to it later for removal/updates
            controlsBox.setId("mainHudContainer"); 
            JavaFxUI.getInstance().attachChild(controlsBox);
        });
    }

    /**
     * Demonstrates using the dialog feature.
     */
    private void showSimpleDialog() {
        // create a simple dialog box content
        VBox dialogContent = new VBox(10);
        dialogContent.setAlignment(Pos.CENTER);
        dialogContent.setStyle("-fx-background-color: #222; -fx-padding: 30; -fx-border-color: red;");
        
        Label dialogLabel = new Label("This is a centered game dialog.");
        dialogLabel.setTextFill(Color.WHITE);
        
        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> {
            JavaFxUI.getInstance().removeDialog();
        });
        
        dialogContent.getChildren().addAll(dialogLabel, closeButton);

        // show the dialog centered on screen with a dimmed background
        JavaFxUI.getInstance().showDialog(dialogContent);
    }
    
    /**
     * cleans up the JavaFX environment when the application shuts down
     */
    public void cleanup() {
        // use JavaFX thread to ensure safe removal, though often not strictly necessary 
        // if the JME app is shutting down, its good practice
        JavaFxUI.getInstance().runInJavaFxThread(() -> {
            // Detach the primary UI element explicitly
            JavaFxUI.getInstance().detachChild("mainHudContainer"); 
            System.out.println("Game UI cleaned up.");
        });
        // App.destroy() or process exit to handle the final JavaFX shutdown
    }
}