package com.mygame;

import com.jme3.app.SimpleApplication;
import io.tlf.jme.jfx.JavaFxUI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import com.mygame.Main;

/**
 * manages the game UI.
 * features a main menu and a settings screen.
 */
public class GameUI {

    private final Main app;
    private StackPane rootPane;
    private VBox mainMenuPane;
    private VBox settingsPane;

    // ID, track the root node for cleanup
    private static final String ROOT_UI_ID = "GameUIRoot";

    public GameUI(Main app) {
        this.app = app;
    }

    public void initializeUI() {
        JavaFxUI.initialize(app);
        // Run the UI construction on the JavaFX thread
        JavaFxUI.getInstance().runInJavaFxThread(this::buildUI);
    }

    /**
     * Builds the root UI pane and initializes all sub-menus.
     */ // new
    private void buildUI() {
        // root Pane Setup
        rootPane = new StackPane();
        rootPane.setId(ROOT_UI_ID);

        // background Setup (GIF, Scaled and Cropped)
        try {
            // file is at src/main/resources/Interface/menu_bg.gif
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

        // Create the different menu panes
        mainMenuPane = createMainMenuPane();
        settingsPane = createSettingsPane();

        // Add both panes to the root, but hide the settings pane initially
        settingsPane.setVisible(false);
        rootPane.getChildren().addAll(mainMenuPane, settingsPane);

        // Attach the root pane to the JME UI
        JavaFxUI.getInstance().attachChild(rootPane);
    }

    /**
     * Creates the VBox containing the main menu buttons.
     * @return A VBox configured as the main menu.
     */
    private VBox createMainMenuPane() {
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
        btnSettings.setOnAction(e -> showSettingsScreen(true)); // Show settings 
        btnQuit.setOnAction(e -> app.stop());

        // add Buttons with Spacers (To distribute height equally)
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
        return menuColumn; // new
    }

    /**
     * Creates the VBox containing the settings screen controls.
     * @return A VBox configured as the settings menu.
     */
    private VBox createSettingsPane() { // new
        VBox settingsColumn = new VBox(); // new
        settingsColumn.setAlignment(Pos.CENTER); // Center-align settings
        settingsColumn.setSpacing(25); // Add spacing between elements
        settingsColumn.prefHeightProperty().bind(rootPane.heightProperty());

        // Add a semi-transparent background to dim the main background
        settingsColumn.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");

        // Settings Title
        Label title = new Label("SETTINGS");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        title.setTextFill(Color.WHITE);

        // Volume
        Label volumeLabel = new Label("Master Volume:");
        volumeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        volumeLabel.setTextFill(Color.WHITE);

        Slider volumeSlider = new Slider(0, 100, 75); // min, max, default
        volumeSlider.setMaxWidth(300);

        // Fullscreen
        CheckBox fullscreenCheck = new CheckBox("Enable Fullscreen");
        fullscreenCheck.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        fullscreenCheck.setStyle("-fx-text-fill: white;"); // CheckBox text color needs CSS 
        fullscreenCheck.setOnAction(e -> System.out.println("Fullscreen toggled: " + fullscreenCheck.isSelected()));

        // Back Button
        Button btnBack = createStyledButton("BACK");
        btnBack.setOnAction(e -> showSettingsScreen(false)); // Hide settings
        
        // Apply Button
        Button btnApply = createStyledButton("APPLY");
        btnApply.setOnAction(e -> toggleToFullscreen());

        settingsColumn.getChildren().addAll(title, volumeLabel, volumeSlider, fullscreenCheck, btnBack);
        return settingsColumn; 
    } 

    /**
     * Toggles between the main menu and the settings screen.
     * @param show true to show settings and hide main menu, false for a vice-versa. 
     */ 
    private void showSettingsScreen(boolean show) {
        if (mainMenuPane != null) { 
            mainMenuPane.setVisible(!show); 
        }
        if (settingsPane != null) {
            settingsPane.setVisible(show); 
        }
    }
        public void toggleToFullscreen(Main app) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode[] modes = device.getDisplayModes();
        int i=0; // note: there are usually several, let's pick the first
        settings.setResolution(modes[i].getWidth(),modes[i].getHeight());
        settings.setFrequency(modes[i].getRefreshRate());
        settings.setBitsPerPixel(modes[i].getBitDepth());
        settings.setFullscreen(device.isFullScreenSupported());
        app.setSettings(settings);
        app.restart(); // restart the context to apply changes
    }

    /**
     * creates a region that acts as a flexible spacer in a VBox
     */
    private Region createSpacer(double height, String color) {
        Region spacer = new Region();
        // CornerRadii makes the corners of the region rounded, Insets defines padding
        BackgroundFill regionFill = new BackgroundFill(Color.valueOf(color), new CornerRadii(0), new Insets(0));
        Background background = new Background(regionFill);
        spacer.setBackground(background);
        // a height of 0 makes the spacer automatically fill up the menu, otherwise it takes a specified height
        if (height > 0) {
            spacer.setMinHeight(height);
            spacer.setPrefHeight(height);
            spacer.setMaxHeight(height);
        } else {
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
        // hide the entire menu UI // new
        if (rootPane != null) { // new
            rootPane.setVisible(false);
        } // new
        // give focus back to JME // new
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