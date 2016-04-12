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
    private GLSurfaceView mGLView;
    //private MyRenderer renderer = null;
    private FrameBuffer frameBuffer = null;


    private int fps = 0;

    //boolean to set whether gles2 is available..?
    private boolean gl2 = true;


    private TextureFromCameraActivity activity;
    private SimParameters simulation;
    private GPSLocator gpsLocator;




    /*
    JPCT COORDINATES
    positive axis:
    x -> right of the screen
    y -> down to the screen
    z -> inside the screen
    */


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
    private Object3D fakeCubeOnZAxis = Primitives.getCube(1);


    private SkyBox sky;
    private void setUpSkyBox(){
        sky.setCenter(world.getCamera().getPosition());
        sky.render(world,frameBuffer);
    }

    public void setUpFrameBuffer(int width, int height){

        if(frameBuffer!=null)frameBuffer.dispose();

        frameBuffer=new FrameBuffer(width, height);

    }


    //to be invoked in onSurfaceCreated
    public void createPrimitiveCube(String id, float x, float y, float z){

        Log.d("object:"+id, "CREATED at:"+x+" y:"+y+" z:"+z);
        TextureManager txtManager = TextureManager.getInstance();
        Texture txt;
        if(!txtManager.containsTexture(id)) {
            //txtManager.removeTexture(id);
            if(id.equals("rosina")){
                txt = new Texture(64,64,RGBColor.RED);
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
            else
                txt = new Texture(64,64,RGBColor.BLUE);

            txtManager.addTexture(id, txt);
        }
        else
            txt = txtManager.getTexture(id);

        int dim = 8;//(int) (500 / z);

        Object3D cube = Primitives.getCube(dim);

        cube.translate(x, y, z);
        cube.setTexture(id);
        cube.setName(id);
        world.addObject(cube);

    }

    //GLSurfaceView.Renderer methods onSurfaceCreated onSurfaceChanged onDrawFrame
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        Log.d("ONSURFACECREATED", "CALL");
        setUpWorld();

        manageMovementUpdate();
        world.getCamera().lookAt(fakeCubeOnZAxis.getTransformedCenter());
        //polyline disegnata sopra tutto il resto e non coinvolta nelle collisioni
        //Polyline p = new Polyline();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        setUpFrameBuffer(width, height);
    }




    public float rollCam=0,pitchCam=0,headCam=0;
    private boolean facedown = false;
    public void setRPHCam(float r,float p,float h,boolean fdown){
            facedown=fdown;
            rollCam=r;
            pitchCam=p;
            headCam=h;

    }
    @Override
    public void onDrawFrame(GL10 gl) {

        //check user interaction and reacts
        RGBColor transp = new RGBColor(0,0,0,0);//transparent background
        frameBuffer.clear(transp);

        //background is drawn first
        //this.setUpSkyBox();

        //perform lights and transformations to stored object
        manageMovementUpdate();
        orientateCamera(rollCam, pitchCam, headCam);

        //cube.rotateX(0.1f);


        //this.panoramicView();

        world.renderScene(frameBuffer);
        //the scene is drawn on the frame buffer.
        world.draw(frameBuffer);//also world.drawWireframe can be used to just draw the borders

        //stored image is presented onto the screen
        frameBuffer.display();
    }



    private Double toRad(Double value) {
        return value * Math.PI / 180;
    }

    //R = earth’s radius (mean radius = 6371km)
    private static final int R = 6371000;//result will be in meters

    /*
    translation attempt

    public void manageMovementUpdate(){

        Location myLoc = gpsLocator.getLocation();
        Object3D temp=null;
        if(myLoc!=null) {
            for (String targetID : simulation.getTargetLocations().keySet()) {
                Location target = simulation.getTargetLocations().get(targetID);

                float ray = myLoc.distanceTo(target);
                float bearingAngleOfView = headCam + (-1 * rollCam);
                float azimuth = myLoc.bearingTo(target) - bearingAngleOfView;
                Double azim = toRad((double) azimuth);
                float altitude = 90 + pitchCam;

                if (facedown) {
                    altitude = altitude * -1;
                }

                Double alti = toRad((double) altitude);


                Log.e("isFacedown", facedown + "");


                Double x = ray * Math.sin(azim);
                Double z = ray * Math.cos(alti) * Math.cos(azim);
                Double y = -1 * ray * Math.cos(azim) * Math.sin(alti);

                if (targetID.equals("pirandello10")) {
                    Log.e("id:" + targetID + "XYZ:", x.floatValue() + " " + y.floatValue() + " " + z.floatValue() + " ");
                    //Log.e("lat:" + myLoc.getLatitude(), "long:" + myLoc.getLongitude());
                    Log.e("myLoc-targ:distTo():", myLoc.distanceTo(target) + "");
                    Log.e("head:", headCam + "");
                    Log.e("BEARING-head:", azimuth + "");
                    Log.e("altitude", altitude + "");
                }

                temp = world.getObjectByName(targetID);
                if(temp==null)
                    createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());
                else {
                    Log.e("object","translated");
                    world.getObjectByName(targetID).translate(x.floatValue(), y.floatValue(), z.floatValue());
                }
            }
        }

    }
     */


    // transform gps-points to the correspending screen-points on the android device
    public void manageMovementUpdate(){

        world.removeAllObjects();
        //createPrimitiveCube("xaxis", 5, 0, 0);
        //createPrimitiveCube("yaxis",0,5,0);
        //createPrimitiveCube("zaxis",0,0,5);
        Location myLoc = gpsLocator.getLocation();

        if(myLoc!=null) {
            for (String targetID : simulation.getTargetLocations().keySet()) {
                Location target = simulation.getTargetLocations().get(targetID);

                float ray = myLoc.distanceTo(target);
                float bearingAngleOfView = headCam + (-1 * rollCam);
                float azimuth = myLoc.bearingTo(target) - bearingAngleOfView;
                Double azim = toRad((double) azimuth);
                float altitude = 90 + pitchCam;

                if (facedown) {
                    altitude = altitude * -1;
                }

                Double alti = toRad((double) altitude);


                Log.e("isFacedown", facedown + "");


                Double x = ray * Math.sin(azim);
                Double z = ray * Math.cos(alti) * Math.cos(azim);
                Double y = -1 * ray * Math.cos(azim) * Math.sin(alti);

                if (targetID.equals("pirandello10")) {
                    Log.e("id:" + targetID + "XYZ:", x.floatValue() + " " + y.floatValue() + " " + z.floatValue() + " ");
                    //Log.e("lat:" + myLoc.getLatitude(), "long:" + myLoc.getLongitude());
                    Log.e("myLoc-targ:distTo():", myLoc.distanceTo(target) + "");
                    Log.e("head:", headCam + "");
                    Log.e("BEARING-head:", azimuth + "");
                    Log.e("altitude", altitude + "");
                }
                createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());
                //world.getObjectByName(targetID).translate(x,y,z);
            }
        }

    }


    /*
    //R = earth’s radius (mean radius = 6371km)
    private static final int R = 6371000;//result will be in meters
    // transform gps-points to the correspending screen-points on the android device
    public void manageMovementUpdate(){
        world.removeAllObjects();
        Location myLoc = gpsLocator.getLocation();
        if(myLoc!=null)
            for(String targetID : simulation.getTargetLocation().keySet()){
                Location target = simulation.getTargetLocation().get(targetID);

                double r = target.getAltitude() + R;

                Double targLat = toRad(target.getLatitude());
                Double targLon = toRad(target.getLongitude());
                Double xTarget = r * Math.cos(targLat) * Math.cos(targLon);
                Double yTarget = r * Math.cos(targLat) * Math.sin(targLon);
                Double zTarget = r * Math.sin(targLat);


                Double myLat = toRad(myLoc.getLatitude());
                Double myLon = toRad(myLoc.getLongitude());
                Double xMy = r * Math.cos(myLat) * Math.cos(myLon);
                Double yMy = r * Math.cos(myLat) * Math.sin(myLon);
                Double zMy = r * Math.sin(myLat);

                Double x = xTarget - xMy;//-Y
                Double y = yTarget - yMy;//Z
                Double z = zTarget - zMy;//X

                Log.d("id:"+targetID+"XYZ:",x.floatValue()+" "+y.floatValue()+" "+z.floatValue()+" ");
                Log.d("lat:"+myLoc.getLatitude(), "long:"+myLoc.getLongitude());
                Log.d("myLoc-targ:distTo():",myLoc.distanceTo(target)+"");
                Log.d("xTarget:"+xTarget," xMy:"+xMy);
                Log.d("yTarget:"+yTarget," yMy:"+yMy);
                Log.d("zTarget:" + zTarget, " zMy:" + zMy);
                Log.d("distance from "+targetID, "myLoc: "+target.distanceTo(myLoc));
                //createPrimitiveCube(targetID, x.floatValue(), y.floatValue(), z.floatValue());
                //createPrimitiveCube(targetID, -z.floatValue(), -x.floatValue(), y.floatValue());
                createPrimitiveCube(targetID, y.floatValue(), -z.floatValue(), x.floatValue());
                //world.getObjectByName(targetID).translate(x,y,z);
            }

    }
    */
    float[] orMatrix;
    public void remapCoors(float[] mResult){
        orMatrix=mResult;

    }





    float grade = 0;
    public void panoramicView(){
        if(grade>=360)grade=0;
        world.getCamera().rotateCameraY(toRad(new Double(grade)).floatValue());
        grade++;
    }
    //mAzimuthView heading -> Z JPCT
    //mPitchView -> x JPCT
    //mRollView -> y JPCT
    public void orientateCamera(float roll,float pitch,float head){



        Double r = new Double(-roll);
        Double p = new Double(90 + pitch);
        Double h = new Double(head);


        Log.d("rolling camera: r:", roll + " p:" + pitch + " h:"+head);
        //SimpleVector orientationVect = new SimpleVector(x,y,z);
        /*
        Camera cam = world.getCamera();
        cam.getBack().setIdentity();
        cam.rotateCameraAxis(new SimpleVector(0,1,0),toRad(zD).floatValue());
        cam.rotateCameraAxis(new SimpleVector(1,0,0), toRad(xD).floatValue());
        cam.rotateCameraAxis(new SimpleVector(0, 0, 1), toRad(yD).floatValue());
        */

        Camera cam = world.getCamera();

        //cam.setFovAngle(toRad(45.0).floatValue());
        cam.lookAt(fakeCubeOnZAxis.getTransformedCenter());
        /*
        cam.getBack().setIdentity();
        cam.rotateCameraAxis(new SimpleVector(0, 1, 0), toRad(r).floatValue());
        //cam.rotateCameraAxis(new SimpleVector(0, 1, 0), toRad(h).floatValue());
        if(!facedown){
            cam.rotateCameraAxis(new SimpleVector(1, 0, 0), toRad(p).floatValue());
            activity.showOrientationVirtualCameraAngle(r.floatValue(),p.floatValue(),h.floatValue());
        }
        else {
            cam.rotateCameraAxis(new SimpleVector(1, 0, 0), -toRad(p).floatValue());
            activity.showOrientationVirtualCameraAngle(r.floatValue(),-p.floatValue(),h.floatValue());
        }

        */
        //cam.rotateCameraAxis(new SimpleVector(0, 0, 1), toRad(h).floatValue());

        //cam.rotateCameraY(toRad(r).floatValue());
        //cam.rotateCameraX(toRad(p).floatValue());
        //cam.rotateCameraZ(toRad(h).floatValue());

        //cam.rotateCameraY(r.floatValue());
        //cam.rotateCameraX(p.floatValue());
        //cam.rotateCameraZ(h.floatValue());

    }


    /*

    public void moveCamera(){
        SimpleVector backVect = cube.getTransformedCenter();
        backVect.scalarMul(-1.0f);
        rotationmatrix.translate(backVect);
        rotationmatrix.rotateY(touchTurn);
        rotationmatrix.translate(cube.getTransformedCenter());
        Camera cam = world.getCamera();
        cam.moveCamera(Camera.CAMERA_MOVEOUT, 50);
        cam.lookAt(cube.getTransformedCenter());
        cam.moveCamera(,30);
    }
    */


    //IPaintListener
    /*
        you have two methods that will be called before and after a frame is being drawn.
        i.e it can be used to count frames per second
    */


    /*

    public void moveCamera(){
        Camera cam = world.getCamera();
        cam.moveCamera(Camera.CAMERA_MOVEOUT, 50);
        cam.lookAt(cube.getTransformedCenter());
    }
    */
}
