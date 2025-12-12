package com.mygame;

import com.jme3.app.Application;
import com.jme3.font.BitmapText;
import com.jme3.font.BitmapFont;
import com.jme3.scene.Node;

public class Timer {

    private final Application app;
    private final Node guiNode;

    private BitmapText timerText;
    private float timeSeconds = 0f;
    private int timeInt = 0;

    public boolean exitedDoor = false;     // Trigger to stop timer
    private boolean hidden = false;

    public Timer(Application app, Node guiNode) {
        this.app = app;
        this.guiNode = guiNode;
        init();
    }

    private void init() {
        //For the Timer, initializing the text 
        BitmapFont guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        timerText = new BitmapText(guiFont);
        timerText.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        timerText.setLocalTranslation(500, timerText.getLineHeight(), 0);

        guiNode.attachChild(timerText);
    }

    public void update(float tpf) {

        // If player exited the door → stop updating + hide
        if (exitedDoor) {

            if (!hidden) {
                guiNode.detachChild(timerText);  // Hide once
                hidden = true;
            }

            return;   // Stop the timer
        }

        // Normal timer 
        timeSeconds += tpf;

        if (timeSeconds >= 1f) {
            timeInt++;
            timerText.setText("Time: " + timeInt);
            timeSeconds = 0f;
        }
    }

    public int getTime() {
        return timeInt;
    }
}
