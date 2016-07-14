package com.android.libraries;

/*
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

import com.android.libraries.jpctutils.Terrain;
import com.android.libraries.location.LocationFusionStrategy;
import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.*;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.SkyBox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
*/
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

import com.android.libraries.R;
import com.android.libraries.jpctutils.Terrain;
import com.android.libraries.location.LocationFusionStrategy;
import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.SkyBox;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Mario Salierno on 21/03/2016.
 */
public class JPCTWorldManager implements GLSurfaceView.Renderer{

    private final String TAG = "JPCTWorldManager";

    private World world = null;
    private SkyBox skybox = null;
    private int SKYBOX_DIM = 8192;//1024;
    private int CAMERA_HEIGHT = 0;//default value
    private int GROUND_ALTITUDE = -20;
    private double Y_FOV_VALUE = 0; //in degrees
    private final int X_TO_NORTH_ANGLE = 90;

    //private World sky = null;
    private Light sun = null;

    //ground
    private Object3D ground;
    private SimpleVector groundTransformedCenter;
    private final String groundID = "groundobjID";


    private HashMap<String, TranslationObject> worldObjects = new HashMap<String, TranslationObject>();
    RGBColor background = new RGBColor(0,0,0,0);//bigtransparent background
    private FrameBuffer frameBuffer = null;

    //boolean to set whether gles2 is available..?
    private boolean gl2 = true;

    private TextureFromCameraActivity activity;
    private GLSurfaceView mGLView;
    private SimParameters simulation;
    LocationFusionStrategy locationFusion;
    //Location lastLocation;
    //Location myLoc;
    Location zeroLoc=null;
    private float xCAMERA=0, yCAMERA=0, zCAMERA=0;
    /*
    JPCT COORDINATES
    positive axis:
    x -> right of the screen
    y -> down to the screen
    z -> inside the screen

    mAzimuthView heading -> Z JPCT
    mPitchView -> x JPCT
    mRollView -> y JPCT
    */
    private float roll=0,pitch=0,head=0;
    private boolean facedown = false;

    public void setRPHCam(float r,float p,float h,boolean fdown){
        facedown=fdown;
        roll=r;
        pitch=p;
        head=h;

    }

    public JPCTWorldManager(TextureFromCameraActivity activity,
                            SimParameters simulation,
                            LocationFusionStrategy locationFusion,
                            Integer CAMERA_HEIGHT) {

        this.activity = activity;
        this.simulation=simulation;
        this.locationFusion=locationFusion;

        if(CAMERA_HEIGHT!=null)
            this.CAMERA_HEIGHT=CAMERA_HEIGHT;


    }


    private void configureEnvironment(){

        /*
        The first two lines (glAvoid... and maxPolys...) are mainly to save some memory.
        We don't need copies of your textures in main memory once uploaded to the graphics card
        and we are using Compiled objects, which is why our VisList won't get very huge,
        so 1000 polygons are more than enough here.
        */
        //Config.glAvoidTextureCopies = true;
        Config.maxPolysVisible = 1000;
        //Config.glColorDepth = 24;
        //Config.glFullscreen = false;

        //TO FIX setting right value
        Config.farPlane = 4000;
        //Config.glShadowZBias = 0.8f;
        //Config.lightMul = 1;

        /*
        If set to true, buffers for uploading the textures will be reused if possible.
        This can improve texture upload speed and may reduce garbage collection activity during uploads
        but it can lead to peaks in memory usage. Default is false.
        Config.reuseTextureBuffers=true;
        */
        Config.reuseTextureBuffers=true;

        /*
        collideOffset is needed to make the Collision detection work with the plane.
        It's a common problem when using collision detection, that your collision sources
        are passing through the obstacles, if this value is too low.
        500 is a reasonable value for this example. The last line (glTrilinear)
        makes jPCT use trilinear filtering.
        It simply looks better that way.
         */
        //Config.collideOffset = 500;
        Config.glTrilinear = true;


    }

    public void setUpWorld(){

        configureEnvironment();

        world = new World();
        //world.setAmbientLight(20, 20, 20);


        sun = new Light(world);
        sun.setIntensity(128, 128, 128);
        //sun.setIntensity(255, 255, 255);
        Camera worldCam = world.getCamera();
        worldCam.setPosition(0,0,CAMERA_HEIGHT);

        //Double fov = toRad(Y_FOV_VALUE);
        //worldCam.setYFovAngle(fov.floatValue());
        //worldCam.setFovAngle(fov.floatValue());
        //worldCam.se
        //worldCam.setFOVLimits(0.7f,1.3f);

        /*
            Two worlds because one is for the scene itself and one is for the sky dome.
            This makes it much easier to handle the dome, but you don't have to do it this way.
            Adding the dome to the scene itself would work too.
        */
        //sky = new World();
        //sky.setAmbientLight(255, 255, 255);

        //world.getLights().setRGBScale(Lights.RGB_SCALE_2X);
        //sky.getLights().setRGBScale(Lights.RGB_SCALE_2X);
        //setUpSkyBox();

    }



    private void setUpSkyBox(){
        //TO FIX vedi classe BitmapHelper jpct
        String textureID = "groundTexture";

        TextureManager txtManager = TextureManager.getInstance();
        //Drawable groundImage = activity.getResources().getDrawable(R.drawable.bigoffice);
        Drawable groundImage = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.bigoffice, null);
        Texture groundTexture = new Texture(groundImage);
        txtManager.addTexture(textureID,groundTexture);


        String transparentID = "transparentback";
        //Drawable transpImage = activity.getResources().getDrawable(activity.getResources(),R.drawable.transparentback, null);
        //Drawable transpImage = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.transparentback, null);

        //
        //int skyBWidth = groundImage.getIntrinsicWidth();
        //int skyBHeight = groundImage.getIntrinsicWidth();
        //Log.e("GROUND", "W:"+groundImage.getIntrinsicWidth()+" H:"+groundImage.getIntrinsicHeight());
        //Log.e("GROUND", "W:"+groundImage.getMinimumWidth()+" H:"+groundImage.getMinimumHeight());
        Bitmap transparentbmp = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        this.makeTransparent(transparentbmp);
        Drawable transpImage = new BitmapDrawable(activity.getResources(), transparentbmp);

        //


        Texture transpTexture = new Texture(transpImage);//new Texture(1024,1024, RGBColor.BLACK);

        txtManager.addTexture(transparentID,transpTexture);

        //TextureManager.getInstance().addTexture("panda", new Texture(getBitmapFromAssetsARGB8888(256,256,"gfx/alpha.png", AppContext), true);


        skybox = new SkyBox(transparentID,
                transparentID,
                transparentID,
                textureID,
                transparentID,
                transparentID,
                SKYBOX_DIM);
        //skybox.setCenter(world.getCamera().getPosition());
        SimpleVector skyBoxPos = new SimpleVector(world.getCamera().getPosition());
        //SimpleVector skyBoxPos = world.getCamera().getPosition();
        skyBoxPos.x=skyBoxPos.x+SKYBOX_DIM/2;
        skyBoxPos.y=skyBoxPos.y+SKYBOX_DIM/2;
        //skyBoxPos.z-=100;
        //skyBoxPos.z=skyBoxPos.z+SKYBOX_DIM/2-80;
        skybox.setCenter(skyBoxPos);
        Log.e("setUpSkyBox", "skybox created");

    }

    public void setUpFrameBuffer(int width, int height){

        if(frameBuffer!=null)frameBuffer.dispose();

        frameBuffer=new FrameBuffer(width, height);

    }


    public void createGroundPlane(){
        String id = "groundPlaneID";
        float x = 0, y=0, z=0;
        Log.e("ground:"+id, "CREATED at:"+x+" y:"+y+" z:"+z);
        TextureManager txtManager = TextureManager.getInstance();
        Texture txt = new Texture(128,128, RGBColor.BLACK);
        txtManager.addTexture(id, txt);


        int dim = 1;//(int) (500 / z);

        Object3D plane = Primitives.getPlane(1,128);

        plane.translate(x, y, z);
        plane.rotateX(30);
        plane.setTexture(id);
        plane.setName(id);
        world.addObject(plane);
        worldObjects.put(id, new TranslationObject(id, plane, x,y,z));

    }
    /*
    public void createGroundPlane(){

        String textureID = "groundTexture";

        TextureManager txtManager = TextureManager.getInstance();
        Drawable groundImage = activity.getResources().getDrawable(R.drawable.office);
        Texture groundTexture = new Texture(groundImage);
        txtManager.addTexture(textureID,groundTexture);
        //ground = Terrain.generateDynamicGround(128,128,activity.getResources(),txtManager,textureID,GROUND_ALTITUDE);

        ground=Primitives.getPlane(1, 1024);




        SimpleVector sv = world.getCamera().getPosition();
        ground.translate(sv.x, sv.y, sv.z);
        ground.setTexture(textureID);
        ground.setName(groundID);
        world.addObject(ground);
        groundTransformedCenter = ground.getTransformedCenter();
        Log.e("createGround", "ground created");

    }
    */

    public void createGround(){

        String textureID = "groundTexture";

        TextureManager txtManager = TextureManager.getInstance();
        Drawable groundImage = activity.getResources().getDrawable(R.drawable.office);
        Texture groundTexture = new Texture(groundImage);
        txtManager.addTexture(textureID,groundTexture);
        ground = Terrain.generateDynamicGround(128,128,activity.getResources(),txtManager,textureID,GROUND_ALTITUDE);



        int dim = 1;//(int) (500 / z);


        //cube.translate(x, y, z);
        ground.setTexture(textureID);
        ground.setName(groundID);
        world.addObject(ground);
        groundTransformedCenter = ground.getTransformedCenter();
        Log.e("createGround", "ground created");

    }

    public void createPrimitiveCube(String id, float x, float y, float z){
        Log.e("object:"+id, "CREATED at:"+x+" y:"+y+" z:"+z);
        TextureManager txtManager = TextureManager.getInstance();
        Texture txt;
        if(!txtManager.containsTexture(id)) {
            //txtManager.removeTexture(id);
            if(id.equals("rosina")){
                txt = new Texture(64,64,RGBColor.RED);
            }
            else if(id.equals("sanpaolo")){
                txt = new Texture(64,64,RGBColor.RED);
                txtManager.addTexture(id, txt);
                int dim = 5;//(int) (500 / z);

                Object3D cube = Primitives.getCube(dim);

                cube.translate(x, y, z);
                cube.rotateX((float) Math.toRadians(90.0));
                cube.setTexture(id);
                cube.setName(id);
                world.addObject(cube);
                return;
            }
            else if(id.equals("asobrero")){
                txt = new Texture(64,64,new RGBColor(255, 255, 0));

                txtManager.addTexture(id, txt);
                int dim = 20;//(int) (500 / z);

                Object3D cube = Primitives.getCube(dim);

                cube.translate(x, y, z);
                cube.setTexture(id);
                cube.setName(id);
                world.addObject(cube);
                return;
            }
            else if(id.equals("pirandello")){
                txt = new Texture(64,64,RGBColor.WHITE);
            }
            else if(id.equals("pirandello10")){
                txt = new Texture(64,64,RGBColor.GREEN);
            }
            else if(id.equals("traiano")){
                txt = new Texture(64,64,RGBColor.BLACK);
            }
            else if(id.equals("moscovio")){
                txt = new Texture(64,64,new RGBColor(144,132,53));
            }
            else if(id.equals("duomo")){
                txt = new Texture(64,64,new RGBColor(255,153,0));
            }
            else if(id.equals("nikila")){
                txt = new Texture(64,64,new RGBColor(223,115,255));
            }
            else if(id.equals("XAXIS")){
                txt = new Texture(64,64,new RGBColor(0,0,255));//BLU MYSPOT
            }
            else if(id.equals("XAXISBACK")){
                txt = new Texture(64,64,new RGBColor(179, 179, 255));
            }
            else if(id.equals("YAXIS")){
                txt = new Texture(64,64,new RGBColor(255, 0, 0));//ROSSO MLN
            }
            else if(id.equals("YAXISBACK")){
                txt = new Texture(64,64,new RGBColor(255, 179, 179));
            }
            else if(id.equals("ZAXIS")){
                txt = new Texture(64,64,new RGBColor(0, 255, 0));//VERDE su
            }
            else if(id.equals("ZAXISBACK")){
                txt = new Texture(64,64,new RGBColor(179, 255, 179));
            }

            else
                txt = new Texture(64,64,RGBColor.BLACK);

            txtManager.addTexture(id, txt);
        }
        else
            txt = txtManager.getTexture(id);

        int dim = 1;//(int) (500 / z);

        Object3D cube = Primitives.getCube(dim);

        cube.translate(x, y, z);
        cube.setTexture(id);
        cube.setName(id);
        world.addObject(cube);
        worldObjects.put(id, new TranslationObject(id, cube, x,y,z));

    }


    //http://stackoverflow.com/questions/15298130/load-3d-models-with-jpct-ae
    public class TexturedObject{

        public String id;
        public int textureId;
        public float x,y,z;
        public int dimension;
        public float scale;
        public Texture texture;
        public Object3D obj3D;

        //public String texturePath;

        public TexturedObject(String id,
                              int textureId,///*p.e R.drawable.bigoffice*/
                              //String texturePath,
                              float scale,
                              float x,
                              float y,
                              float z,
                              int dim){
            this.id=id;
            this.textureId = textureId;
            //this.texturePath=texturePath; //path = assets/
            this.scale=scale;
            this.x=x;
            this.y=y;
            this.z=z;
            dimension=dim;
            obj3D = load3DSObject(id, textureId, scale);
        }



        //http://www.jpct.net/forum2/index.php/topic,2168.15.html?PHPSESSID=2963dbbdcd6472ebe013778ea71482ec
        ////txtrName for example for a named bman.3ds, it would be just bman
        private Object3D load3DSObject(String txtrName, int textureID/*p.e R.drawable.bigoffice*/, float thingScale /*= 1*/)
        {

            //TextureManager.getInstance().addTexture(txtrName + ".jpg", new Texture("res/" + txtrName + ".jpg"));
            // Create a texture out of the icon...:-)
            //texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(activity.getResources().getDrawable(R.drawable.ic_launcher)), 64, 64));
            Log.e("TEXTUREchairID","chair:"+textureID);
            //Drawable image = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.bigoffice   /*textureID*/ , null);

            //texture = new Texture(image);

            //Bitmap bmp = BitmapFactory.decodeFile( "/drawable/" );
            Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), textureID);
            //texture = new Texture(activity.getResources().openRawResource(textureID));
            texture = new Texture(bm);
            //texture = new Texture(200,200);
            //texture = new Texture(BitmapHelper.convert(image));
            TextureManager.getInstance().addTexture(txtrName, texture);


            try {
                String fname = /*"assets/" + */txtrName+".3ds";
                Log.e(TAG, "model file name:"+fname);
                Object3D objT = loadModel(fname, thingScale);
                //Primitives.getCube(10);
                //cube.calcTextureWrapSpherical();
                //cube.setTexture("texture");
                //cube.strip();
                objT.build();

                return objT;
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            return null;
        }


        //http://stackoverflow.com/questions/15298130/load-3d-models-with-jpct-ae
        private Object3D loadModel(String filename, float scale) throws UnsupportedEncodingException {

            //InputStream stream = new ByteArrayInputStream(filename.getBytes("UTF-8"));
            //InputStream stream = mContext.getAssets().open("FILENAME.3DS")
            InputStream stream = null;
            try {
                stream = activity.getApplicationContext().getAssets().open(filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Object3D[] model;
            if(filename.endsWith(".3ds")){
                model = Loader.load3DS(stream, scale);
            }
            else
                return null;

            Object3D o3d = new Object3D(0);
            Object3D temp = null;
            for (int i = 0; i < model.length; i++) {
                temp = model[i];
                temp.setCenter(SimpleVector.ORIGIN);
                temp.rotateX((float)( -.5*Math.PI));
                temp.rotateMesh();
                temp.setRotationMatrix(new Matrix());
                o3d = Object3D.mergeObjects(o3d, temp);
                o3d.build();
            }
            return o3d;
        }
    }

    public void manageObjectCreationFromModel(TexturedObject tObj){
        Log.e("object:"+tObj.id, "CREATED at:"+tObj.x+" y:"+tObj.y+" z:"+tObj.z);
        TextureManager txtManager = TextureManager.getInstance();
        Texture txt;
        if(!txtManager.containsTexture(tObj.id)) {

            txt = tObj.texture;


        }
        else
            txt = txtManager.getTexture(tObj.id);


        //txtManager.addTexture(tObj.id, txt);

        //Object3D cube = Primitives.getCube(tObj.dimension);
        Object3D obj3D = tObj.obj3D;
        obj3D.translate(tObj.x, tObj.y, tObj.z);
        obj3D.setTexture(tObj.id);
        obj3D.setName(tObj.id);
        world.addObject(obj3D);

    }

    //GLSurfaceView.Renderer methods onSurfaceCreated onSurfaceChanged onDrawFrame
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        TextureManager.getInstance().flush();

        setUpWorld();

        manageObjectsCreation2();


        /*
        TexturedObject tObj = new TexturedObject("chair",//id
                                            R.drawable.chair,//p.e R.drawable.bigoffice//textureID
                                                1f,//scale
                                                -180,//x
                                                -30,//y
                                                -80,//z
                                                1);//dim
        */

        TexturedObject tObj = new TexturedObject("chair",//id
                R.drawable.chair,//p.e R.drawable.bigoffice//textureID
                0.025f,//scale
                -4,//x
                0,//y
                -2,//z
                1);//dim

        tObj.obj3D.rotateX((float)Math.toRadians(90.0));

        //tObj.obj3D.rotateY((float)Math.toRadians(90.0));
        //tObj.obj3D.rotateZ((float)Math.toRadians(90.0));
        manageObjectCreationFromModel(tObj);

        //createCubesOnTheJPCTAxis();
        //createGroundPlane();
        /*
        Runnable r = new Runnable() {
            @Override
            public void run() {

                createGround();

            }
        };
        Thread t = new Thread(r);
        t.start();
        */

        //lastLocation = gpsLocator.requestLocationUpdate();
        //manageMovementUpdate();
        //world.getCamera().lookAt(fakeCubeOnZAxis.getTransformedCenter());
        //polyline disegnata sopra tutto il resto e non coinvolta nelle collisioni
        //Polyline p = new Polyline();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        setUpFrameBuffer(width, height);

    }



    @Override
    public void onDrawFrame(GL10 gl) {

        //check user interaction and reacts
        frameBuffer.clear(background);//set background
        //background is drawn first
        //this.setUpSkyBox();

        //perform lights and transformations to stored object
        //manageMovementUpdate();
        //manageGroundPositionUpdate();
        //manageObjectsPositionUpdate();
        //handleCameraPosition();
        handleCameraPositionSpherical();
        handleCameraRotations();
        //createCubesOnTheJPCTAxis();
        world.renderScene(frameBuffer);
         //the scene is drawn on the frame buffer.
        if(skybox!=null)
            skybox.render(world,frameBuffer);

        world.draw(frameBuffer);//also world.drawWireframe can be used to just draw the borders

        //stored image is presented onto the screen
        frameBuffer.display();
    }



    // transform gps-points to the correspending screen-points on the android device
    public void createCubesOnTheJPCTAxis(){

        world.removeAllObjects();
        final Double DISTANCE = 25.0;


        Double xx = DISTANCE;
        Double yx = 0.0;
        Double zx = 0.0;

        createPrimitiveCube("XAXIS", xx.floatValue(), yx.floatValue(), zx.floatValue());
        createPrimitiveCube("XAXISBACK", - xx.floatValue(), yx.floatValue(), zx.floatValue());

        Double xy = 0.0;
        Double yy = DISTANCE;
        Double zy = 0.0;

        createPrimitiveCube("YAXIS", xy.floatValue(), yy.floatValue(), zy.floatValue());
        createPrimitiveCube("YAXISBACK", xy.floatValue(), - yy.floatValue(), zy.floatValue());

        Double xz = 0.0;
        Double yz = 0.0;
        Double zz = DISTANCE;

        createPrimitiveCube("ZAXIS", xz.floatValue(), yz.floatValue(), zz.floatValue());
        createPrimitiveCube("ZAXISBACK", xz.floatValue(), yz.floatValue(), - zz.floatValue());



    }

    private Double toRad(Double value) {
        return value * Math.PI / 180;
    }


    //http://tutorial.math.lamar.edu/Classes/CalcIII/SphericalCoords.aspx
    public void manageObjectsCreation2(){

        world.removeAllObjects();

        /*
        zeroLoc=new Location("asobrero");
        zeroLoc.setLatitude(45.0808178);
        zeroLoc.setLongitude(7.6655203);
        zeroLoc.setAltitude(245);
        */

        int attempt = 10;
        while(zeroLoc==null && attempt>0) {
            //zeroLoc = gpsLocator.getLocation();
            zeroLoc=locationFusion.getAsynchBestLocationAmongLocators();
            attempt--;
        }



        if(zeroLoc!=null) {
            Double zeroDeltaLat = zeroLoc.getLatitude()/*-0.002*/;
            Double zeroDeltaLng = zeroLoc.getLongitude()/*-0.002*/;
            zeroLoc.setLatitude(zeroDeltaLat);
            zeroLoc.setLongitude(zeroDeltaLng);

            //Log.e("CAMERA POS", "x:"+xCAMERA+" y:"+yCAMERA+" z:"+zCAMERA);
            Log.e("ZEROLOC","altitude:"+zeroLoc.getAltitude());
            //locationFusion.smoothAltitudeValueThroughService(zeroLoc);
            for (String targetID : simulation.getTargetLocations().keySet()) {
                Location target = simulation.getTargetLocations().get(targetID);

                //
                //p stands for ro
                float p = zeroLoc.distanceTo(target);

                //float bearingAngleOfView = head + (-1 * pitch);
                //float azimuth = newLoc.bearingTo(zeroLoc) - bearingAngleOfView;
                //Log.e("bearingAngleOfView",bearingAngleOfView+"");

                float bearing = zeroLoc.bearingTo(target);
                Log.e("manageObjectsCreation2", "bearing:"+bearing+" id:"+targetID);
                float theta = X_TO_NORTH_ANGLE - bearing;// - bearingAngleOfView;
                //Log.e("bearing", theta+" "+targetID);
                Log.e("manageObjectsCreation2", "thetadeg:"+theta+" id:"+targetID);
                Double thetaD = toRad((double) theta);
                Log.e("manageObjectsCreation2", "thetarad:"+thetaD+" id:"+targetID);
                Double z = target.getAltitude() - zeroLoc.getAltitude();
                // z == p * Math.cos(phiD);
                // cosPhi = z/p
                //Log.e("handleCamPosSpherical", "z/p="+z.floatValue()+"/"+p);
                Double cosPhi;
                if(p>1)
                    cosPhi = z/p;
                else {
                    createPrimitiveCube(targetID, 0, 0, 0);
                    continue;
                }
                //Log.e("handleCamPosSpherical", "cosPhi="+cosPhi);
                //Double phiD = toRad((double) phi);
                Double phiD = Math.acos(cosPhi);

                //double r = p * Math.sin(phiD);
                //Log.e("isFacedown", facedown + "");

                //the code would be this...
                Double x = p * Math.sin(phiD) * Math.cos(thetaD);
                Double y = p * Math.sin(phiD) * Math.sin(thetaD);

                Log.e("manageObjectsCreation2", "x:"+x+" y:"+y+" z:"+z);

                createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());
            }
        }
        else{
            Log.e("JPCTWorldManager", "manageObjectsPositionUpdate: null location");
        }

    }

    // transform gps-points to the correspending screen-points on the android device
    /*
    mAzimuthView heading -> Z JPCT
    mPitchView -> x JPCT
    mRollView -> y JPCT
    */
    //distanza sull'asse
    //http://tutorial.math.lamar.edu/Classes/CalcIII/SphericalCoords.aspx
    public void manageObjectsCreation(){

        world.removeAllObjects();

        int attempt = 10;
        while(zeroLoc==null && attempt>0) {
            //zeroLoc = gpsLocator.getLocation();
            zeroLoc=locationFusion.getAsynchBestLocationAmongLocators();
            attempt--;
        }



        if(zeroLoc!=null) {
            Double zeroDeltaLat = zeroLoc.getLatitude()-0.05;
            Double zeroDeltaLng = zeroLoc.getLongitude()-0.05;
            zeroLoc.setLatitude(zeroDeltaLat);
            zeroLoc.setLongitude(zeroDeltaLng);

            for (String targetID : simulation.getTargetLocations().keySet()) {
                Location target = simulation.getTargetLocations().get(targetID);

                float ray = zeroLoc.distanceTo(target);
                float bearingAngleOfView = head + (-1 * pitch);
                float azimuth = zeroLoc.bearingTo(target) - bearingAngleOfView;
                Double azim = toRad((double) azimuth);
                float altitude = 90 + roll;

                //if (facedown) {
                if(roll>-90 && roll < 90){//looking down
                    altitude = altitude * -1;
                }

                Double alti = toRad((double) altitude);

                //Log.e("isFacedown", facedown + "");

                Double x = ray * Math.sin(azim);
                Double z = ray * Math.cos(alti) * Math.cos(azim);
                Double y = -1 * ray * Math.cos(azim) * Math.sin(alti);

                createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());
            }
        }
        else{
            Log.e("JPCTWorldManager", "manageObjectsPositionUpdate: null location");
        }

    }



    /*
    public void handleCameraPosition(){


        Location newLoc = gpsLocator.getLocation();

        if(newLoc!=null) {
            for (String targetID : worldObjects.keySet()) {
                //Location target = simulation.getTargetLocations().get(targetID);
                if(zeroLoc==null){
                    //Log.e("manageObjPositionUpdate", "id:"+targetID+" target=null,it shouldn't");
                    continue;
                }
                float ray = newLoc.distanceTo(zeroLoc);
                float bearingAngleOfView = head + (-1 * pitch);
                float azimuth = newLoc.bearingTo(zeroLoc) - bearingAngleOfView;
                Double azim = toRad((double) azimuth);
                float altitude = 90 + roll;

                //if (facedown) {
                if(roll>-90 && roll < 90){//looking down
                    altitude = altitude * -1;
                }

                Double alti = toRad((double) altitude);

                //Log.e("isFacedown", facedown + "");

                Double x = ray * Math.sin(azim);
                Double y = -1 * ray * Math.cos(azim) * Math.sin(alti);
                Double z = ray * Math.cos(alti) * Math.cos(azim);

                z=z+CAMERA_HEIGHT;
                //Log.e("CAMERA POSITION", "x:"+x+" y:"+y+" z:"+z);

                world.getCamera().setPosition(x.floatValue(),y.floatValue(),z.floatValue());

                //createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());
            }
        }
        else{
            Log.e("JPCTWorldManager", "manageObjectsPositionUpdate: null location");
        }

    }
    */


    //http://tutorial.math.lamar.edu/Classes/CalcIII/SphericalCoords.aspx
    public void handleCameraPositionSpherical(){
        //if(true)return;

        //Log.e("handleCamPosSpherical", "1");
        //Location currLoc = gpsLocator.getLocation();
        //Location gCurrLoc = googleServicesLocator.getCurrentLocation();

        Location currLoc = locationFusion.getBestLocationAmongLocators();

        if(currLoc==null)return;

        Location newLoc = new Location(currLoc);

        //locationFusion.smoothAltitudeValueThroughService(newLoc);


        if(newLoc!=null) {
                //Log.e("handleCamPosSpherical", "2");
                //Location target = simulation.getTargetLocations().get(targetID);
                if(zeroLoc==null){
                    //Log.e("handleCamPosSpherical", "zeroLOC==null");
                    //zeroLoc=gpsLocator.requestLocationUpdate();
                    return;
                }
                //p stands for ro
                float p = zeroLoc.distanceTo(newLoc);
                if(p<1)return;
                //float bearingAngleOfView = head + (-1 * pitch);
                //float azimuth = newLoc.bearingTo(zeroLoc) - bearingAngleOfView;
                //Log.e("bearingAngleOfView",bearingAngleOfView+"");

                float bearing = zeroLoc.bearingTo(newLoc);
                float theta = X_TO_NORTH_ANGLE - bearing;// - bearingAngleOfView;
                //Log.e("theta", theta+"");
                Double thetaD = toRad((double) theta);


                Double z = newLoc.getAltitude() - zeroLoc.getAltitude();
                // z == p * Math.cos(phiD);
                // cosPhi = z/p
                //Log.e("handleCamPosSpherical", "z/p="+z.floatValue()+"/"+p);
                Double cosPhi;
                if(p!=0)
                    cosPhi = z/p;
                else
                    cosPhi=0.0;
                //Log.e("handleCamPosSpherical", "cosPhi="+cosPhi);
                //Double phiD = toRad((double) phi);
                Double phiD = Math.acos(cosPhi);

                //double r = p * Math.sin(phiD);
                //Log.e("isFacedown", facedown + "");


                //the code would be this...
                Double x = p * Math.sin(phiD) * Math.cos(thetaD);
                Double y = p * Math.sin(phiD) * Math.sin(thetaD);

                z=z+CAMERA_HEIGHT;
                //Log.e("CAMERA POSITION", "x:"+x+" y:"+y+" z:"+z);


                if(true/*x.floatValue()-xCAMERA>=1 || y.floatValue()-yCAMERA>=1 || z.floatValue()-zCAMERA>=1*/) {

                    xCAMERA = x.floatValue();
                    yCAMERA = y.floatValue();
                    zCAMERA = z.floatValue();

                    world.getCamera().setPosition(xCAMERA, yCAMERA, zCAMERA);

                    TextureFromCameraActivity.MainHandler mainH = activity.getMainHandler();
                    if (mainH != null) {

                        //if(zeroLoc==null)
                        boolean zerolocisnull = zeroLoc==null;
                        String vcoors = "x:"+xCAMERA+"\n"+
                                        "y:"+yCAMERA+"\n"+
                                        "z:"+zCAMERA+"\n"+
                                        "accuracyRay="+newLoc.getAccuracy()+"\n"+
                                        "zerolocisNULL="+zerolocisnull+"\n";

                        vcoors=vcoors+"bearing: "+bearing+"\n";

                        if(!zerolocisnull){
                            vcoors=vcoors+"zeroloc altitude="+zeroLoc.getAltitude()+"\n";
                            vcoors=vcoors+"zerloc lat: "+zeroLoc.getLatitude()+"\n";
                            vcoors=vcoors+"zerloc lng: "+zeroLoc.getLongitude()+"\n";
                        }

                        mainH.sendVirtualCoordinates(vcoors);
                    }

                }
            //Log.e("handleCamPosSpherical", "cam pos:  "+x.floatValue()+" "+y.floatValue()+" "+z.floatValue() );
                //createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());

        }
        else{
            Log.e("handleCamPosSpherical", "newLoc null location");
        }

    }


    // transform gps-points to the correspending screen-points on the android device
    /*
    mAzimuthView heading -> Z JPCT
    mPitchView -> x JPCT
    mRollView -> y JPCT
    */
    //distanza sull'asse
    /*
    public void manageObjectsPositionUpdate(){


        Location myLoc = gpsLocator.getLocation();

        if(myLoc!=null) {
            for (String targetID : worldObjects.keySet()) {
                Location target = simulation.getTargetLocations().get(targetID);
                if(target==null){
                    //Log.e("manageObjPositionUpdate", "id:"+targetID+" target=null,it shouldn't");
                    continue;
                }
                float ray = myLoc.distanceTo(target);
                float bearingAngleOfView = head + (-1 * pitch);
                float azimuth = myLoc.bearingTo(target) - bearingAngleOfView;
                Double azim = toRad((double) azimuth);
                float altitude = 90 + roll;

                //if (facedown) {
                if(roll>-90 && roll < 90){//looking down
                    altitude = altitude * -1;
                }

                Double alti = toRad((double) altitude);

                //Log.e("isFacedown", facedown + "");

                Double x = ray * Math.sin(azim);
                Double z = ray * Math.cos(alti) * Math.cos(azim);
                Double y = -1 * ray * Math.cos(azim) * Math.sin(alti);

                TranslationObject obj = worldObjects.get(targetID);
                moveObjectToNewPosition(obj, x.floatValue(), y.floatValue(), z.floatValue());
                //createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());
            }
        }
        else{
            Log.e("JPCTWorldManager", "manageObjectsPositionUpdate: null location");
        }

    }

    public void manageGroundPositionUpdate(){


        Location myLoc = gpsLocator.getLocation();

        if(myLoc!=null) {

                //Location target = simulation.getTargetLocations().get(targetID);
                if(lastLocation==null){
                    //Log.e("manageObjPositionUpdate", "id:"+targetID+" target=null,it shouldn't");
                    return;
                }
                float ray = myLoc.distanceTo(lastLocation);
                float bearingAngleOfView = head + (-1 * pitch);
                float azimuth = myLoc.bearingTo(lastLocation) - bearingAngleOfView;
                Double azim = toRad((double) azimuth);
                float altitude = 90 + roll;

                //if (facedown) {
                if(roll>-90 && roll < 90){//looking down
                    altitude = altitude * -1;
                }

                Double alti = toRad((double) altitude);

                //Log.e("isFacedown", facedown + "");

                Double x = ray * Math.sin(azim);
                Double z = ray * Math.cos(alti) * Math.cos(azim);
                Double y = -1 * ray * Math.cos(azim) * Math.sin(alti);


                moveObjectToNewPosition(ground, groundTransformedCenter, x.floatValue(), y.floatValue(), z.floatValue());
                //createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());

        }
        else{
            Log.e("JPCTWorldManager", "manageObjectsPositionUpdate: null location");
        }


    }

    private void moveObjectToNewPosition(TranslationObject toBeMoved,float px, float py, float pz){

        Log.e("moveObjectToNewPosition","translation");
        float x = px-toBeMoved.x;
        float y = py-toBeMoved.y;
        float z = pz-toBeMoved.z;

        toBeMoved.obj.translate(x,y,z);

        toBeMoved.x=x;
        toBeMoved.y=y;
        toBeMoved.z=z;
    }

    private void moveObjectToNewPosition(Object3D toBeMoved, SimpleVector oldPos,float px, float py, float pz){

        Log.e("moveObjectToNewPosition","translation");
        float x = px-oldPos.x;
        float y = py-oldPos.y;
        float z = pz-oldPos.z;

        toBeMoved.translate(x,y,z);


        oldPos.x=x;
        oldPos.y=y;
        oldPos.z=z;

    }
    */


    /*
    // method used for debug purposes
    float grade = 0;
    public void panoramicView(){
        if(grade>=360)grade=0;
        world.getCamera().rotateCameraY(toRad(new Double(grade)).floatValue());
        grade++;
    }
    */





    //IPaintListener
    /*
        you have two methods that will be called before and after a frame is being drawn.
        i.e it can be used to count frames per second
    */



    private boolean landscape=true;
    private float[] rotationMatrix = null;
    public void setRotationMatrix(float[] mat){
        rotationMatrix=mat;
    }




    //http://stackoverflow.com/questions/14740808/android-problems-calculating-the-orientation-of-the-device
    /*
    public void onSensorChanged(SensorEvent event)
    {
        // It is good practice to check that we received the proper sensor event
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
        {
        // Convert the rotation-vector to a 4x4 matrix.
        SensorManager.getRotationMatrixFromVector(mRotationMatrix,
                event.values);
        SensorManager
                .remapCoordinateSystem(mRotationMatrix,
                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
                        mRotationMatrix);
        SensorManager.getOrientation(mRotationMatrix, orientationVals);

        // Optionally convert the result from radians to degrees
        orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
        orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
        orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);

        tv.setText(" Yaw: " + orientationVals[0] + "\n Pitch: "
                + orientationVals[1] + "\n Roll (not used): "
                + orientationVals[2]);

        }
    }
    */
    public void handleCameraRotations(){

        if(rotationMatrix==null)return;

        //Log.e("handleCameraRotations()", "HANDLED");
        Camera cam = world.getCamera();

        if (landscape) {
            // in landscape mode first remap the
            // rotationMatrix before using
            // it with camera.setBack:
            float[] result = new float[9];
            //SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_MINUS_X, result);
            // -Y of jpct cooresponds to X of sensor system -> x sensor = -y jpct = east
            // -X of jpct cooresponds to Y of sensor system -> y sensor = -x jpct = nord
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_MINUS_X, result);
            com.threed.jpct.Matrix mResult = new com.threed.jpct.Matrix();
            copyMatrix(result, mResult);
            cam.setBack(mResult);
            //Log.e("Camera is looking at:")

        } else {
            // WARNING: This solution doesn't work in portrait mode
            // See the explanation below
        }

    }





    private void copyMatrix(float[] src, com.threed.jpct.Matrix dest) {
        if(src.length==9) {
            dest.setRow(0, src[0], src[1], src[2], 0);
            dest.setRow(1, src[3], src[4], src[5], 0);
            dest.setRow(2, src[6], src[7], src[8], 0);
            dest.setRow(3, 0f, 0f, 0f, 1f);
        }
        else if(src.length==16){
            dest.setRow(0, src[0], src[1], src[2], 0);
            dest.setRow(1, src[3], src[4], src[5], 0);
            dest.setRow(2, src[6], src[7], src[8], 0);
            dest.setRow(3, 0f, 0f, 0f, 1f);
            //dest.setRow(0, src[0], src[1], src[2], src[3]);
            //dest.setRow(1, src[4], src[5], src[6], src[7]);
            //dest.setRow(2, src[8], src[9], src[10],src[11]);
            //dest.setRow(3, src[12],src[13],src[14],src[15]);
        }
    }


    int axis = 2;
    public void getCameraPosition(){




        SimpleVector s = world.getCamera().getUpVector();
        Log.e("JPCT:getUpVector", "x:"+s.x+" y:"+s.y+" z:"+s.z);

        //axis++;
    }


    // Convert transparentColor to be transparent in a Bitmap.
    public static Bitmap makeTransparent(Bitmap bit) {
        int width =  bit.getWidth();
        int height = bit.getHeight();
        Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int [] allpixels = new int [ myBitmap.getHeight()*myBitmap.getWidth()];
        bit.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(),myBitmap.getHeight());
        myBitmap.setPixels(allpixels, 0, width, 0, 0, width, height);

        for(int i =0; i<myBitmap.getHeight()*myBitmap.getWidth();i++){
            //if( allpixels[i] == transparentColor)

                allpixels[i] = Color.alpha(Color.TRANSPARENT);
        }

        myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
        return myBitmap;
    }

    private class TranslationObject{
        public String objID;
        public Object3D obj;
        public float x, y, z;

        public TranslationObject(String objID, Object3D obj, float x, float y, float z){

            this.objID=objID;
            this.obj=obj;
            this.x=x;
            this.y=y;
            this.z=z;

        }



    }

}
