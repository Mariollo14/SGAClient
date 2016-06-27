package com.android.libraries;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.threed.jpct.Camera;
import com.threed.jpct.Config;
import com.threed.jpct.*;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Logger;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;
import com.threed.jpct.util.SkyBox;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Mario Salierno on 21/03/2016.
 */
public class JPCTWorldManager implements GLSurfaceView.Renderer{

    private World world = null;
    //private World sky = null;
    private Light sun = null;
    private Object3D fakeCubeOnZAxis = Primitives.getCube(1);
    RGBColor background = new RGBColor(0,0,0,0);//transparent background
    private FrameBuffer frameBuffer = null;

    //boolean to set whether gles2 is available..?
    private boolean gl2 = true;

    private TextureFromCameraActivity activity;
    private GLSurfaceView mGLView;
    private SimParameters simulation;
    private GPSLocator gpsLocator;

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

    public JPCTWorldManager(TextureFromCameraActivity activity, SimParameters simulation, GPSLocator gpsLocator) {
        this.activity = activity;
        this.simulation=simulation;
        this.gpsLocator=gpsLocator;
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
        Config.farPlane = 4000;
        //Config.glShadowZBias = 0.8f;
        //Config.lightMul = 1;

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
        world.getCamera().setPosition(0,0,0);

        /*
            Two worlds because one is for the scene itself and one is for the sky dome.
            This makes it much easier to handle the dome, but you don't have to do it this way.
            Adding the dome to the scene itself would work too.
        */
        //sky = new World();
        //sky.setAmbientLight(255, 255, 255);

        //world.getLights().setRGBScale(Lights.RGB_SCALE_2X);
        //sky.getLights().setRGBScale(Lights.RGB_SCALE_2X);

        fakeCubeOnZAxis.translate(0,0,10);
        world.addObject(fakeCubeOnZAxis);
    }


    private SkyBox sky;
    private void setUpSkyBox(){
        sky.setCenter(world.getCamera().getPosition());
        sky.render(world,frameBuffer);
    }

    public void setUpFrameBuffer(int width, int height){

        if(frameBuffer!=null)frameBuffer.dispose();

        frameBuffer=new FrameBuffer(width, height);

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

    }



    //GLSurfaceView.Renderer methods onSurfaceCreated onSurfaceChanged onDrawFrame
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        setUpWorld();


        manageObjectsPositionUpdate();
        createCubesOnTheJPCTAxis();
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

        //orientateCameraAlongZ();
        /*
        if(axis>2)axis=0;

        if(axis==0)
            world.getCamera().lookAt(world.getObjectByName("XAXIS").getTransformedCenter());
        if(axis==1)
            world.getCamera().lookAt(world.getObjectByName("YAXIS").getTransformedCenter());
        if(axis==2)
            world.getCamera().lookAt(world.getObjectByName("ZAXIS").getTransformedCenter());
        */
        handleCameraRotations();

        world.renderScene(frameBuffer);
        //the scene is drawn on the frame buffer.
        world.draw(frameBuffer);//also world.drawWireframe can be used to just draw the borders

        //stored image is presented onto the screen
        frameBuffer.display();
    }



    // transform gps-points to the correspending screen-points on the android device
    public void createCubesOnTheJPCTAxis(){


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

    // transform gps-points to the correspending screen-points on the android device
    /*
    mAzimuthView heading -> Z JPCT
    mPitchView -> x JPCT
    mRollView -> y JPCT
    */
    //distanza sull'asse
    public void manageObjectsPositionUpdate(){

        world.removeAllObjects();

        Location myLoc = gpsLocator.getLocation();

        if(myLoc!=null) {
            for (String targetID : simulation.getTargetLocations().keySet()) {
                Location target = simulation.getTargetLocations().get(targetID);

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

                createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());
            }
        }
        else{
            Log.e("JPCTWorldManager", "manageObjectsPositionUpdate: null location");
        }

    }

    /*
    // transform gps-points to the correspending screen-points on the android device
    public void manageMovementUpdate(){

        world.removeAllObjects();

        Location myLoc = gpsLocator.getLocation();

        if(myLoc!=null) {
            for (String targetID : simulation.getTargetLocations().keySet()) {
                Location target = simulation.getTargetLocations().get(targetID);

                float ray = myLoc.distanceTo(target);
                float bearingAngleOfView = head + (-1 * roll);
                float azimuth = myLoc.bearingTo(target) - bearingAngleOfView;
                Double azim = toRad((double) azimuth);
                float altitude = 90 + pitch;

                if (facedown) {
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

    public void orientateCameraAlongZ(){

        Camera cam = world.getCamera();
        cam.lookAt(fakeCubeOnZAxis.getTransformedCenter());

    }




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

    public void handleCameraRotations(){

        if(rotationMatrix==null)return;

        //Log.e("handleCameraRotations()", "HANDLED");
        Camera cam = world.getCamera();

        if (landscape) {
            // in landscape mode first remap the
            // rotationMatrix before using
            // it with camera.setBack:
            float[] result = new float[9];
            SensorManager.remapCoordinateSystem(
                    rotationMatrix, SensorManager.AXIS_MINUS_Y,
                                    SensorManager.AXIS_MINUS_X, result);
            com.threed.jpct.Matrix mResult = new com.threed.jpct.Matrix();
            copyMatrix(result, mResult);
            cam.setBack(mResult);
        } else {
            // WARNING: This solution doesn't work in portrait mode
            // See the explanation below
        }

    }

    private void copyMatrix(float[] src, com.threed.jpct.Matrix dest) {
        dest.setRow(0, src[0], src[1], src[2],   0);
        dest.setRow(1, src[3], src[4], src[5],   0);
        dest.setRow(2, src[6], src[7], src[8],   0);
        dest.setRow(3,     0f,     0f,     0f,  1f);
    }


    int axis = 2;
    public void getCameraPosition(){




        SimpleVector s = world.getCamera().getUpVector();
        Log.e("JPCT:getUpVector", "x:"+s.x+" y:"+s.y+" z:"+s.z);

        //axis++;
    }

}
