package com.jdydev.ew.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jdydev.ew.Launcher;
import com.jdydev.ew.comm.CommUtil;
import com.jdydev.ew.comm.LoginMessage;
import com.jdydev.ew.server.EWServer;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.MMOCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

public class EWClient extends SimpleApplication implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(EWClient.class);

    public static final float SCALE = 0.01f;
    public static final float BASE_MOVE_SPEED = 700.0f;
    public static final float MOVE_SPEED = SCALE * BASE_MOVE_SPEED;
    public static final int DEBUG_COUNT = 1;

    private Client netClient;
    private Launcher launcher;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
    private MMOCharacterControl player;
    private Node ninja;
    private Vector3f walkDirection = new Vector3f();
    private boolean autoWalk = false;
    private boolean left = false, right = false, up = false, down = false;

    // Temporary vectors used on each frame.
    // They here to avoid instantiating new vectors on each frame
    private Vector3f forwardDir = new Vector3f();
    private Vector3f leftDir = new Vector3f();
    private float time = 0.0f;

    public static void main(String[] args) {
        try {
            new EWClient().start();
        } catch (Exception e) {
            log.error("Error while starting application", e);
            log.info("Loading with defaults");
            new EWClient(true).start();
        }
    }

    public EWClient() {
        super();
    }

    public EWClient(boolean retry) {
        this();
        if (retry) {
            AppSettings tempSettings = new AppSettings(true);
            tempSettings.setRenderer(AppSettings.LWJGL_OPENGL1);
            this.setSettings(tempSettings);
        }
    }

    public boolean authenticate(LoginMessage lm) {
        log.debug("Authenticate received message: {}", lm);
        try {
            CommUtil.registerMessages();
            netClient = Network.connectToServer(EWServer.SERVER_NAME, EWServer.SERVER_VERSION,
                    EWServer.SERVER_HOST, EWServer.SERVER_PORT);
            this.setupClient();
            netClient.start();
            log.debug("Connected to Server: {}", netClient.getId());
            while (!netClient.isConnected()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // Should probably throw something if we got interrupted
                }
            }
            netClient.send(lm);
            return true;
        } catch (IOException e) {
            log.error("Error while connecting to network server", e);
        }
        return false;
    }

    public void setLauncher(Launcher l) {
        this.launcher = l;
    }

    private void setupClient() {
        // Replace this with a Guava Assert?
        if (this.netClient == null) {
            throw new RuntimeException("Attempting to setup client when not initialized");
        }
        netClient.addMessageListener(new MessageListener<Client>() {
            @Override
            public void messageReceived(Client c, Message m) {
                LoginMessage lm = (LoginMessage) m;
                log.debug("Message Received: {}", lm);
                if (lm.isAccepted()) {
                    launcher.loginSuccess();
                } else {
                    launcher.loginFailure();
                }
            }
        }, LoginMessage.class);
    }

    @Override
    public void destroy() {
        log.debug("Checking network client");
        if (netClient != null) {
            log.debug("Shutting down network client");
            netClient.close();
        }
        super.destroy();
    }

    @Override
    public void simpleInitApp() {
        this.setupBullet(this.createTerrain());
    }

    public TerrainQuad createTerrain() {
        TerrainQuad terrain;
        Material mat_terrain;

        // 1. Create terrain material and load four textures into it.
        mat_terrain = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");

        // 1.1) Add ALPHA map (for red-blue-green coded splat textures)
        mat_terrain.setTexture("Alpha",
                assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));

        // / 1.2) Add GRASS texture into the red layer (Tex1).
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex1", grass);
        mat_terrain.setFloat("Tex1Scale", 64f);

        // 1.3) Add DIRT texture into the green layer (Tex2)
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex2", dirt);
        mat_terrain.setFloat("Tex2Scale", 32f);

        // 1.4) Add ROAD texture into the blue layer (Tex3)
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex3", rock);
        mat_terrain.setFloat("Tex3Scale", 128f);

        // 2. Create the height map
        Texture heightMapImage = assetManager
                .loadTexture("Textures/Terrain/splat/mountains512.png");
        AbstractHeightMap heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();

        /*
         * 3. We have prepared material and heightmap. Now we create the actual terrain:
         * 
         * 3.1) Create a TerrainQuad and name it "my terrain".
         * 
         * 3.2) A good value for terrain tiles is 64x64 -- so we supply 64+1=65.
         * 
         * 3.3) We prepared a heightmap of size 512x512 -- so we supply 512+1=513.
         * 
         * 3.4) As LOD step scale we supply Vector3f(1,1,1).
         * 
         * 3.5) We supply the prepared heightmap itself.
         */
        int patchSize = 65;
        terrain = new TerrainQuad("my terrain", patchSize, 513, heightmap.getHeightMap());

        /**
         * 4. We give the terrain its material, position & scale it, and attach it.
         */
        terrain.setMaterial(mat_terrain);
        terrain.setLocalScale(5f, 0.5f, 5f);

        /** 5. The LOD (level of detail) depends on were the camera is: */
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        terrain.addControl(control);

        return terrain;
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);

        DirectionalLight dl2 = new DirectionalLight();
        dl2.setColor(ColorRGBA.White);
        dl2.setDirection(new Vector3f(-2.8f, 2.8f, 2.8f).normalizeLocal());
        rootNode.addLight(dl2);
    }

    /**
     * We over-write some navigational key mappings here, so we can add physics-controlled walking
     * and jumping:
     */
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W), new MouseButtonTrigger(
                MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("Walk", new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Walk");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
    }

    /**
     * These are our custom actions triggered by key presses. We do not walk yet, we just keep track
     * of the direction the user pressed.
     */
    public void onAction(String binding, boolean isPressed, float tpf) {
        if (binding.equals("Left")) {
            left = isPressed;
        } else if (binding.equals("Right")) {
            right = isPressed;
        } else if (binding.equals("Up")) {
            up = isPressed;
        } else if (binding.equals("Down")) {
            down = isPressed;
        } else if (binding.equals("Jump")) {
            if (isPressed) {
                player.jump();
            }
        } else if (binding.equals("Walk")) {
            if (!isPressed)
                autoWalk = !autoWalk;
        }
    }

    /**
     * This is the main event loop--walking happens here. We check in which direction the player is
     * walking by interpreting the camera direction forward (camDir) and to the side (camLeft). The
     * setWalkDirection() command is what lets a physics-controlled player walk. We also make sure
     * here that the camera moves with player.
     */
    @Override
    public void simpleUpdate(float tpf) {
        cam.getDirection(forwardDir);
        cam.getLeft(leftDir);
        if (player.isOnGround()) {
            walkDirection.set(0, 0, 0);
            if (left) {
                walkDirection.addLocal(leftDir);
            }
            if (right) {
                walkDirection.subtractLocal(leftDir);
            }
            if (up || autoWalk) {
                walkDirection.addLocal(forwardDir);
            }
            if (down) {
                walkDirection.subtractLocal(forwardDir);
            }
            walkDirection.multLocal(1.0f, 0.0f, 1.0f);
            walkDirection.normalizeLocal();
            walkDirection.multLocal(MOVE_SPEED);
        } else if (!player.isJumping()) {
            walkDirection.setY(-MOVE_SPEED);
            log.debug("Airborne, and not jumping");
        }
        player.setWalkDirection(walkDirection);
        player.setViewDirection(forwardDir.negateLocal().multLocal(MOVE_SPEED));
        cam.setLocation(ninja.getLocalTranslation().addLocal(forwardDir));
        int timeOld = (int) (time * DEBUG_COUNT);
        time += tpf;
        if (log.isDebugEnabled()) {
            int timeNew = (int) (time * DEBUG_COUNT);
            if (timeOld != timeNew) {
                log.debug("{}: {}", timeNew, player);
            }
        }
    }

    private void setupBullet(Node scene) {
        /** Set up Physics */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(MOVE_SPEED);
        flyCam.setDragToRotate(true);
        // this is a hack to toggle inverted mouse, because it's not directly a function on the
        // flyCam.
        flyCam.onAction("FLYCAM_InvertY", false, 0.05f);
        setUpKeys();
        setUpLight();

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(scene);
        landscape = new RigidBodyControl(sceneShape, 0);
        scene.addControl(landscape);

        ninja = new Node();
        Node model = (Node) assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        model.scale(SCALE * 0.5f, SCALE * 0.5f, SCALE * 0.5f);
        model.rotate(0.0f, 0.0f, 0.0f);
        Vector3f loc = new Vector3f(384.61444f, 55.23122f, -523.6855f);
        ninja.attachChild(model);
        ninja.setLocalTranslation(loc);
        model.setLocalTranslation(0, -0.075f, 0);

        rootNode.attachChild(ninja);
        player = new MMOCharacterControl(SCALE * 10.0f, SCALE * 100.0f, SCALE * 1000.0f);
        ninja.addControl(player);

        // We attach the scene and the player to the rootnode and the physics space,
        // to make them appear in the game world.
        rootNode.attachChild(scene);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().add(player);
        // Toggle to true to see grids
        bulletAppState.setDebugEnabled(false);
    }
}
