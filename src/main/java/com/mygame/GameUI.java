package com.mygame;

import java.io.InputStream;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;

import io.tlf.jme.jfx.JavaFxUI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * manages the game UI.
 * features a main menu with a 3D parallax background.
 */
public class GameUI {
    

    private final Main app;
    private StackPane rootPane;
    private VBox mainMenuPane;
    private VBox settingsPane;

    // Parallax Layers
    private ImageView bgLayer;
    private ImageView mgLayer;
    private ImageView fgLayer;

    // ID, track the root node for cleanup
    private static final String ROOT_UI_ID = "GameUIRoot";

    public GameUI(Main app) {
        this.app = app;
    }

    public void initializeUI() {
        JavaFxUI.initialize(app);
        JavaFxUI.getInstance().runInJavaFxThread(this::buildUI);
    }

    private void buildUI() {

        System.out.println("--- DEBUGGING PATHS ---");
        // find the Interface folder or textures folder (this is legacy debugging)
        System.out.println("Root Search: " + getClass().getResource("/"));
        System.out.println("Texture Search: " + getClass().getResource("/Textures/"));
        System.out.println("Direct Search: " + getClass().getResource("/Textures/menu/parallax/background.png"));
        System.out.println("-----------------------");
        
        // 1. Root Pane Setup
        rootPane = new StackPane();
        rootPane.setId(ROOT_UI_ID);

        // force the root pane to fill the entire JME window. fixes the fill problem
        double screenWidth = app.getCamera().getWidth();
        double screenHeight = app.getCamera().getHeight();
        
        rootPane.setPrefSize(screenWidth, screenHeight);
        rootPane.setMinSize(screenWidth, screenHeight);
        rootPane.setMaxSize(screenWidth, screenHeight);

        // initialize parallax layers (order matters for z-indexing)
        bgLayer = createParallaxLayer("Textures/background.png");
        mgLayer = createParallaxLayer("Textures/middleground.png");
        fgLayer = createParallaxLayer("Textures/foreground.png");

        // menu panes
        mainMenuPane = createMainMenuPane();
        settingsPane = createSettingsPane();

        settingsPane.setVisible(false);

        // order: Back -> Middle -> Front -> UI
        rootPane.getChildren().addAll(bgLayer, mgLayer, fgLayer, mainMenuPane, settingsPane);

        setupParallaxEffect();

        JavaFxUI.getInstance().attachChild(rootPane);
    }

    /**
     * creates an ImageView for screen menu
     */
    private ImageView createParallaxLayer(String path) {
        ImageView view = new ImageView();
        try {
            // sanitize path for JME
            String jmePath = path.startsWith("/") ? path.substring(1) : path;

            // attempt to find the files
            AssetKey<Object> key = new AssetKey<>(jmePath);
            AssetInfo info = app.getAssetManager().locateAsset(key);

            if (info != null) {
                // open a stream and pass it to JavaFX
                try (InputStream stream = info.openStream()) {
                    Image img = new Image(stream);
                    view.setImage(img);
                }
            } else {
                System.err.println("JME could not find asset: " + jmePath);
            }

            // bind dimensions
            view.fitWidthProperty().bind(rootPane.widthProperty().multiply(1.1));
            view.fitHeightProperty().bind(rootPane.heightProperty().multiply(1.1));
            view.setPreserveRatio(false); 

            // prevent infinite scaling Loop
            // StackPane tries to grow to fit the child but the child tries to be 1.1x the StackPane
            // tells the StackPane to suck it up and drop this node when calculating its own size
            view.setManaged(false);

            // Since it is unmanaged, the StackPane won't center it automatically.
            // We must manually bind X/Y to keep it centered: (ParentWidth - ImageWidth) / 2
            view.layoutXProperty().bind(rootPane.widthProperty().subtract(view.fitWidthProperty()).divide(2));
            view.layoutYProperty().bind(rootPane.heightProperty().subtract(view.fitHeightProperty()).divide(2));

        } catch (Exception e) {
            System.err.println("Failed to load layer: " + path);
            e.printStackTrace();
        }
        return view;
    }

    /**
     * calculate movement for layer / mouse parallax
     */
    private void setupParallaxEffect() {
        rootPane.setOnMouseMoved(event -> {
            double width = rootPane.getWidth();
            double height = rootPane.getHeight();

            // prevent divide by zero (just in case)
            if (width <= 0 || height <= 0) {
                return;
            }

            double centerX = width / 2.0;
            double centerY = height / 2.0;

            // use getSceneX()
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();

            double offsetX = (mouseX - centerX) / centerX; 
            double offsetY = (mouseY - centerY) / centerY; 

            // clampe values (-1.0 to 1.0)
            offsetX = Math.max(-1.0, Math.min(1.0, offsetX));
            offsetY = Math.max(-1.0, Math.min(1.0, offsetY));

            // move layers
            shiftLayer(bgLayer, offsetX, offsetY, 15); // background
            shiftLayer(mgLayer, offsetX, offsetY, 30); // middle
            shiftLayer(fgLayer, offsetX, offsetY, 60); // foreground
        });
    }

    /**
     * move layer based on offset
     * uses setTranslateX/Y absolute positioning
     */
    private void shiftLayer(ImageView layer, double xFactor, double yFactor, double strength) {
        if (layer != null) {
            // use negative factor to make the layer move opposite to mouse (3D depth effect)
            // setTranslateX (absolute) NOT setTranslateX(getTranslateX()..)
            layer.setTranslateX(-xFactor * strength);
            layer.setTranslateY(-yFactor * strength);
        }
    }

    private VBox createMainMenuPane() {
        VBox menuColumn = new VBox();
        menuColumn.setAlignment(Pos.CENTER_LEFT);
        menuColumn.prefHeightProperty().bind(rootPane.heightProperty());
        menuColumn.setStyle("-fx-padding: 0 0 0 50;");

        Button btnNewGame = createStyledButton("NEW GAME");
        Button btnLoadGame = createStyledButton("LOAD GAME");
        Button btnSettings = createStyledButton("SETTINGS");
        Button btnQuit = createStyledButton("QUIT");

        btnNewGame.setOnAction(e -> startGame());
        btnLoadGame.setOnAction(e -> System.out.println("Load Game clicked"));
        btnSettings.setOnAction(e -> showSettingsScreen(true));
        btnQuit.setOnAction(e -> app.stop());

        menuColumn.getChildren().addAll(
                createSpacer(10, "transparent"),
                btnNewGame,
                createSpacer(10, "transparent"),
                btnLoadGame,
                createSpacer(10, "transparent"),
                btnSettings,
                createSpacer(10, "transparent"),
                btnQuit,
                createSpacer(10, "transparent")
        );
        return menuColumn;
    }

    private VBox createSettingsPane() {
        VBox settingsColumn = new VBox();
        settingsColumn.setAlignment(Pos.CENTER);
        settingsColumn.setSpacing(25);
        settingsColumn.prefHeightProperty().bind(rootPane.heightProperty());

        // darker background for settings legibility
        settingsColumn.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");

        Label title = new Label("SETTINGS");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        title.setTextFill(Color.WHITE);

        Label volumeLabel = new Label("Master Volume:");
        volumeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        volumeLabel.setTextFill(Color.WHITE);

        Slider volumeSlider = new Slider(0, 100, 75);
        volumeSlider.setMaxWidth(300);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Convert slider's 0-100 value to the 0.0-2.0f volume range
            float newVolume = newVal.floatValue() / 50.0f;
            // Call the setAmbientVolume method that now lives in Main.java
            JavaFxUI.getInstance().runInJmeThread(() -> {
                app.setAmbientVolume(newVolume);
            });
        });
        JavaFxUI.getInstance().runInJmeThread(() -> {
            app.setAmbientVolume(75.0f / 50.0f); // 1.5f
        });

        CheckBox fullscreenCheck = new CheckBox("Enable Fullscreen");
        fullscreenCheck.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        fullscreenCheck.setStyle("-fx-text-fill: white;");
        fullscreenCheck.setOnAction(e -> app.toggleFullscreen(fullscreenCheck.isSelected()));

        Button btnBack = createStyledButton("BACK");
        btnBack.setOnAction(e -> showSettingsScreen(false));

        settingsColumn.getChildren().addAll(title, volumeLabel, volumeSlider, fullscreenCheck, btnBack);
        return settingsColumn;
    }

    private void showSettingsScreen(boolean show) {
        if (mainMenuPane != null) {
            mainMenuPane.setVisible(!show);
        }
        if (settingsPane != null) {
            settingsPane.setVisible(show);
        }
    }

    private Region createSpacer(double height, String color) {
        Region spacer = new Region();
        if (!color.equals("transparent")) {
            BackgroundFill regionFill = new BackgroundFill(Color.valueOf(color), new CornerRadii(0), new Insets(0));
            spacer.setBackground(new Background(regionFill));
        }
        
        if (height > 0) {
            spacer.setMinHeight(height);
            spacer.setPrefHeight(height);
            spacer.setMaxHeight(height);
        } else {
            VBox.setVgrow(spacer, Priority.ALWAYS);
        }
        return spacer;
    }

    private Button createStyledButton(String text) {
        Button btn = new Button(text.toUpperCase());
        btn.setPrefWidth(250);
        btn.setPrefHeight(60);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        String styleNormal =
                "-fx-background-color: rgba(200, 200, 200, 0.3);" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.5);" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;";

        String styleHover =
                "-fx-background-color: rgba(255, 255, 255, 0.6);" +
                        "-fx-text-fill: black;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: white;" +
                        "-fx-cursor: hand;";

        btn.setStyle(styleNormal);
        btn.setOnMouseEntered(e -> btn.setStyle(styleHover));
        btn.setOnMouseExited(e -> btn.setStyle(styleNormal));
        return btn;
    }

    private void startGame() {
        if (rootPane != null) {
            rootPane.setVisible(false);
        }
        JavaFxUI.getInstance().runInJmeThread(() -> {
            app.getInputManager().setCursorVisible(false);
        });
    }

    public void cleanup() {
        JavaFxUI.getInstance().runInJavaFxThread(() -> {
            JavaFxUI.getInstance().detachChild(ROOT_UI_ID);
        });
    }
}