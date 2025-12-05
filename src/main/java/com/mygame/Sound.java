package com.mygame; //error check --> error caused in Main.javas

import com.jme3.app.Application;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData.DataType;
import com.jme3.font.BitmapText;
import com.jme3.font.BitmapFont;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.Node;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;

public class Sound {

    private Application app;           // Reference to the main app
    private Node rootNode, guiNode;
    private Vector3f penguinPos = new Vector3f(0,0,0);
    private Spatial penguin;
    private AudioNode[] radios;
    private int currentRadio = 0;
    private float maxDistance = 20;
    private float switchDiameter = 5;

    private BitmapText helloText;
    private boolean textAttached = false;

    public Sound(Application app, Node rootNode, Node guiNode) {
        this.app = app;
        this.rootNode = rootNode;
        this.guiNode = guiNode;

        init();
    }

    private void init() {
        // Load penguin model
        penguin = app.getAssetManager().loadModel("Models/Office1/radioModel.glb");
        penguin.setLocalTranslation(penguinPos);
        rootNode.attachChild(penguin);
        penguin.setLocalTranslation(5.0f, 3.0f, 1.0f);
        penguin.rotate(0.0f, 2.0f, 0.0f);

        // Load audio
        radios = new AudioNode[]{
            loadRadio("Sounds/radio/mono_radio1.wav"),
            loadRadio("Sounds/radio/mono_radio2.wav"),
            loadRadio("Sounds/radio/mono_radio3.wav"),
            loadRadio("Sounds/radio/mono_radio4.wav")

        };
        playRadio(0);

        // Bind E key
        app.getInputManager().addMapping("SwitchRadio", new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addListener(actionListener, "SwitchRadio");

        // Setup proximity text
        BitmapFont guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        helloText = new BitmapText(guiFont, false);
        helloText.setSize(guiFont.getCharSet().getRenderedSize());
        helloText.setText("E");
    }

    private AudioNode loadRadio(String path) {
        AudioNode node = new AudioNode(app.getAssetManager(), path, DataType.Buffer);
        node.setLooping(true);
        node.setPositional(false);
        node.setVolume(1f);
        rootNode.attachChild(node);
        return node;
    }

    private void playRadio(int index) {
        for (AudioNode r : radios) r.stop();
        radios[index].play();
        currentRadio = index;
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("SwitchRadio") && isPressed) {
                float distance = app.getCamera().getLocation().distance(penguinPos);
                if (distance <= switchDiameter / 2) {
                    playRadio((currentRadio + 1) % radios.length);
                }
            }
        }
    };

    public void update(float tpf) {
        float distance = app.getCamera().getLocation().distance(penguinPos);
        float volume = Math.max(0, 1 - distance / maxDistance);
        radios[currentRadio].setVolume(volume);

        // Show text when close
        if (distance <= switchDiameter / 2) {
            if (!textAttached) {
                helloText.setLocalTranslation(10, app.getContext().getSettings().getHeight() - 10, 0);
                guiNode.attachChild(helloText);
                textAttached = true;
            }
        } else {
            if (textAttached) {
                guiNode.detachChild(helloText);
                textAttached = false;
            }
        }
    }
}
