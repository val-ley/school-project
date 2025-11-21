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

import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowRenderer;

public class Main extends SimpleApplication {

    private BulletAppState bulletAppState;
    private BetterCharacterControl playerControl;
    private Node playerNode;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    
    private GameUI gameUI; 
    private float mouseSensitivity = 1.0f;

    // store the sun here if we find one to cast shadows (probably wont be needed at all)
    private DirectionalLight mainSun;
    private List<Light> allSceneLights = new ArrayList<>(); // THIS IS VERY EXPENSIVE
    // for those who dont know, a cubemap will need to be generated for shadows every frame on the GPU,
    // since this game runs at 2K+ FPS on a decent graphics card, I think its fine

    private com.jme3.post.filters.DepthOfFieldFilter dofFilter;
    private float currentFocusDist = 50f;

    

    public static void main(String[] args) {

        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            configureLinuxCompatibility();
        }

        Main app = new Main();
        
        // configure Settings before start
        AppSettings settings = new AppSettings(true);
        settings.setGammaCorrection(true); // required for PBR textures
        settings.setTitle("Bunker");
        settings.setResolution(1280, 720);
        
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // physics pre-setup
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        // camera setup
        flyCam.setEnabled(false); 
        cam.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 0.01f, 1000f);
        
        // loading scene and applying filters and transperancy pass
        Spatial officeScene = assetManager.loadModel("Scenes/OfficeScene.j3o");
        rootNode.attachChild(officeScene);
        TextureUtils.setNearestFilter(officeScene);
        fixTransparency(officeScene);
        // extract and apply lights
        extractLightsFromScene(officeScene);

        // enable physics colissions for the room
        CollisionShape officeShape = CollisionShapeFactory.createMeshShape(officeScene);
        RigidBodyControl officePhys = new RigidBodyControl(officeShape, 0); 
        officeScene.addControl(officePhys);
        bulletAppState.getPhysicsSpace().add(officePhys); 
        
        // setup and enable player movement
        setupPlayer();
        setupKeys();

        // post processing pass
        setupVisuals();
        
        // run the UI
        gameUI = new GameUI(this); 
        gameUI.initializeUI();
    }

    private void extractLightsFromScene(Spatial sceneModel) {
        // clear list when the scene is reloaded
        allSceneLights.clear(); 

        sceneModel.breadthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                for (Light light : spatial.getLocalLightList()) {
                    allSceneLights.add(light);
                }
            }
        });

        System.out.println("--- LIGHTS FOUND & ENABLED ---");
        for (Light light : allSceneLights) {
            System.out.println("Loaded: " + light.getName() + " [" + light.getClass().getSimpleName() + "]");
            
            rootNode.addLight(light);
            
            // even though we render shadows for all lights, still identify the sun seperate
            if (light instanceof DirectionalLight && mainSun == null) {
                mainSun = (DirectionalLight) light;
            }
        }
        System.out.println("-----------------------------");
    }

    private void setupVisuals() {
        
        // loop through lights and add shadows
        for (Light light : allSceneLights) {
            
            // directional light (the sun)
            if (light instanceof DirectionalLight) {
                DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 3);
                dlsr.setLight((DirectionalLight) light);
                dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON); 
                viewPort.addProcessor(dlsr);
            } 
            
            // spot lights (flashlight)
            else if (light instanceof SpotLight) {
                SpotLightShadowRenderer slsr = new SpotLightShadowRenderer(assetManager, 2048);
                slsr.setLight((SpotLight) light);
                slsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON); 
                viewPort.addProcessor(slsr);
            }
            
            // point lights (lamp / ceiling light)
            // this is the expensive (renders 6 times per light), one of the reasons the res is set so low
            else if (light instanceof PointLight) {
                PointLightShadowRenderer plsr = new PointLightShadowRenderer(assetManager, 512);
                plsr.setLight((PointLight) light);
                plsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON); 
                viewPort.addProcessor(plsr);
            }
        }

        // post processing filters (SSAO, Bloom, etc.)
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        // SSAO
        SSAOFilter ssao = new SSAOFilter(6.0f, 10f, 1.0f, 0.1f);
        fpp.addFilter(ssao);

        // Bloom
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Scene);
        bloom.setBloomIntensity(0.5f);
        fpp.addFilter(bloom);

        // Tone Mapping
        ToneMapFilter toneMap = new ToneMapFilter();
        toneMap.setWhitePoint(new Vector3f(11.2f, 11.2f, 11.2f));
        fpp.addFilter(toneMap);

        // Depth Of Field
        dofFilter = new com.jme3.post.filters.DepthOfFieldFilter(); 
        dofFilter.setFocusDistance(50); 
        dofFilter.setFocusRange(10);   
        dofFilter.setBlurScale(1.5f); 
        fpp.addFilter(dofFilter);

        // FXAA
        FXAAFilter fxaa = new FXAAFilter();
        fpp.addFilter(fxaa);

        viewPort.addProcessor(fpp);
        
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

    private void setupKeys() { // author: random dude on reddit, thanks for the movement code!
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

    
    private static void configureLinuxCompatibility() {
        System.out.println("--- 🚨 LINUX DETECTED 🚨 : APPLYING COMPATIBILITY FIXES ---");

        // FORCE SOFTWARE RENDERING FOR UI
        // the crash happens because JavaFX tries to grab the GPU context from Wayland.
        // setting "sw" should force CPU rendering, dumping the GPU render issues.
        // This should in theory bypass the "Gdk-CRITICAL" and "SIGSEGV" errors when running on Wayland.
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.verbose", "true");

        // FORCE GTK3
        // Ubuntu 25 might try GTK4, but JavaFX expects GTK3.
        System.setProperty("jdk.gtk.version", "3");
        System.setProperty("org.lwjgl.glfw.checkThread0", "false");
        System.setProperty("org.lwjgl.glfw.libname", "glfw");
    }

    @Override
    public void simpleRender(RenderManager rm) {}
    
    @Override
    public void destroy() {
        super.destroy(); 
        if (gameUI != null) gameUI.cleanup(); 
    }
}