package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData.DataType;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/** Sample 11 - playing 3D audio. */
public class Sound extends SceneGraphVisitorAdapter {


  public static void main(String[] args) {
    Sound app = new Sound();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    flyCam.setMoveSpeed(40);

    /** just a blue box floating in space */
    Box box1 = new Box(1, 1, 1);
    Geometry player = new Geometry("Player", box1);
    Material mat1 = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
    mat1.setColor("Color", ColorRGBA.Blue);
    player.setMaterial(mat1);
    rootNode.attachChild(player);

    initAudio();
    
    
    
  }

  /** We create two audio nodes. */
  private void initAudio() {

    /* nature sound - keeps playing in a loop. */
    AudioNode audio_nature = new AudioNode(assetManager, "Sounds/radio1.wav", DataType.Stream);
    audio_nature.setLooping(true);  // activate continuous playing
    audio_nature.setPositional(false);
    audio_nature.setVolume(3);
    rootNode.attachChild(audio_nature);
    audio_nature.play(); // play continuously
  }

  /** Move the listener with the a camera - for 3D audio.
     * @param tpf */
  @Override
  public void simpleUpdate(float tpf) {
    listener.setLocation(cam.getLocation());
    listener.setRotation(cam.getRotation());
  }

}