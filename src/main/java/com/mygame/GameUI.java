package com.mygame;

import com.jme3.app.SimpleApplication;
import io.tlf.jme.jfx.JavaFxUI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.Label;

/**
 * manages the game UI.
 * features a left-aligned vertically distributed menu
 */
public class GameUI {

    private final Main app;
    private StackPane rootPane;
    
    // ID, track the root node for cleanup
    private static final String ROOT_UI_ID = "GameUIRoot"; 

    public GameUI(Main app) {
        this.app = app;
    }

    public void initializeUI() {
        JavaFxUI.initialize(app);
        JavaFxUI.getInstance().runInJavaFxThread(this::createMainMenu);
    }

    private void createMainMenu() {
        // root Pane Setup
        rootPane = new StackPane();
        rootPane.setId(ROOT_UI_ID);

        // background Setup (GIF, Scaled and Cropped)
        try {
            // file is at src/main/resources/Interface/menu_bg.gif currently it doesnt exist
            String bgPath = getClass().getResource("/Interface/menu_bg.gif").toExternalForm();
            Image bgImage = new Image(bgPath);

            BackgroundImage background = new BackgroundImage(
                bgImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1.0, 1.0, true, true, false, true) // The last 'true' is for 'cover'
            );
            rootPane.setBackground(new Background(background));
        } catch (Exception e) {
            System.err.println("Could not load background GIF. Ensure /src/main/resources/Interface/menu_bg.gif exists.");
            rootPane.setStyle("-fx-background-color: black;"); // fallback
        }

        // layout Container (VBox)
        VBox menuColumn = new VBox();
        menuColumn.setAlignment(Pos.CENTER_LEFT); // Align content to the left
        // bind the VBox height to the root height so it stretches top-to-bottom
        menuColumn.prefHeightProperty().bind(rootPane.heightProperty());
        // add some padding on the left so buttons aren't glued to the screen edge
        menuColumn.setStyle("-fx-padding: 0 0 0 50;");
        // create buttons
        Button btnNewGame = createStyledButton("NEW GAME");
        Button btnLoadGame = createStyledButton("LOAD GAME");
        Button btnSettings = createStyledButton("SETTINGS");
        Button btnQuit = createStyledButton("QUIT");

        // setup button actions
        btnNewGame.setOnAction(e -> startGame());
        btnLoadGame.setOnAction(e -> System.out.println("Load Game clicked"));
        btnSettings.setOnAction(e -> System.out.println("Settings clicked"));
        btnQuit.setOnAction(e -> app.stop());

        // add Buttons with Spacers (To distribute height equally)
        // add a "spacer" region between every item -> height=0 to fill space, height>0 to give specified height
        menuColumn.getChildren().addAll(
            createSpacer(10, "#ff00ff"),
            btnNewGame,
            createSpacer(10, "#ff00ff"),
            btnLoadGame,
            createSpacer(10, "#ff00ff"),
            btnSettings,
            createSpacer(10, "#ff00ff"),
            btnQuit,
            createSpacer(10, "#ff00ff")
        );

        // final assembly
        rootPane.getChildren().add(menuColumn);
        JavaFxUI.getInstance().attachChild(rootPane);
    }

    /**
     * creates a region that acts as a flexible spacer in a VBox
     */
    private Region createSpacer(double height, String color) {
        Region spacer = new Region();
        // CornerRadii makes the corners of the region rounded, not sure what Insets does
        BackgroundFill regionFill = new BackgroundFill(Color.valueOf(color), new CornerRadii(0), new Insets(0));
        Background background = new Background(regionFill);
        spacer.setBackground(background);
        // a height of 0 makes the spacer automatically fill up the menu, otherwise it takes a specified height
        if (height > 0) {
            spacer.setMinHeight(height);
            spacer.setPrefHeight(height);
            spacer.setMaxHeight(height);
        }
        else {
            VBox.setVgrow(spacer, Priority.ALWAYS); // this takes priority over giving the spacers a specified height
        }
        return spacer;
    }

    /**
     * factory method to create buttons with the specific style
     */
    private Button createStyledButton(String text) {
        Button btn = new Button(text.toUpperCase());
        
        // sizing
        btn.setPrefWidth(250); // fixed width for uniformity
        btn.setPrefHeight(60); // fixed height for clickability
        
        // font
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        
        // CSS Styling
        // - light Gray Transparent Background: rgba(200, 200, 200, 0.5)
        // - no corner rounding: -fx-background-radius: 0
        // - centered Text (default in JavaFX, but reinforced here)
        String styleNormal = 
            "-fx-background-color: rgba(200, 200, 200, 0.3);" + 
            "-fx-text-fill: white;" +
            "-fx-background-radius: 0;" +
            "-fx-border-color: rgba(255, 255, 255, 0.5);" + // slight border for visibility
            "-fx-border-width: 1;" +
            "-fx-cursor: hand;";
            
        String styleHover = 
            "-fx-background-color: rgba(255, 255, 255, 0.6);" + // brighter on hover
            "-fx-text-fill: black;" +
            "-fx-background-radius: 0;" +
            "-fx-border-color: white;" +
            "-fx-cursor: hand;";

        btn.setStyle(styleNormal);

        // hover Effects
        btn.setOnMouseEntered(e -> btn.setStyle(styleHover));
        btn.setOnMouseExited(e -> btn.setStyle(styleNormal));

        return btn;
    }

    private void startGame() {
        // hide the menu
        rootPane.setVisible(false);
        // give focus back to JME (optional, usually happens automatically on click)
        JavaFxUI.getInstance().runInJmeThread(() -> {
            app.getInputManager().setCursorVisible(false); // hide mouse for FPS camera
        });
    }

    public void cleanup() {
        JavaFxUI.getInstance().runInJavaFxThread(() -> {
            JavaFxUI.getInstance().detachChild(ROOT_UI_ID); 
        });
    }
}