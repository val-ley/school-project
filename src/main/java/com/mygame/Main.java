package com.mygame;

import com.jme3.scene.SceneGraphVisitor;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.List;

import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;

/**
 * Realistic Office Scene Loader
 * Automatically extracts lights from the scene file.
 */
public class Main extends SimpleApplication {

    private BulletAppState bulletAppState;
    private BetterCharacterControl playerControl;
    private Node playerNode;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    
    private GameUI gameUI; 
    private float mouseSensitivity = 1.0f;

    // We store the "Sun" here if we find one, so we can make it cast shadows
    private DirectionalLight mainSun;

    private com.jme3.post.filters.DepthOfFieldFilter dofFilter;
    private float currentFocusDist = 50f;


    public static void main(String[] args) {
        Main app = new Main();
        
        // 1. CRITICAL: Configure Settings before start
        AppSettings settings = new AppSettings(true);
        settings.setGammaCorrection(true); // Essential for PBR
        settings.setTitle("Bunker");
        settings.setResolution(1280, 720);
        
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 1. Physics Setup
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        // 2. Camera Setup
        flyCam.setEnabled(false); 
        cam.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 0.01f, 1000f);
        
        // 3. Load Scene Model
        Spatial officeScene = assetManager.loadModel("Scenes/OfficeScene.j3o");
        rootNode.attachChild(officeScene);
        TextureUtils.setNearestFilter(officeScene);
        fixTransparency(officeScene);
        // 4. EXTRACT LIGHTS from the Scene
        // This finds every lamp, sun, and spot you made in the editor
        extractLightsFromScene(officeScene);

        // 5. Physics for the Room
        CollisionShape officeShape = CollisionShapeFactory.createMeshShape(officeScene);
        RigidBodyControl officePhys = new RigidBodyControl(officeShape, 0); 
        officeScene.addControl(officePhys);
        bulletAppState.getPhysicsSpace().add(officePhys); 
        
        // 6. Setup Player
        setupPlayer();
        setupKeys();

        // 7. Setup Post-Processing and Shadows
        setupVisuals();
        
        // 8. UI
        gameUI = new GameUI(this); 
        gameUI.initializeUI();
    }
    
    private void extractLightsFromScene(Spatial sceneModel) {
        List<Light> lightsFound = new ArrayList<>();

        // CHANGE: Use 'SceneGraphVisitor' (Interface) instead of 'SceneGraphVisitorAdapter'
        sceneModel.breadthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                // Now we can visit every single object (Node or Geometry)
                for (Light light : spatial.getLocalLightList()) {
                    lightsFound.add(light);
                }
            }
        });

        System.out.println("--- LIGHTS FOUND IN SCENE ---");
        for (Light light : lightsFound) {
            System.out.println("Loaded: " + light.getName() + " [" + light.getClass().getSimpleName() + "]");
            
            rootNode.addLight(light);
            
            if (light instanceof DirectionalLight && mainSun == null) {
                mainSun = (DirectionalLight) light;
            }
        }
        System.out.println("-----------------------------");
    }

    private void setupVisuals() {
        // 1. Shadow Renderer
        // We only enable shadows for the MAIN SUN to save performance.
        // Calculating shadows for 20 point lights would kill the FPS.
        if (mainSun != null) {
            DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 3);
            dlsr.setLight(mainSun);
            dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON); // Soft shadows
            viewPort.addProcessor(dlsr);
        }

        // 2. Post Processing Filter Stack
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        // Fixed (Softer)
        // Param 1: Radius (Spread) - Keep at 3.0f or lower to 1.5f for tighter corners
        // Param 2: Intensity (Darkness) - LOWER THIS from 15.0f to roughly 2.0f - 5.0f
        SSAOFilter ssao = new SSAOFilter(6.0f, 10f, 1.0f, 0.1f);
        fpp.addFilter(ssao);

        // Bloom (Glow)
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Scene);
        bloom.setBloomIntensity(0.5f);
        fpp.addFilter(bloom);

        // Tone Mapping (HDR -> Monitor colors)
        ToneMapFilter toneMap = new ToneMapFilter();
        toneMap.setWhitePoint(new Vector3f(11.2f, 11.2f, 11.2f));
        fpp.addFilter(toneMap);

        dofFilter = new com.jme3.post.filters.DepthOfFieldFilter(); 
        dofFilter.setFocusDistance(50); 
        dofFilter.setFocusRange(10);   
        dofFilter.setBlurScale(1.5f); 
        fpp.addFilter(dofFilter);

        // FXAA (Anti-Aliasing)
        FXAAFilter fxaa = new FXAAFilter();
        fpp.addFilter(fxaa);

        viewPort.addProcessor(fpp);
        
        // Handle transparency correctly
        renderManager.setAlphaToCoverage(true);
    }

    private void setupPlayer() {
        playerNode = new Node("Player");
        playerControl = new BetterCharacterControl(1f, 5.5f, 2f); // (width), (hight), (weight) in float
        playerNode.addControl(playerControl);
        
        bulletAppState.getPhysicsSpace().add(playerControl);
        rootNode.attachChild(playerNode);
        
        playerControl.warp(new Vector3f(0.1f, 6f, 1.1f));
    }

    private void setupKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));

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

    private final AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            if (inputManager.isCursorVisible()) return;

            if (name.equals("RotateLeft")) rotatePlayer(value * mouseSensitivity, Vector3f.UNIT_Y);
            if (name.equals("RotateRight")) rotatePlayer(-value * mouseSensitivity, Vector3f.UNIT_Y);
            if (name.equals("LookUp")) rotateCamera(-value * mouseSensitivity);
            if (name.equals("LookDown")) rotateCamera(value * mouseSensitivity);
        }
    };

    private void rotatePlayer(float value, Vector3f axis) {
        Quaternion rotate = new Quaternion().fromAngleAxis(value, axis);
        playerNode.rotate(rotate);
        playerControl.setViewDirection(playerNode.getLocalRotation().mult(Vector3f.UNIT_Z));
    }

    private void rotateCamera(float value) {
        Quaternion pitch = new Quaternion().fromAngleAxis(value, Vector3f.UNIT_X);
        cam.getRotation().multLocal(pitch);
    }

    @Override
    public void simpleUpdate(float tpf) {
        // 1. Movement Logic (Your existing code)
        Vector3f nodeDir = playerNode.getLocalRotation().mult(Vector3f.UNIT_Z);
        Vector3f nodeLeft = playerNode.getLocalRotation().mult(Vector3f.UNIT_X);
        
        walkDirection.set(0, 0, 0);
        if (left) walkDirection.addLocal(nodeLeft);
        if (right) walkDirection.addLocal(nodeLeft.negate());
        if (up) walkDirection.addLocal(nodeDir);
        if (down) walkDirection.addLocal(nodeDir.negate());
        
        walkDirection.y = 0; 
        playerControl.setWalkDirection(walkDirection.mult(5f)); 

        cam.setLocation(playerNode.getWorldTranslation().add(0, 5.3f, 0));
        
        // 2. Camera Sync (Your existing code)
        float[] angles = new float[3];
        playerNode.getLocalRotation().toAngles(angles);
        float[] camAngles = new float[3];
        cam.getRotation().toAngles(camAngles);
        Quaternion currentCamRot = new Quaternion().fromAngles(camAngles[0], angles[1], 0);
        cam.setRotation(currentCamRot);

        // 3. AUTO-FOCUS LOGIC (New)
        if (dofFilter != null) {
            // Create a Ray from camera position, pointing forward
            com.jme3.math.Ray ray = new com.jme3.math.Ray(cam.getLocation(), cam.getDirection());
            com.jme3.collision.CollisionResults results = new com.jme3.collision.CollisionResults();
            
            // Check what the ray hits in the scene
            rootNode.collideWith(ray, results);

            float targetDist = 100f; // Default to far away if we look at the sky
            
            if (results.size() > 0) {
                // Get distance to the closest object
                float dist = results.getClosestCollision().getDistance();
                // Clamp min distance to 1.0f so we don't focus INSIDE our own eyeball
                targetDist = Math.max(0.5f, dist); 
            }

            // Smoothly transition focus (Lerp)
            // '10f * tpf' determines the speed of the eye adaptation
            float focusSpeed = 10f * tpf; 
            currentFocusDist = com.jme3.math.FastMath.interpolateLinear(focusSpeed, currentFocusDist, targetDist);

            // Apply to filter
            dofFilter.setFocusDistance(currentFocusDist);
            
            // OPTIONAL: Dynamic Range
            // When looking at something close (Macro), the range of sharpness is small.
            // When looking far away (Landscape), the range of sharpness is huge.
            // This math simulates that physics behavior:
            dofFilter.setFocusRange(Math.max(5f, currentFocusDist * 2.0f));
        }
    }
    
    private void fixTransparency(Spatial spatial) {
        spatial.breadthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Geometry) {
                    Geometry geom = (Geometry) spatial;
                    Material mat = geom.getMaterial();
                    
                    // 1. Enable Alpha Discard (The Magic Fix)
                    // If a pixel's alpha is lower than 0.5, the GPU skips it entirely.
                    // This allows walls behind the object to be drawn.
                    if (mat.getParam("AlphaDiscardThreshold") == null) {
                        mat.setFloat("AlphaDiscardThreshold", 0.5f);
                    }

                    // 2. Optional: If you have "True Glass" (windows), you need this instead:
                    /*
                    if (geom.getName().contains("Glass")) {
                        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                        geom.setQueueBucket(RenderQueue.Bucket.Transparent);
                        mat.clearParam("AlphaDiscardThreshold"); // Don't use discard on glass
                    }
                    */
                    
                    // 3. Double Sided (Optional)
                    // Useful for fences/leaves so you can see them from behind
                    mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                }
            }
        });
    }

    
    @Override
    public void simpleRender(RenderManager rm) {}
    
    @Override
    public void destroy() {
        super.destroy(); 
        if (gameUI != null) gameUI.cleanup(); 
    }
}