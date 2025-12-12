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


// THIS IS FOR THE RADIO AND THE CHAIR CAUSE I DONT WANT TO PUT THE CHAIR SOMEWHERE ELSE

public class Sound {

    private Application app;           // Reference to the main app
    private Node rootNode, guiNode;
    private final Vector3f penguinPos = new Vector3f(5, 3, 2);
    private final Vector3f chairPos = new Vector3f(8, 0, -2);
    private Spatial radioModel;
    private Spatial chairModel;
    private AudioNode[] radios;
    private int currentRadio = 0;
    private final float maxDistance = 20;
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
        // Load radio model
        radioModel = app.getAssetManager().loadModel("Models/radioModel.glb");
        radioModel.setLocalTranslation(penguinPos);
        rootNode.attachChild(radioModel);
        
        // Load chair model
        chairModel = app.getAssetManager().loadModel("Models/chairModel.glb");
        chairModel.setLocalTranslation(chairPos);
        rootNode.attachChild(chairModel);

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