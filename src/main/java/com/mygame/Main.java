package com.mygame;

import com.jme3.scene.Spatial;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;



/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {

    private BulletAppState bulletAppState;
    private BetterCharacterControl playerControl;
    private Node playerNode;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        
        // Physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        // bulletAppState.setDebugEnable(true); // enables debug mode
        flyCam.setEnabled(true);
        // Scene setup
        cam.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 0.01f, 1000f);
        
        Spatial officeStart = assetManager.loadModel("Scenes/OfficeScene.j3o");
        rootNode.attachChild(officeStart);
        
        // enable physics for room
        CollisionShape officeShape = CollisionShapeFactory.createMeshShape(officeStart);
        RigidBodyControl officePhys = new RigidBodyControl(officeShape, 0); // 0 means static, no mass
        officeStart.addControl(officePhys);
        
        // create player
        playerNode = new Node("Player");
        // radius 1.5, hight 6, weight 1 
        playerControl = new BetterCharacterControl(1.5f, 6f, 1f);
        playerNode.addControl(playerControl);
        
        bulletAppState.getPhysicsSpace().add(playerControl);
        rootNode.attachChild(playerNode);
        
        /* how to attach geometry to scene node
        Box b = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", b);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);

        rootNode.attachChild(geom);
        */
        
        // tp player to room start
        playerControl.warp(new Vector3f(0.1f, 1f, 1.1f));
        
        //setupKeys();
        
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        rootNode.addLight(sun);
        
    }
/*
    public void setupKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        
        inputManager.addListener(actionListener, "Left", "Right", "Up", "Down", "Jump");
        
    }
    
    private final actionListener() {
        
    }
*/
    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}

