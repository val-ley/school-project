package com.mygame;

import com.jme3.scene.Spatial;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;

/**
 * This is the Main Class of the Game.
 * @author bulutthecat
 */
public class Main extends SimpleApplication {

    private BulletAppState bulletAppState;
    private BetterCharacterControl playerControl;
    private Node playerNode;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    
    // UI Manager
    private GameUI gameUI; 
    
    // Camera/Mouse variables
    private float mouseSensitivity = 1.0f; // adjust this for speed
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        
        // physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        // disable flyCam so we can implement our own FPS camera
        flyCam.setEnabled(false); 
        
        // scene setup | getting the camera to not clip through walls
        cam.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 0.01f, 1000f);
        
        Spatial officeStart = assetManager.loadModel("Scenes/OfficeScene.j3o");
        officeStart.breadthFirstTraversal(new ScenePolisher(assetManager));
        rootNode.attachChild(officeStart);
        
        // enable physics for room
        CollisionShape officeShape = CollisionShapeFactory.createMeshShape(officeStart);
        RigidBodyControl officePhys = new RigidBodyControl(officeShape, 0); 
        officeStart.addControl(officePhys);
        bulletAppState.getPhysicsSpace().add(officePhys); 
        
        // create player
        playerNode = new Node("Player");
        // radius 0.5, height 2, weight 0.5 
        playerControl = new BetterCharacterControl(0.2f, 1f, 0.5f);
        playerNode.addControl(playerControl);
        
        bulletAppState.getPhysicsSpace().add(playerControl);
        rootNode.attachChild(playerNode);
        
        // initilize and setup JavaFX UI (Main Menu)
        gameUI = new GameUI(this); 
        gameUI.initializeUI();
        
        // tp player to room start
        playerControl.warp(new Vector3f(0.1f, 1f, 1.1f));
        
        setupKeys();
        
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        rootNode.addLight(sun);
        
    }

    private void setupKeys() {
        // keyboard mappings
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));

        // mouse mappings (For looking around)
        inputManager.addMapping("RotateLeft", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("RotateRight", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("LookUp", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("LookDown", new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        inputManager.addListener(actionListener, "Left", "Right", "Up", "Down", "Jump");
        inputManager.addListener(analogListener, "RotateLeft", "RotateRight", "LookUp", "LookDown");
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Left")) left = isPressed;
            if (name.equals("Right")) right = isPressed;
            if (name.equals("Up")) up = isPressed;
            if (name.equals("Down")) down = isPressed;
            if (name.equals("Jump") && isPressed) playerControl.jump();
        }
    };

    // analog listener for smooth mouse movement
    private final AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            // only rotate if the cursor is hidden
            
            if (inputManager.isCursorVisible()) {
                return;
            }

            if (name.equals("RotateLeft")) {
                rotatePlayer(value * mouseSensitivity, Vector3f.UNIT_Y);
            }
            if (name.equals("RotateRight")) {
                rotatePlayer(-value * mouseSensitivity, Vector3f.UNIT_Y);
            }
            if (name.equals("LookUp")) {
                rotateCamera(-value * mouseSensitivity);
            }
            if (name.equals("LookDown")) {
                rotateCamera(value * mouseSensitivity);
            }
        }
    };

    // helper to rotate the player node (YAW)
    private void rotatePlayer(float value, Vector3f axis) {
        Quaternion rotate = new Quaternion().fromAngleAxis(value, axis);
        // rotate the playerNode. the camera will follow this rotation in simpleUpdate
        playerNode.rotate(rotate);
        
        // we need to update the physics control view direction so we walk in the correct direction
        playerControl.setViewDirection(playerNode.getLocalRotation().mult(Vector3f.UNIT_Z));
    }

    // helper to rotate the Camera (PITCH)
    private void rotateCamera(float value) {
        // apply pitch directly to the camera, but dont rotate the player body up or down
        // (Unless we want the character to do a backflip!)
        
        // limit how far up/down we can look (roughly 90 degrees)
        // this part is annoying and tricky in raw JME, but according to some guy on reddit a simple lookAt is often enough.
        // for now, its fine to just pitch the cam local rotation.
        
        Quaternion pitch = new Quaternion().fromAngleAxis(value, Vector3f.UNIT_X);
        cam.getRotation().multLocal(pitch);
    }
    
    public BulletAppState getBulletAppState() {
        return bulletAppState;
    }

    public Node getPlayerNode() {
        return playerNode;
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        // calculate movement direction based on player node rotation (not camera)
        // why? because the Camera might be looking up at the sky, so the player would fly up, instead of walk forward
        Vector3f nodeDir = playerNode.getLocalRotation().mult(Vector3f.UNIT_Z);
        Vector3f nodeLeft = playerNode.getLocalRotation().mult(Vector3f.UNIT_X);
        
        walkDirection.set(0, 0, 0);
        if (left) walkDirection.addLocal(nodeLeft);
        if (right) walkDirection.addLocal(nodeLeft.negate());
        if (up) walkDirection.addLocal(nodeDir);
        if (down) walkDirection.addLocal(nodeDir.negate());
        
        // flatten walk to prevent flying if we used cam direction
        walkDirection.y = 0; 
        
        // move the physics character
        playerControl.setWalkDirection(walkDirection.mult(10f)); 

        // sync camera position
        // move cam to player head
        cam.setLocation(playerNode.getWorldTranslation().add(0, 6f, 0));
        
        // sync Camera YAW to player, but keep PITCH independent
        // this is the tricky math part
        // get player YAW
        float[] angles = new float[3];
        playerNode.getLocalRotation().toAngles(angles);
        
        // get the camera current pitch (X rotation)
        float[] camAngles = new float[3];
        cam.getRotation().toAngles(camAngles);
        
        // set the camera rotation to: Player's YAW + Camera's Existing PITCH
        Quaternion currentCamRot = new Quaternion().fromAngles(camAngles[0], angles[1], 0);
        cam.setRotation(currentCamRot);
    }
    
    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    @Override
    public void destroy() {
        super.destroy(); 
        if (gameUI != null) { 
            gameUI.cleanup(); 
        }
    }
    
}