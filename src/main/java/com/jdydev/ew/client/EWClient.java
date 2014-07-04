package com.jdydev.ew.client;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
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
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

public class EWClient extends SimpleApplication implements ActionListener {

    public static final float SCALE = 0.10f;
    public static final float MOVE_SPEED = 300.0f;
    // private Spatial sceneModel;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
    private BetterCharacterControl player;
    private Node ninja;
    private Vector3f walkDirection = new Vector3f();
    private boolean autoWalk = false;
    private boolean left = false, right = false, up = false, down = false;

    // Temporary vectors used on each frame.
    // They here to avoid instantiating new vectors on each frame
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();

    public static void main(String[] args) {
        EWClient app = new EWClient();
        app.start();
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
//        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2f, 0.5f, 2f);

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
        camDir.set(cam.getDirection()).multLocal(SCALE * MOVE_SPEED);
        camLeft.set(cam.getLeft()).multLocal(SCALE * MOVE_SPEED * 0.5f);
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up || autoWalk) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        walkDirection.multLocal(1.0f, 0, 1.0f);
        walkDirection.addLocal(0, -1, 0);
        player.setWalkDirection(walkDirection);
        player.setViewDirection(camDir.negate());
        cam.setLocation(ninja.getLocalTranslation().addLocal(camDir.negate()));
    }

    private void setupBullet(Node scene) {
        /** Set up Physics */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        // Toggle to true to see grids
        bulletAppState.setDebugEnabled(false);

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

        // We set up collision detection for the player by creating
        // a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for
        // size, stepheight, jumping, falling, and gravity.
        // We also put the player in its starting position.

        ninja = new Node();
        Node model = (Node) assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        model.scale(SCALE * 0.5f, SCALE * 0.5f, SCALE * 0.5f);
        model.rotate(0.0f, 0.0f, 0.0f);
        Vector3f loc = new Vector3f(125, 32, 5);
        ninja.attachChild(model);
        ninja.setLocalTranslation(loc);
        model.setLocalTranslation(0, -0.075f, 0);
        

        rootNode.attachChild(ninja);
        player = new BetterCharacterControl(SCALE * 13.0f, SCALE * 100.0f, SCALE * 80000.0f);
        ninja.addControl(player);

        // We attach the scene and the player to the rootnode and the physics space,
        // to make them appear in the game world.
        rootNode.attachChild(scene);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().add(player);
        player.setGravity(new Vector3f(0, -10, 0));
        player.setJumpForce(new Vector3f(0, 15, 0));
    }
}
