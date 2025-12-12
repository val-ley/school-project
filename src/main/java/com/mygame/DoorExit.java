package com.mygame;

import com.jme3.app.Application;
import com.jme3.font.BitmapText;
import com.jme3.font.BitmapFont;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class DoorExit {

    private final Application app;
    private final Node guiNode;
    private final Vector3f zonePos;
    private final float radius = 3f;

    private BitmapText prompt;
    private boolean promptVisible = false;

    public boolean exitedDoor = false;

    public DoorExit(Application app, Node guiNode, Vector3f pos) {
        this.app = app;
        this.guiNode = guiNode;
        this.zonePos = pos;

        init();
    }

    private void init() {
        // Create prompt text
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        prompt = new BitmapText(font);
        prompt.setText("Press R to Exit");
        prompt.setLocalTranslation(400, 150, 0);

        // Bind key
        app.getInputManager().addMapping("ExitDoor", new KeyTrigger(KeyInput.KEY_R));
        app.getInputManager().addListener(actionListener, "ExitDoor");
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isPressed) return;  // Only on key press
            if (!name.equals("ExitDoor")) return;

            // Distance check
            float distance = app.getCamera().getLocation().distance(zonePos);
            if (distance <= radius) {
                exitedDoor = true;
                hidePrompt();
            }
        }
    };

    public void update(float tpf) {
        if (exitedDoor) {
            hidePrompt();
            return;  // Stop updating
        }

        // Show prompt if player is close
        float distance = app.getCamera().getLocation().distance(zonePos);
        if (distance <= radius) {
            if (!promptVisible) {
                guiNode.attachChild(prompt);
                promptVisible = true;
            }
        } else {
            hidePrompt();
        }
    }
    
    private void endScreen() {
        
        
    }

    private void hidePrompt() {
        if (promptVisible) {
            guiNode.detachChild(prompt);
            promptVisible = false;
        }
    }
}
