package com.android.libraries;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.SensorListener;
import android.net.wifi.WifiManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.libraries.gles.Drawable2d;
import com.android.libraries.gles.EglCore;
import com.android.libraries.gles.GlUtil;
import com.android.libraries.gles.Sprite2d;
import com.android.libraries.gles.Texture2dProgram;
import com.android.libraries.gles.WindowSurface;
import com.android.libraries.location.GPSLocator;
import com.android.libraries.location.GeoLocator;
import com.android.libraries.location.GoogleServicesLocator;
import com.android.libraries.location.LocationFusionStrategy;
import com.android.libraries.location.kalman.KalmanLocator;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import android.hardware.Camera.PreviewCallback;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import teaonly.droideye.MediaBlock;

/**
 * Direct the Camera preview to a GLES texture and manipulate it.
 * <p/>
 * We manage the Camera and GLES rendering from a dedicated thread.  We don't animate anything,
 * so we don't need a Choreographer heartbeat -- just redraw when we get a new frame from the
 * camera or the user has caused a change in size or position.
 * <p/>
 * The Camera needs to follow the activity pause/resume cycle so we don't keep it locked
 * while we're in the background.  Also, for power reasons, we don't want to keep getting
 * frames when the screen is off.  As noted in
 * http://source.android.com/devices/graphics/architecture.html#activity
 * the Surface lifecycle isn't quite the same as the activity's.  We follow approach #1.
 * <p/>
 * The tricky part about the lifecycle is that our SurfaceView's Surface can outlive the
 * Activity, and we can get surface callbacks while paused, so we need to keep track of it
 * in a static variable and be prepared for calls at odd times.
 * <p/>
 * The zoom, size, and rotate values are determined by the values stored in the "seek bars"
 * (sliders).  When the device is rotated, the Activity is paused and resumed, but the
 * controls retain their value, which is kind of nice.  The position, set by touch, is lost
 * on rotation.
 * <p/>
 * The UI updates go through a multi-stage process:
 * <ol>
 * <li> The user updates a slider.
 * <li> The new value is passed as a percent to the render thread.
 * <li> The render thread converts the percent to something concrete (e.g. size in pixels).
 * The rect geometry is updated.
 * <li> (For most things) The values computed by the render thread are sent back to the main
 * UI thread.
 * <li> (For most things) The UI thread updates some text views.
 * </ol>
 */
public class TextureFromCameraActivity extends Activity
        implements SurfaceHolder.Callback/*,SeekBar.OnSeekBarChangeListener*/ {
    public static final String TAG = "TextureCameraActivity";/////MainActivity.TAG;


    private static final int DEFAULT_ZOOM_PERCENT = 0;      // 0-100
    private static final int DEFAULT_SIZE_PERCENT = 100;     // 0-100
    private static final int DEFAULT_ROTATE_PERCENT = 0;    // 0-100

    // Requested values; actual may differ.
    private static final int REQ_CAMERA_WIDTH = 640;//1280;
    private static final int REQ_CAMERA_HEIGHT = 480;//720;
    private static final int REQ_CAMERA_FPS = 30;//20;


    // The holder for our SurfaceView.  The Surface can outlive the Activity (e.g. when
    // the screen is turned off and back on with the power button).
    //
    // This becomes non-null after the surfaceCreated() callback is called, and gets set
    // to null when surfaceDestroyed() is called.
    private static SurfaceHolder sSurfaceHolder;

    private SurfaceView cameraView = null;
    private TextView tmptv = null, LOCview = null, IPview = null, virtualCoorsView = null;
    private GLSurfaceView mGLView;
    private boolean gl2 = true;
    private JPCTWorldManager jpctWorldManager = null;
    private SimParameters simulation = null;

    // Thread that handles rendering and controls the camera.  Started in onResume(),
    // stopped in onPause().
    private RenderThread mRenderThread;

    // Receives messages from renderer thread.
    private MainHandler mHandler;


    public MainHandler getMainHandler(){
        return mHandler;
    }
    /*
    // User controls.
    private SeekBar mZoomBar;
    private SeekBar mSizeBar;
    private SeekBar mRotateBar;
    */
    // These values are passed to us by the camera/render thread
    private int mCameraPreviewWidth, mCameraPreviewHeight;
    private float mCameraPreviewFps;
    private int mRectWidth, mRectHeight;
    private int mZoomWidth, mZoomHeight;
    private int mRotateDeg;


    private Double xG, yG, zG;
    //private GPSLocator gpsLocator;
    //private GoogleServicesLocator googleLocator;
    private LocationFusionStrategy locationFusion;
    //private SensorFusion mySensorFusion;
    private SensorHandler sensorHandler;


    //EYE
    //private final int ServerPort = 8080;
    private final int StreamingPort = 8088;
    private final int PictureWidth = 640;//480;
    private final int PictureHeight = 480;//360;
    private static final int MediaBlockNumber = 6;
    private static final int MediaBlockSize = 131072;//1024 * 512;//
    private final int EstimatedFrameNumber = 1;//30;
    private final int StreamingInterval = 10;//100;
    // EYE
    private StreamingServer streamingServer = null;
    //ExecutorService executor = Executors.newFixedThreadPool(3);
    //VideoEncodingTask videoTask = new VideoEncodingTask();
    private ReentrantLock previewLock = new ReentrantLock();
    boolean inProcessing = false;
    byte[] yuvFrame = new byte[1920 * 1280 * 2];
    private static MediaBlock[] mediaBlocks = new MediaBlock[MediaBlockNumber];
    int mediaWriteIndex = 0;
    int mediaReadIndex = 0;
    private Handler streamingHandler;
    private StreamingThread streamingThread;


    //EYE class CameraView fragment
    private List<int[]> supportedFrameRate;
    private List<Camera.Size> supportedSizes;
    private Camera.Size procSize_;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        simulation = new SimParameters();
        GPSLocator gpsLocator = new GPSLocator(this, simulation);
        GoogleServicesLocator googleLocator = new GoogleServicesLocator(this,false);
        //KalmanLocator kalmanLocator = new KalmanLocator(this.getApplicationContext(),this);

        //ArrayList<GeoLocator> locators = new ArrayList<GeoLocator>();
        //locators.add(gpsLocator);
        //locators.add(googleLocator);
        //locators.add(kalmanLocator);
        locationFusion = new LocationFusionStrategy(this);
        locationFusion.addGeoLocator(gpsLocator,false);
        locationFusion.addGeoLocator(googleLocator,false);


        //mySensorFusion = new SensorFusion(this);
        sensorHandler = new SensorHandler(this);

        jpctWorldManager = new JPCTWorldManager(this, simulation, locationFusion, 0);

        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mHandler = new MainHandler(this);

        setContentView(R.layout.activity_texture_from_camera);

        cameraView = (SurfaceView) this.findViewById(R.id.surfaceViewCamera);//new SurfaceView(this);
        SurfaceHolder sh = cameraView.getHolder();
        sh.addCallback(this);
        sh.setFormat(PixelFormat.TRANSLUCENT);

        tmptv = (TextView) this.findViewById(R.id.orientationTV);//new TextView(this);
        tmptv.setBackgroundColor(PixelFormat.OPAQUE);

        mGLView = (GLSurfaceView) this.findViewById(R.id.glsSurfaceViewOnTopOfCamera);//new GLSurfaceView(this);
        SurfaceHolder GLSsfOnTopHolder = mGLView.getHolder();
        GLSsfOnTopHolder.setFormat(PixelFormat.TRANSLUCENT);

        if (gl2) {
            mGLView.setEGLContextClientVersion(2);
        } else {
             /*
                The creation of a dedicated EGLConfigChooser:
                This serves one single purpose, which is to make 3D acceleration on old phones.
                Almost no device requires this nowadays but it doesn't hurt either.
            */
            mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
                public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                    // Ensure that we get a 16bit framebuffer. Otherwise, we'll
                    // fall back to Pixelflinger on some device (read: Samsung
                    // I7500). Current devices usually don't need this, but it
                    // doesn't hurt either.
                    int[] attributes = new int[]{EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE};
                    EGLConfig[] configs = new EGLConfig[1];
                    int[] result = new int[1];
                    egl.eglChooseConfig(display, attributes, configs, 1, result);
                    return configs[0];
                }
            });


        }

        // Translucent window 8888 pixel format and depth buffer
        mGLView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mGLView.setRenderer(jpctWorldManager);

        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mGLView.setZOrderOnTop(true);

        //setContentView(R.layout.activity_texture_from_camera);

        LOCview = (TextView)this.findViewById(R.id.localizationTV);
        LOCview.setBackgroundColor(PixelFormat.OPAQUE);
        //LOCview.setText("LOCALIZATION");
        LOCview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Location l = gpsLocator.requestLocationUpdate();
                //gpsLocator.onLocationChanged(l);

                locationFusion.getAsynchBestLocationAmongLocators();

            }
        });
        locationFusion.getAsynchBestLocationAmongLocators();


        IPview = (TextView)this.findViewById(R.id.ipaddressTV);
        IPview.setBackgroundColor(PixelFormat.OPAQUE);
        //IPview.setText("IPADDRESS");


        virtualCoorsView = (TextView)this.findViewById(R.id.virtualCoorsTV);
        virtualCoorsView.setBackgroundColor(PixelFormat.OPAQUE);
        //virtualCoorsView.setText("Coordinates");




        /*
        RelativeLayout rl = (RelativeLayout) this.findViewById(R.id.relLay);
        // inflate content layout and add it to the relative layout as second child
        // add as second child, therefore pass index 1 (0,1,...)

        rl.addView(cameraView);
        rl.addView(mGLView);

        RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        relativeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        rl.addView(tmptv);

        LOCview = new TextView(this);
        LOCview.setBackgroundColor(PixelFormat.OPAQUE);
        LOCview.setText("LOCALIZATION");

        rl.addView(LOCview);

        IPview = new TextView(this);
        IPview.setBackgroundColor(PixelFormat.OPAQUE);
        IPview.setText("IPADDRESS");

        rl.addView(IPview);
        */


        showIpAddress();


        try {
            streamingServer = new StreamingServer(StreamingPort);
            streamingServer.start();
            //Log.e("Server Started:", streamingServer.getAddress().getHostString());


        } catch (UnknownHostException e) {
            return;
        }



        streamingThread = new StreamingThread();
        streamingThread.start();

        videoEncoderThread = new VideoEncoderThread();
        //videoEncoderThread.start();

        /*
        streamingHandler = new Handler();

        streamingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                doStreaming();
            }
        }, StreamingInterval);
        */


    }



    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }


    @Override
    protected void onResume() {

        super.onResume();


        mGLView.onResume();
        locationFusion.resumeLocators();
        //gpsLocator.requestLocationUpdate();

        //googleLocator.setRequestingLocationUpdates(true);

        /*
        if (googleLocator.isConnected() && !googleLocator.isRequestingLocationUpdates()) {
            googleLocator.startLocationUpdates();
        }
        */

        //mySensorFusion.initListeners();
        sensorHandler.initListeners();
        //try catch mario
        //try {

        mRenderThread = new RenderThread(mHandler);
        mRenderThread.setName("TexFromCam Render");
        mRenderThread.start();
        mRenderThread.waitUntilReady();

        RenderHandler rh = mRenderThread.getHandler();

            /*
            mario
            rh.sendZoomValue(mZoomBar.getProgress());
            rh.sendSizeValue(mSizeBar.getProgress());
            rh.sendRotateValue(mRotateBar.getProgress());
            */
        if (sSurfaceHolder != null) {
            rh.sendSurfaceAvailable(sSurfaceHolder, false);
        } else {
            Log.d(TAG, "No previous surface");
        }

        //} catch (Exception e) {
        // Show toast to the user
        //Toast.makeText(getApplicationContext(), "Data lost due to excess use of other apps", Toast.LENGTH_LONG).show();
        //}

        /*
        try {
            streamingServer = new StreamingServer(StreamingPort);
            streamingServer.start();
            Log.e("Server Started:", streamingServer.getAddress().getHostString());


        } catch (UnknownHostException e) {
            return;
        }
        */

    }

    @Override
    protected void onPause() {

        mGLView.onPause();
        //gpsLocator.stopUsingGPS();
        //googleLocator.stopLocationUpdates();

        locationFusion.pauseLocators();
        //mySensorFusion.unregisterListeners();
        sensorHandler.unregisterListeners();

        super.onPause();

        if (mRenderThread != null) {
            RenderHandler rh = mRenderThread.getHandler();
            rh.sendShutdown();
            try {
                mRenderThread.join();
            } catch (InterruptedException ie) {
                // not expected
                throw new RuntimeException("join was interrupted", ie);
            }
            mRenderThread = null;
        }
    }



    @Override   // SurfaceHolder.Callback
    public void surfaceCreated(SurfaceHolder holder) {

        if (sSurfaceHolder != null) {
            throw new RuntimeException("sSurfaceHolder is already set");
        }
        sSurfaceHolder = holder;

        if (mRenderThread != null) {
            // Normal case -- render thread is running, tell it about the new surface.
            RenderHandler rh = mRenderThread.getHandler();
            rh.sendSurfaceAvailable(holder, true);
        } else {
            // Sometimes see this on 4.4.x N5: power off, power on, unlock, with device in
            // landscape and a lock screen that requires portrait.  The surface-created
            // message is showing up after onPause().
            //
            // Chances are good that the surface will be destroyed before the activity is
            // unpaused, but we track it anyway.  If the activity is un-paused and we start
            // the RenderThread, the SurfaceHolder will be passed in right after the thread
            // is created.
            Log.e(TAG, "render thread not running");
        }


    }

    @Override   // SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Log.e(TAG, "surfaceChanged format=" + format + " w=" + width + " h=" + height);

        if (mRenderThread != null) {
            RenderHandler rh = mRenderThread.getHandler();
            rh.sendSurfaceChanged(format, width, height);
        } else {
            Log.e(TAG, "Ignoring surfaceChanged");
            return;
        }
    }

    @Override   // SurfaceHolder.Callback
    public void surfaceDestroyed(SurfaceHolder holder) {
        // In theory we should tell the RenderThread that the surface has been destroyed.
        if (mRenderThread != null) {
            RenderHandler rh = mRenderThread.getHandler();
            rh.sendSurfaceDestroyed();
        }

        sSurfaceHolder = null;
    }

    /*
    mario
    @Override   // SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mRenderThread == null) {
            // Could happen if we programmatically update the values after setting a listener
            // but before starting the thread.  Also, easy to cause this by scrubbing the seek
            // bar with one finger then tapping "recents" with another.
            Log.w(TAG, "Ignoring onProgressChanged received w/o RT running");
            return;
        }
        RenderHandler rh = mRenderThread.getHandler();

        // "progress" ranges from 0 to 100
        if (seekBar == mZoomBar) {
            //Log.v(TAG, "zoom: " + progress);
            rh.sendZoomValue(progress);
        } else if (seekBar == mSizeBar) {
            //Log.v(TAG, "size: " + progress);
            rh.sendSizeValue(progress);
        } else if (seekBar == mRotateBar) {
            //Log.v(TAG, "rotate: " + progress);
            rh.sendRotateValue(progress);
        } else {
            throw new RuntimeException("unknown seek bar");
        }

        // If we're getting preview frames quickly enough we don't really need this, but
        // we don't want to have chunky-looking resize movement if the camera is slow.
        // OTOH, if we get the updates too quickly (60fps camera?), this could jam us
        // up and cause us to run behind.  So use with caution.
        rh.sendRedraw();
    }

    @Override   // SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override   // SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {}
    */

    @Override
    /**
     * Handles any touch events that aren't grabbed by one of the controls.
     */
    public boolean onTouchEvent(MotionEvent e) {

        //jpctWorldManager.getCameraPosition();

        /*
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            //xpos = e.getX();
            //ypos = e.getY();
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_UP) {
            //xpos = -1;
            //ypos = -1;
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            return true;
        }
        */
        return super.onTouchEvent(e);
        /*
        mario
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
                //Log.v(TAG, "onTouchEvent act=" + e.getAction() + " x=" + x + " y=" + y);
                if (mRenderThread != null) {
                    RenderHandler rh = mRenderThread.getHandler();
                    rh.sendPosition((int) x, (int) y);

                    // Forcing a redraw can cause sluggish-looking behavior if the touch
                    // events arrive quickly.
                    //rh.sendRedraw();
                }
                break;
            default:
                break;
        }
        return true;
        */
    }


    /*
    called by SensorFusion to provide roll pitch and heading values to JPCTWorldManager
    */
    public void onNewOrientationAnglesComputed(float roll, float pitch, float head, final boolean facedown) {

        final float rollA = roll, pitchA = pitch, headA = head;

        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                jpctWorldManager.setRPHCam(rollA, pitchA, headA, facedown);
            }
        });

    }

    /*
    called by SensorFusion to provide orientationMatrix to JPCTWorldManager
    */
    public void onNewOrientationMatrixComputed(final float [] gyroMatrix) {


        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                jpctWorldManager.setRotationMatrix(gyroMatrix);
            }
        });

    }


    /*
        Tell the render thread to update the text view showing the values of Roll, Pitch and Heading
    */
    public void showOrientationFusedAngle(double f0, double f1, double f2) {

        this.mHandler.sendTempMessageParams(f0, f1, f2);

    }


    /**
     * Updates the current state of the controls.
     */

    //mario
    private void updateControls() {
        //String str = getString(R.string.tfcCameraParams, mCameraPreviewWidth,mCameraPreviewHeight, mCameraPreviewFps);

        //TextView tv = (TextView) findViewById(R.id.tfcCameraParams_text);
        //tv.setText(str);
        if (xG != null) {
            String coordinates = "H:" + xG.floatValue() + "\n P:" + yG.floatValue() + "\n R:" + zG.floatValue();
            //+ "VR:" + vCamRoll.floatValue() + " VP:" + vCamPitch.floatValue() + " VH:" + vCamHead.floatValue();
            tmptv.setText(coordinates);
        }
       /*
        str = getString(R.string.tfcRectSize, mRectWidth, mRectHeight);
        tv = (TextView) findViewById(R.id.tfcRectSize_text);
        tv.setText(str);

        str = getString(R.string.tfcZoomArea, mZoomWidth, mZoomHeight);
        tv = (TextView) findViewById(R.id.tfcZoomArea_text);
        tv.setText(str);
        */
    }


    /**
     * Function to show settings alert dialog.
     * On pressing the Settings button it will launch Settings Options.
     *
     * TO FIX : has to be called within the main thread context
     *
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing the Settings button.
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // On pressing the cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }




    /**
     * Custom message handler for main UI thread.
     * <p/>
     * Receives messages from the renderer thread with UI-related updates, like the camera
     * parameters (which we show in a text message on screen).
     */
    public static class MainHandler extends Handler {
        private static final int MSG_SEND_CAMERA_PARAMS0 = 0;
        private static final int MSG_SEND_CAMERA_PARAMS1 = 1;
        private static final int MSG_SEND_RECT_SIZE = 2;
        private static final int MSG_SEND_ZOOM_AREA = 3;
        private static final int MSG_SEND_ROTATE_DEG = 4;
        private static final int MSG_SET_TEMP_TV = 5;
        private static final int MSG_SET_LOCALIZATION = 6;
        private static final int MSG_SET_VIRTUALCOORS = 7;
        private static final int MSG_SHOW_SETTINGS_ALERT = 8;


        private WeakReference<TextureFromCameraActivity> mWeakActivity;

        public MainHandler(TextureFromCameraActivity activity) {
            mWeakActivity = new WeakReference<TextureFromCameraActivity>(activity);
        }

        public void sendTempMessageParams(double f0, double f1, double f2) {
            sendMessage(obtainMessage(MSG_SET_TEMP_TV, 0, 0, new Coordinates(f0, f1, f2)));
        }


        /**
         * Sends the updated camera parameters to the main thread.
         * <p/>
         * Call from render thread.
         */
        public void sendCameraParams(int width, int height, float fps) {
            // The right way to do this is to bundle them up into an object.  The lazy
            // way is to send two messages.
            sendMessage(obtainMessage(MSG_SEND_CAMERA_PARAMS0, width, height));
            sendMessage(obtainMessage(MSG_SEND_CAMERA_PARAMS1, (int) (fps * 1000), 0));
        }

        /**
         * Sends the updated rect size to the main thread.
         * <p/>
         * Call from render thread.
         */
        public void sendRectSize(int width, int height) {
            sendMessage(obtainMessage(MSG_SEND_RECT_SIZE, width, height));
        }

        /**
         * Sends the updated zoom area to the main thread.
         * <p/>
         * Call from render thread.
         */
        public void sendZoomArea(int width, int height) {
            sendMessage(obtainMessage(MSG_SEND_ZOOM_AREA, width, height));
        }

        /**
         * Sends the updated zoom area to the main thread.
         * <p/>
         * Call from render thread.
         */
        public void sendRotateDeg(int rot) {
            sendMessage(obtainMessage(MSG_SEND_ROTATE_DEG, rot, 0));
        }

        public void sendLocalization(String loc) {
            sendMessage(obtainMessage(MSG_SET_LOCALIZATION, loc));
        }


        public void sendVirtualCoordinates(String coors) {
            sendMessage(obtainMessage(MSG_SET_VIRTUALCOORS, coors));
        }


        public void sendSettingsAlertMessage() {
            sendMessage(obtainMessage(MSG_SHOW_SETTINGS_ALERT));
        }



        @Override
        public void handleMessage(Message msg) {
            TextureFromCameraActivity activity = mWeakActivity.get();
            if (activity == null) {
                return;
            }

            switch (msg.what) {
                case MSG_SEND_CAMERA_PARAMS0: {
                    activity.mCameraPreviewWidth = msg.arg1;
                    activity.mCameraPreviewHeight = msg.arg2;
                    break;
                }
                case MSG_SEND_CAMERA_PARAMS1: {
                    activity.mCameraPreviewFps = msg.arg1 / 1000.0f;
                    //mario
                    //activity.updateControls();
                    break;
                }
                case MSG_SEND_RECT_SIZE: {
                    activity.mRectWidth = msg.arg1;
                    activity.mRectHeight = msg.arg2;
                    //mario
                    //activity.updateControls();
                    break;
                }
                case MSG_SEND_ZOOM_AREA: {
                    activity.mZoomWidth = msg.arg1;
                    activity.mZoomHeight = msg.arg2;
                    //mario
                    //activity.updateControls();
                    break;
                }
                case MSG_SEND_ROTATE_DEG: {
                    activity.mRotateDeg = msg.arg1;
                    //mario
                    //activity.updateControls();
                    break;
                }
                case MSG_SET_TEMP_TV: {
                    Coordinates coors = (Coordinates) msg.obj;
                    activity.xG = coors.getX();
                    activity.yG = coors.getY();
                    activity.zG = coors.getZ();
                    activity.updateControls();
                    break;
                }
                case MSG_SET_LOCALIZATION: {
                    String loc = (String) msg.obj;
                    activity.LOCview.setText(loc);
                    break;
                }
                case MSG_SET_VIRTUALCOORS: {
                    String coors = (String) msg.obj;
                    activity.virtualCoorsView.setText(coors);
                    break;
                }
                case MSG_SHOW_SETTINGS_ALERT: {
                    activity.showSettingsAlert();
                    break;
                }
                default:
                    throw new RuntimeException("Unknown message " + msg.what);
            }
        }

    }


    /**
     * Thread that handles all rendering and camera operations.
     */
    private class RenderThread extends Thread implements
            SurfaceTexture.OnFrameAvailableListener {
        // Object must be created on render thread to get correct Looper, but is used from
        // UI thread, so we need to declare it volatile to ensure the UI thread sees a fully
        // constructed object.
        private volatile RenderHandler mHandler;

        // Used to wait for the thread to start.
        private Object mStartLock = new Object();
        private boolean mReady = false;

        private MainHandler mMainHandler;

        private Camera mCamera;
        private int mCameraPreviewWidth, mCameraPreviewHeight;

        private EglCore mEglCore;
        private WindowSurface mWindowSurface;
        private int mWindowSurfaceWidth;
        private int mWindowSurfaceHeight;

        // Receives the output from the camera preview.
        private SurfaceTexture mCameraTexture;

        // Orthographic projection matrix.
        private float[] mDisplayProjectionMatrix = new float[16];

        private Texture2dProgram mTexProgram;
        private final ScaledDrawable2d mRectDrawable =
                new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
        private final Sprite2d mRect = new Sprite2d(mRectDrawable);

        private int mZoomPercent = DEFAULT_ZOOM_PERCENT;
        private int mSizePercent = DEFAULT_SIZE_PERCENT;
        private int mRotatePercent = DEFAULT_ROTATE_PERCENT;
        private float mPosX, mPosY;


        /**
         * Constructor.  Pass in the MainHandler, which allows us to send stuff back to the
         * Activity.
         */
        public RenderThread(MainHandler handler) {
            mMainHandler = handler;
        }

        /**
         * Thread entry point.
         */
        @Override
        public void run() {
            Looper.prepare();

            // We need to create the Handler before reporting ready.
            mHandler = new RenderHandler(this);
            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();    // signal waitUntilReady()
            }

            // Prepare EGL and open the camera before we start handling messages.
            mEglCore = new EglCore(null, 0);

            openCamera(REQ_CAMERA_WIDTH, REQ_CAMERA_HEIGHT, REQ_CAMERA_FPS);

            //Log.e(TAG, "REQCAMw:"+REQ_CAMERA_WIDTH+"  REQCAMh:"+REQ_CAMERA_HEIGHT);


            Looper.loop();

            Log.e(TAG, "looper quit");
            releaseCamera();
            releaseGl();
            mEglCore.release();

            synchronized (mStartLock) {
                mReady = false;
            }
        }

        /**
         * Waits until the render thread is ready to receive messages.
         * <p/>
         * Call from the UI thread.
         */
        public void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        /**
         * Shuts everything down.
         */
        private void shutdown() {
            Looper.myLooper().quit();
        }

        /**
         * Returns the render thread's Handler.  This may be called from any thread.
         */
        public RenderHandler getHandler() {
            return mHandler;
        }

        /**
         * Handles the surface-created callback from SurfaceView.  Prepares GLES and the Surface.
         */
        private void surfaceAvailable(SurfaceHolder holder, boolean newSurface) {


            Surface surface = holder.getSurface();
            mWindowSurface = new WindowSurface(mEglCore, surface, false);
            mWindowSurface.makeCurrent();

            // Create and configure the SurfaceTexture, which will receive frames from the
            // camera.  We set the textured rect's program to render from it.
            mTexProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
            int textureId = mTexProgram.createTextureObject();
            mCameraTexture = new SurfaceTexture(textureId);
            mRect.setTexture(textureId);

            if (!newSurface) {
                // This Surface was established on a previous run, so no surfaceChanged()
                // message is forthcoming.  Finish the surface setup now.
                //
                // We could also just call this unconditionally, and perhaps do an unnecessary
                // bit of reallocating if a surface-changed message arrives.
                mWindowSurfaceWidth = mWindowSurface.getWidth();
                mWindowSurfaceHeight = mWindowSurface.getHeight();
                //Log.e(TAG, "camW:" + mWindowSurfaceWidth + " camH:" + mWindowSurfaceHeight);



                finishSurfaceSetup();
            }

            mCameraTexture.setOnFrameAvailableListener(this);

            //EYE
            //onCameraReady fragment
            mCamera.stopPreview();

            Log.e("PICTURE WH","w:"+PictureWidth+" H:"+PictureHeight);
            Camera.Size chosenSize = setupCamera(PictureWidth, PictureHeight, 4, 25.0, previewCb);
            Log.e(TAG, "camW:" + chosenSize.width + " camH:" + chosenSize.height);

            //nativeInitMediaEncoder(cameraView.getWidth(), cameraView.getHeight());

            nativeInitMediaEncoder(chosenSize.width, chosenSize.height);//M48 remember using nativeReleaseMediaEncoder

            /*
            List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
            for(Camera.Size size : sizes){
                Log.e(TAG,"Sizes: w:"+size.width+" h:"+size.height);
            }
            */

            //Log.e(TAG,"starting Preview");
            mCamera.startPreview();

            //Log.e(TAG,"started Preview");

        }

        //EYE
        public Camera.Size setupCamera(int wid, int hei, int bufNumber, double fps, PreviewCallback cb) {

            double diff = Math.abs(supportedSizes.get(0).width * supportedSizes.get(0).height - wid * hei);
            int targetIndex = 0;
            for (int i = 1; i < supportedSizes.size(); i++) {
                double newDiff = Math.abs(supportedSizes.get(i).width * supportedSizes.get(i).height - wid * hei);
                if (newDiff < diff) {
                    diff = newDiff;
                    targetIndex = i;
                }
            }
            procSize_.width = supportedSizes.get(targetIndex).width;
            procSize_.height = supportedSizes.get(targetIndex).height;


            diff = Math.abs(supportedFrameRate.get(0)[0] * supportedFrameRate.get(0)[1] - fps * fps * 1000 * 1000);
            targetIndex = 0;
            for (int i = 1; i < supportedFrameRate.size(); i++) {
                double newDiff = Math.abs(supportedFrameRate.get(i)[0] * supportedFrameRate.get(i)[1] - fps * fps * 1000 * 1000);
                if (newDiff < diff) {
                    diff = newDiff;
                    targetIndex = i;
                }
            }
            int targetMaxFrameRate = supportedFrameRate.get(targetIndex)[0];
            int targetMinFrameRate = supportedFrameRate.get(targetIndex)[1];

            Camera.Parameters p = mCamera.getParameters();
            p.setPreviewSize(procSize_.width, procSize_.height);
            Log.e("Preview Size set to:", "w:" + procSize_.width + " h:" + procSize_.height);
            p.setPreviewFormat(ImageFormat.NV21);
            //p.setPreviewFormat(ImageFormat.YV12);//YUV - N21

            p.setPreviewFpsRange(targetMaxFrameRate, targetMinFrameRate);
            mCamera.setParameters(p);

            //
            int previewFormat=p.getPreviewFormat();
            int bitsperpixel=ImageFormat.getBitsPerPixel(previewFormat);
            Log.e(TAG, "bits per pixel = "+bitsperpixel);
            double byteperpixel=(double) bitsperpixel/8;
            Log.e(TAG, "bytes per pixel = "+byteperpixel);
            Camera.Size camerasize=p.getPreviewSize();
            int bufSize= (int) Math.ceil(((camerasize.width*camerasize.height)*byteperpixel));

            //
            PixelFormat pixelFormat = new PixelFormat();
            //PixelFormat.getPixelFormatInfo(ImageFormat.NV21, pixelFormat);
            //PixelFormat.getPixelFormatInfo(ImageFormat.YV12, pixelFormat); //YUV - N21

            /*
            Log.println(Log.ASSERT,"BITSPERPIXEL=", ""+pixelFormat.bitsPerPixel);
            Log.e("BITSPERPIXEL=", ""+pixelFormat.bitsPerPixel);
            Log.d("BITSPERPIXEL=", ""+pixelFormat.bitsPerPixel);
            Log.i("BITSPERPIXEL=", ""+pixelFormat.bitsPerPixel);
            Log.v("BITSPERPIXEL=", ""+pixelFormat.bitsPerPixel);
            Log.w("BITSPERPIXEL=", ""+pixelFormat.bitsPerPixel);
            Log.wtf("BITSPERPIXEL=", ""+pixelFormat.bitsPerPixel);
            */

            //calculate bufsize for YV12 ImageFormat


            /*
            int yStride   = (int) Math.ceil(procSize_.width / 16.0) * 16;
            int uvStride  = (int) Math.ceil( (yStride / 2) / 16.0) * 16;
            int ySize     = yStride * procSize_.height;
            int uvSize    = uvStride * procSize_.height / 2;
            //int yRowIndex = yStride * y;
            //int uRowIndex = ySize + uvSize + uvStride * c;
            //int vRowIndex = ySize + uvStride * c;
            //int bufSize      = ySize + uvSize * 2;
            */
            //int bufSize = 1382400;

            //int bufSize = procSize_.width * procSize_.height * pixelFormat.bitsPerPixel / 8;
            //int bufSize = procSize_.width * procSize_.height * (3/2);

            byte[] buffer = null;
            for (int i = 0; i < bufNumber; i++) {
                buffer = new byte[bufSize];
                mCamera.addCallbackBuffer(buffer);
            }
            mCamera.setPreviewCallbackWithBuffer(cb);

            return camerasize;//supportedSizes.get(targetIndex);
        }

        /**
         * Releases most of the GL resources we currently hold (anything allocated by
         * surfaceAvailable()).
         * <p/>
         * Does not release EglCore.
         */
        private void releaseGl() {
            GlUtil.checkGlError("releaseGl start");

            if (mWindowSurface != null) {
                mWindowSurface.release();
                mWindowSurface = null;
            }
            if (mTexProgram != null) {
                mTexProgram.release();
                mTexProgram = null;
            }
            GlUtil.checkGlError("releaseGl done");

            mEglCore.makeNothingCurrent();
        }

        /**
         * Handles the surfaceChanged message.
         * <p/>
         * We always receive surfaceChanged() after surfaceCreated(), but surfaceAvailable()
         * could also be called with a Surface created on a previous run.  So this may not
         * be called.
         */
        private void surfaceChanged(int width, int height) {
            //Log.e(TAG, "RenderThread surfaceChanged w:" + width + " h:" + height);


            mWindowSurfaceWidth = width;
            mWindowSurfaceHeight = height;

            finishSurfaceSetup();
        }

        /**
         * Handles the surfaceDestroyed message.
         */
        private void surfaceDestroyed() {
            // In practice this never appears to be called -- the activity is always paused
            // before the surface is destroyed.  In theory it could be called though.
            //Log.d(TAG, "RenderThread surfaceDestroyed");
            releaseGl();
        }

        /**
         * Sets up anything that depends on the window size.
         * <p/>
         * Open the camera (to set mCameraAspectRatio) before calling here.
         */
        private void finishSurfaceSetup() {

            //Log.e(TAG, "finishSurfaceSetup start");

            int width = mWindowSurfaceWidth;
            int height = mWindowSurfaceHeight;
            //Log.e(TAG, "finishSurfaceSetup size=" + width + "x" + height +" camera=" + mCameraPreviewWidth + "x" + mCameraPreviewHeight);

            // Use full window.
            GLES20.glViewport(0, 0, width, height);

            // Simple orthographic projection, with (0,0) in lower-left corner.
            Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, width, 0, height, -1, 1);

            // Default position is center of screen.
            mPosX = width / 2.0f;
            mPosY = height / 2.0f;

            updateGeometry();

            // Ready to go, start the camera.
            try {
                mCamera.setPreviewTexture(mCameraTexture);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }


            //Log.e(TAG,"starting Preview");
            mCamera.startPreview();
            //Log.e(TAG,"started Preview");

            //Log.e(TAG, "finishSurfaceSetup end");
        }

        /**
         * Updates the geometry of mRect, based on the size of the window and the current
         * values set by the UI.
         */
        private void updateGeometry() {
            int width = mWindowSurfaceWidth;
            int height = mWindowSurfaceHeight;

            int smallDim = Math.min(width, height);
            // Max scale is a bit larger than the screen, so we can show over-size.
            float scaled = smallDim * (mSizePercent / 100.0f) * 1.25f;
            float cameraAspect = (float) mCameraPreviewWidth / mCameraPreviewHeight;
            int newWidth = Math.round(scaled * cameraAspect);
            int newHeight = Math.round(scaled);

            float zoomFactor = 1.0f - (mZoomPercent / 100.0f);
            //mario rotation changed
            int rotAngle = 0;//Math.round(360 * (mRotatePercent / 100.0f));

            mRect.setScale(newWidth, newHeight);
            mRect.setPosition(mPosX, mPosY);
            mRect.setRotation(rotAngle);
            mRectDrawable.setScale(zoomFactor);

            mMainHandler.sendRectSize(newWidth, newHeight);
            mMainHandler.sendZoomArea(Math.round(mCameraPreviewWidth * zoomFactor),
                    Math.round(mCameraPreviewHeight * zoomFactor));
            mMainHandler.sendRotateDeg(rotAngle);
        }

        @Override   // SurfaceTexture.OnFrameAvailableListener; runs on arbitrary thread
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mHandler.sendFrameAvailable();
        }

        /**
         * Handles incoming frame of data from the camera.
         */
        private void frameAvailable() {
            mCameraTexture.updateTexImage();
            draw();
        }

        /**
         * Draws the scene and submits the buffer.
         */
        private void draw() {
            GlUtil.checkGlError("draw start");

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            mRect.draw(mTexProgram, mDisplayProjectionMatrix);
            mWindowSurface.swapBuffers();

            GlUtil.checkGlError("draw done");
        }

        private void setZoom(int percent) {
            mZoomPercent = percent;
            updateGeometry();
        }

        private void setSize(int percent) {
            mSizePercent = percent;
            updateGeometry();
        }

        private void setRotate(int percent) {
            mRotatePercent = percent;
            updateGeometry();
        }

        private void setPosition(int x, int y) {
            mPosX = x;
            mPosY = mWindowSurfaceHeight - y;   // GLES is upside-down
            updateGeometry();
        }


        /**
         * Opens a camera, and attempts to establish preview mode at the specified width
         * and height with a fixed frame rate.
         * <p/>
         * Sets mCameraPreviewWidth / mCameraPreviewHeight.
         */
        private void openCamera(int desiredWidth, int desiredHeight, int desiredFps) {

            //Log.e(TAG, "openCamera start");


            if (mCamera != null) {
                throw new RuntimeException("camera already initialized");
            }

            //EYE
            // init audio and camera
            for (int i = 0; i < MediaBlockNumber; i++) {
                mediaBlocks[i] = new MediaBlock(MediaBlockSize);
            }
            resetMediaBuffer();


            Camera.CameraInfo info = new Camera.CameraInfo();

            // Try to find a front-facing camera (e.g. for videoconferencing).
            int numCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCamera = Camera.open(i);
                    break;
                }
            }
            if (mCamera == null) {
                Log.d(TAG, "No front-facing camera found; opening default");
                mCamera = Camera.open();    // opens first back-facing camera
            }
            if (mCamera == null) {
                throw new RuntimeException("Unable to open camera");
            }

            Camera.Parameters parms = mCamera.getParameters();

            //EYE
            supportedFrameRate = parms.getSupportedPreviewFpsRange();
            supportedSizes = parms.getSupportedPreviewSizes();
            procSize_ = supportedSizes.get(supportedSizes.size() / 2);
            //SUPPORTED SIZES!
            parms.setPreviewSize(procSize_.width, procSize_.height);
            mCamera.setPreviewCallbackWithBuffer(null);


            CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);

            // Try to set the frame rate to a constant value.
            int thousandFps = CameraUtils.chooseFixedPreviewFps(parms, desiredFps * 1000);

            // Give the camera a hint that we're recording video.  This can have a big
            // impact on frame rate.
            parms.setRecordingHint(true);

            mCamera.setParameters(parms);

            int[] fpsRange = new int[2];
            Camera.Size mCameraPreviewSize = parms.getPreviewSize();
            parms.getPreviewFpsRange(fpsRange);
            String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
            if (fpsRange[0] == fpsRange[1]) {
                previewFacts += " @" + (fpsRange[0] / 1000.0) + "fps";
            } else {
                previewFacts += " @[" + (fpsRange[0] / 1000.0) +
                        " - " + (fpsRange[1] / 1000.0) + "] fps";
            }
            Log.i(TAG, "Camera config: " + previewFacts);

            mCameraPreviewWidth = mCameraPreviewSize.width;
            mCameraPreviewHeight = mCameraPreviewSize.height;
            mMainHandler.sendCameraParams(mCameraPreviewWidth, mCameraPreviewHeight,
                    thousandFps / 1000.0f);



            //Log.e(TAG, "openCamera end");
        }

        /**
         * Stops camera preview, and releases the camera to the system.
         */
        private void releaseCamera() {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                Log.d(TAG, "releaseCamera -- done");
            }
        }
    }


    /**
     * Handler for RenderThread.  Used for messages sent from the UI thread to the render thread.
     * <p/>
     * The object is created on the render thread, and the various "send" methods are called
     * from the UI thread.
     */
    private static class RenderHandler extends Handler {
        private static final int MSG_SURFACE_AVAILABLE = 0;
        private static final int MSG_SURFACE_CHANGED = 1;
        private static final int MSG_SURFACE_DESTROYED = 2;
        private static final int MSG_SHUTDOWN = 3;
        private static final int MSG_FRAME_AVAILABLE = 4;
        private static final int MSG_ZOOM_VALUE = 5;
        private static final int MSG_SIZE_VALUE = 6;
        private static final int MSG_ROTATE_VALUE = 7;
        private static final int MSG_POSITION = 8;
        private static final int MSG_REDRAW = 9;

        // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
        // but no real harm in it.
        private WeakReference<RenderThread> mWeakRenderThread;

        /**
         * Call from render thread.
         */
        public RenderHandler(RenderThread rt) {
            mWeakRenderThread = new WeakReference<RenderThread>(rt);
        }

        /**
         * Sends the "surface available" message.  If the surface was newly created (i.e.
         * this is called from surfaceCreated()), set newSurface to true.  If this is
         * being called during Activity startup for a previously-existing surface, set
         * newSurface to false.
         * <p/>
         * The flag tells the caller whether or not it can expect a surfaceChanged() to
         * arrive very soon.
         * <p/>
         * Call from UI thread.
         */
        public void sendSurfaceAvailable(SurfaceHolder holder, boolean newSurface) {
            sendMessage(obtainMessage(MSG_SURFACE_AVAILABLE,
                    newSurface ? 1 : 0, 0, holder));
        }

        /**
         * Sends the "surface changed" message, forwarding what we got from the SurfaceHolder.
         * <p/>
         * Call from UI thread.
         */
        public void sendSurfaceChanged(@SuppressWarnings("unused") int format, int width,
                                       int height) {
            // ignore format
            sendMessage(obtainMessage(MSG_SURFACE_CHANGED, width, height));
        }

        /**
         * Sends the "shutdown" message, which tells the render thread to halt.
         * <p/>
         * Call from UI thread.
         */
        public void sendSurfaceDestroyed() {
            sendMessage(obtainMessage(MSG_SURFACE_DESTROYED));
        }

        /**
         * Sends the "shutdown" message, which tells the render thread to halt.
         * <p/>
         * Call from UI thread.
         */
        public void sendShutdown() {
            sendMessage(obtainMessage(MSG_SHUTDOWN));
        }

        /**
         * Sends the "frame available" message.
         * <p/>
         * Call from UI thread.
         */
        public void sendFrameAvailable() {
            sendMessage(obtainMessage(MSG_FRAME_AVAILABLE));
        }

        /**
         * Sends the "zoom value" message.  "progress" should be 0-100.
         * <p/>
         * Call from UI thread.
         */
        public void sendZoomValue(int progress) {
            sendMessage(obtainMessage(MSG_ZOOM_VALUE, progress, 0));
        }

        /**
         * Sends the "size value" message.  "progress" should be 0-100.
         * <p/>
         * Call from UI thread.
         */
        public void sendSizeValue(int progress) {
            sendMessage(obtainMessage(MSG_SIZE_VALUE, progress, 0));
        }

        /**
         * Sends the "rotate value" message.  "progress" should be 0-100.
         * <p/>
         * Call from UI thread.
         */
        public void sendRotateValue(int progress) {
            sendMessage(obtainMessage(MSG_ROTATE_VALUE, progress, 0));
        }

        /**
         * Sends the "position" message.  Sets the position of the rect.
         * <p/>
         * Call from UI thread.
         */
        public void sendPosition(int x, int y) {
            sendMessage(obtainMessage(MSG_POSITION, x, y));
        }


        /**
         * Sends the "redraw" message.  Forces an immediate redraw.
         * <p/>
         * Call from UI thread.
         */
        public void sendRedraw() {
            sendMessage(obtainMessage(MSG_REDRAW));
        }

        @Override  // runs on RenderThread
        public void handleMessage(Message msg) {
            int what = msg.what;
            //Log.d(TAG, "RenderHandler [" + this + "]: what=" + what);

            RenderThread renderThread = mWeakRenderThread.get();
            if (renderThread == null) {
                Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
                return;
            }

            switch (what) {
                case MSG_SURFACE_AVAILABLE:
                    renderThread.surfaceAvailable((SurfaceHolder) msg.obj, msg.arg1 != 0);
                    break;
                case MSG_SURFACE_CHANGED:
                    renderThread.surfaceChanged(msg.arg1, msg.arg2);
                    break;
                case MSG_SURFACE_DESTROYED:
                    renderThread.surfaceDestroyed();
                    break;
                case MSG_SHUTDOWN:
                    renderThread.shutdown();
                    break;
                case MSG_FRAME_AVAILABLE:
                    renderThread.frameAvailable();
                    break;
                case MSG_ZOOM_VALUE:
                    renderThread.setZoom(msg.arg1);
                    break;
                case MSG_SIZE_VALUE:
                    renderThread.setSize(msg.arg1);
                    break;
                case MSG_ROTATE_VALUE:
                    renderThread.setRotate(msg.arg1);
                    break;
                case MSG_POSITION:
                    renderThread.setPosition(msg.arg1, msg.arg2);
                    break;
                case MSG_REDRAW:
                    renderThread.draw();
                    break;
                default:
                    throw new RuntimeException("unknown message " + what);
            }
        }
    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @SuppressWarnings("JniMissingFunction")
    private native void nativeInitMediaEncoder(int width, int height);

    @SuppressWarnings("JniMissingFunction")
    private native void nativeReleaseMediaEncoder(int width, int height);

    @SuppressWarnings("JniMissingFunction")
    private native int nativeDoVideoEncode(byte[] in, byte[] out, int flag);

    @SuppressWarnings("JniMissingFunction")
    private native int nativeDoAudioEncode(byte[] in, int length, byte[] out);

    static {
        System.loadLibrary("MediaEncoder");
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    //
    //  Internal help functions
    //
    private boolean showIpAddress() {

        String ipAddr = wifiIpAddress(this);


        //TextView tv = (TextView) findViewById(R.id.tv_message);



        if (ipAddr == null) {
            IPview.setText(getString(R.string.msg_wifi_error));
        } else {
            Log.e("IPADDRESS:", getString(R.string.msg_access_local) + " http://" + ipAddr + ":"+StreamingPort);
            IPview.setText(getString(R.string.msg_access_local) + " http://" + ipAddr + ":"+StreamingPort);
            return true;
            //tv.setText(getString(R.string.msg_port_error));
        }
        return false;

    }



    private class StreamingThread extends Thread{


        private Handler streamerHandler;

        public StreamingThread(){


        }

        @Override
        public void run() {

            Looper.prepare();

            // We need to create the Handler before reporting ready.
            streamingHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {




                    if(msg.what==NEW_ENCODED_FRAME_AVAILABLE){
                        doStreaming3();
                    }
                    else {
                        final int readIndex = msg.arg1;
                        doStreaming2(readIndex);
                    }
                    return true;
                }
            });


            Looper.loop();

        }

    }


    private int lastBlockSentTimeInMillis = (int) (System.currentTimeMillis() % 65535);

    private void doStreaming3() {
        //synchronized (TextureFromCameraActivity.this) {
        long newtmill = System.currentTimeMillis();
        long mill = newtmill - tmill;
        tmill = newtmill;
        Log.e("Thread run interval:", ""+mill+" queue size:"+frameToBeEncodedQueue.size());
        //}

        //Log.e(TAG,"doStreaming");
            /*
            long newtmill= System.currentTimeMillis();
            long mill = newtmill - tmill;
            tmill=newtmill;
            Log.e("Thread run interval:", ""+mill);
            */

        MediaBlock targetBlock = frameToBeSent.poll();
        if (targetBlock == null) {
            //Log.e(TAG, "M48, null mediablock, thread yield");
            return;
        } else if (targetBlock != null)
            //if(targetBlock.millis<lastBlockSentTimeInMillis)return;

            if (targetBlock.flag == 1) {
                //Log.e(TAG, "doStreaming: flag=1");

                // HERE IS THE PROBLEM
                    /*
                    long newtmill= System.currentTimeMillis();
                    long mill = newtmill - tmill;
                    tmill=newtmill;
                    Log.e("Thread run interval:", ""+mill);
                    */



                streamingServer.sendMedia(targetBlock.data(), targetBlock.length());
                /*
                    long newtmill= System.currentTimeMillis();
                    long mill = newtmill - tmill;
                    tmill=newtmill;
                    Log.e("Thread run interval:", ""+mill);
                    */
                //targetBlock.reset();
                lastBlockSentTimeInMillis=targetBlock.millis;

            }



    }

    private void doStreaming2(int readIndex) {


            //Log.e(TAG,"doStreaming");
            /*
            long newtmill= System.currentTimeMillis();
            long mill = newtmill - tmill;
            tmill=newtmill;
            Log.e("Thread run interval:", ""+mill);
            */

            MediaBlock targetBlock = mediaBlocks[readIndex];
            if (targetBlock == null) {
                //Log.e(TAG, "M48, null mediablock, thread yield");
                Thread.yield();
            } else if (targetBlock != null)
                if (targetBlock.flag == 1) {

                    //Log.e(TAG, "doStreaming: flag=1");

                    // HERE IS THE PROBLEM
                    /*
                    long newtmill= System.currentTimeMillis();
                    long mill = newtmill - tmill;
                    tmill=newtmill;
                    Log.e("Thread run interval:", ""+mill);
                    */

                    streamingServer.sendMedia(targetBlock.data(), targetBlock.length());
                    targetBlock.reset();

                }



    }


    private void doStreaming() {

        synchronized (TextureFromCameraActivity.this) {
            //Log.e(TAG,"doStreaming");


            MediaBlock targetBlock = mediaBlocks[mediaReadIndex];
            if (targetBlock == null) {
                //Log.e(TAG, "M48, null mediablock, thread yield");
                Thread.yield();
            } else if (targetBlock != null)
                if (targetBlock.flag == 1) {

                    //Log.e(TAG, "doStreaming: flag=1");

                    // HERE IS THE PROBLEM
                    /*
                    long newtmill= System.currentTimeMillis();
                    long mill = newtmill - tmill;
                    tmill=newtmill;
                    Log.e("Thread run interval:", ""+mill);
                    */

                    streamingServer.sendMedia(targetBlock.data(), targetBlock.length());
                    targetBlock.reset();

                    mediaReadIndex++;
                    if (mediaReadIndex >= MediaBlockNumber) {
                        mediaReadIndex = 0;
                    }
                }

        }

        streamingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStreaming();
            }
        }, StreamingInterval);

    }

    protected String wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }


    private long tmill = System.currentTimeMillis();

    //
    //  Internal help class and object definment
    //
    private PreviewCallback previewCb = new PreviewCallback() {


        public void onPreviewFrame(byte[] frame, Camera c) {

            previewLock.lock();
            /*
            long newtmill= System.currentTimeMillis();
            long mill = newtmill - tmill;
            tmill=newtmill;
            Log.e("prevCall interval:", ""+mill);
            */
            if (streamingServer != null)
                if (streamingServer.inStreaming == true)
                    doVideoEncode(frame);



            c.addCallbackBuffer(frame);

            previewLock.unlock();
        }
    };



    private int filter = 0;

    private void doVideoEncode(byte[] frame) {
        //if(frameToBeEncodedQueue.size()>QUEUE_SIZE)return;


        /*
        if (inProcessing == true) {
            return;
        }
        inProcessing = true;
        */

        //int picWidth = mCameraPreviewWidth;//cameraView.Width();
        //int picHeight = mCameraPreviewHeight;//cameraView.Height();
        //Log.e("doVideoEncode","picWidth:"+picWidth+" picHeight:"+picHeight);

        //int size = /*frame.length;*/picWidth*picHeight + picWidth*picHeight/2;

        //Log.e(TAG, "fr.len="+frame.length + "  !=   size="+size);

        if(/*videoEncoderHandler!=null&&*/streamingServer!=null && streamingServer.inStreaming==true) {
            //filter++;
            //if(filter<3)return;

            //filter = 0;


            //byte[] yvFrame = new byte[1920 * 1280 * 2];
            //System.arraycopy(frame, 0, yvFrame, 0, size);
                //Message m = new Message();
                //frameToBeEncodedQueue.add(yvFrame);
                //videoEncoderHandler.sendMessage(m);
                //executor.execute(videoTask);

                if(frameToBeEncodedQueue.size()>10){
                    frameToBeEncodedQueue.poll();
                }
                frameToBeEncodedQueue.add(frame);
                executor.execute(videoEncoderThread);

        }
    }


    ExecutorService executor = Executors.newFixedThreadPool(1);
    ConcurrentLinkedQueue<byte[]> frameToBeEncodedQueue = new ConcurrentLinkedQueue();
    ConcurrentLinkedQueue<MediaBlock> frameToBeSent = new ConcurrentLinkedQueue();
    private VideoEncoderThread videoEncoderThread = new VideoEncoderThread();
    private Handler videoEncoderHandler=null;

    private int NEW_ENCODED_FRAME_AVAILABLE = 2;
    private int QUEUE_SIZE = 10;


    private class VideoEncoderThread implements Runnable {

        private byte[] resultNal = new byte[2*1024 * 1024];
        private byte[] videoHeader = new byte[8];


        public VideoEncoderThread() {
            videoHeader[0] = (byte) 0x19;
            videoHeader[1] = (byte) 0x79;
        }

        /*
        public void run(){


            // We need to create the Handler before reporting ready.
            Looper.prepare();

            // We need to create the Handler before reporting ready.
            videoEncoderHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {


                    encode();

                    return true;
                }
            });


            Looper.loop();

        }
        */

        public void run() {
            //Log.e(TAG,"VIDEOENCODINGTASK");

            Log.e("VIDEOENCODINGTASK", "queue size:"+frameToBeEncodedQueue.size());

            byte [] toBeEncoded = frameToBeEncodedQueue.poll();

            if(toBeEncoded==null)return;

            //MediaBlock currentBlock = mediaBlocks[mediaWriteIndex];
            MediaBlock currentBlock = new MediaBlock(MediaBlockSize);


            int intraFlag = 0;
            if (currentBlock.videoCount == 0) {
                intraFlag = 1;
            }
            int millis = (int) (System.currentTimeMillis() % 65535);

            int ret = -1;

            ret = nativeDoVideoEncode(toBeEncoded, resultNal, intraFlag);

            currentBlock.millis=millis;


                if (ret <= 0) {
                    return;
                }



            // timestamp
            videoHeader[2] = (byte) (millis & 0xFF);
            videoHeader[3] = (byte) ((millis >> 8) & 0xFF);
            // length
            videoHeader[4] = (byte) (ret & 0xFF);
            videoHeader[5] = (byte) ((ret >> 8) & 0xFF);
            videoHeader[6] = (byte) ((ret >> 16) & 0xFF);
            videoHeader[7] = (byte) ((ret >> 24) & 0xFF);


            //veloce

            if (currentBlock.length() + ret + 8 <= MediaBlockSize) {
                currentBlock.write(videoHeader, 8);
                currentBlock.writeVideo(resultNal, ret);

                currentBlock.flag = 1;
                //Log.e(TAG,"NEW BLOCK ENCODED");


                //lento


                if(streamingServer!=null && streamingServer.inStreaming==true && streamingHandler!=null) {

                    //streamingServer.sendMedia(currentBlock.data(),currentBlock.length());
                    frameToBeSent.offer(currentBlock);

                    //Log.e("Thread run", "frame TO BE SENT:"+frameToBeSent.size()+ " current:"+currentBlock.length());



                    Message streamMex = new Message();

                    streamMex.what=NEW_ENCODED_FRAME_AVAILABLE;
                            //Log.e(TAG, "encoded ready to stream block:"+ mediaWriteIndex);

                            //LENTO

                    streamingHandler.sendMessage(streamMex);
                    Log.e("VIDEOENCODER","end");

                }
            }

            //inProcessing = false;
        }
    }




    //EYE
    private class VideoEncodingTask implements Runnable {

        private byte[] resultNal = new byte[1024 * 1024];
        private byte[] videoHeader = new byte[8];


        public VideoEncodingTask() {
            videoHeader[0] = (byte) 0x19;
            videoHeader[1] = (byte) 0x79;
        }

        public void run() {
            //Log.e(TAG,"VIDEOENCODINGTASK");

            MediaBlock currentBlock = mediaBlocks[mediaWriteIndex];
            if (currentBlock.flag == 1) {
                inProcessing = false;
                return;
            }

            int intraFlag = 0;
            if (currentBlock.videoCount == 0) {
                intraFlag = 1;
            }
            int millis = (int) (System.currentTimeMillis() % 65535);
            int ret = nativeDoVideoEncode(yuvFrame, resultNal, intraFlag);


            Log.e(TAG,"Encoding ret:"+ret);

            if (ret <= 0) {
                return;
            }

            // timestamp
            videoHeader[2] = (byte) (millis & 0xFF);
            videoHeader[3] = (byte) ((millis >> 8) & 0xFF);
            // length
            videoHeader[4] = (byte) (ret & 0xFF);
            videoHeader[5] = (byte) ((ret >> 8) & 0xFF);
            videoHeader[6] = (byte) ((ret >> 16) & 0xFF);
            videoHeader[7] = (byte) ((ret >> 24) & 0xFF);


            synchronized (TextureFromCameraActivity.this) {

                //veloce

                if (currentBlock.flag == 0) {
                    boolean changeBlock = false;

                    if (currentBlock.length() + ret + 8 <= MediaBlockSize) {
                        currentBlock.write(videoHeader, 8);
                        currentBlock.writeVideo(resultNal, ret);
                    } else {
                        changeBlock = true;

                        //lento
                    }

                    if (changeBlock == false) {
                        if (currentBlock.videoCount >= EstimatedFrameNumber) {
                            changeBlock = true;



                        }
                    }

                    if (changeBlock == true) {
                        currentBlock.flag = 1;
                        //Log.e(TAG,"NEW BLOCK ENCODED");

                        /*
                        long newtmill= System.currentTimeMillis();
                        long mill = newtmill - tmill;
                        tmill=newtmill;
                        Log.e("Thread run interval:", ""+mill);
                        */

                        //lento


                        if(streamingServer!=null && streamingServer.inStreaming==true && streamingHandler!=null) {

                            Message streamMex = new Message();
                            streamMex.arg1=mediaWriteIndex;
                            //Log.e(TAG, "encoded ready to stream block:"+ mediaWriteIndex);

                            //LENTO

                            streamingHandler.sendMessage(streamMex);

                        }

                        mediaWriteIndex++;
                        if (mediaWriteIndex >= MediaBlockNumber) {
                            mediaWriteIndex = 0;
                        }

                    }
                }

            }

            inProcessing = false;
        }
    }




    //EYE
    private void resetMediaBuffer() {
        synchronized (TextureFromCameraActivity.this) {
            for (int i = 1; i < MediaBlockNumber; i++) {
                mediaBlocks[i].reset();
            }
            mediaWriteIndex = 0;
            mediaReadIndex = 0;
        }
    }

    //EYE
    private class StreamingServer extends WebSocketServer {

        private WebSocket mediaSocket = null;
        public boolean inStreaming = false;
        //private final int MediaBlockSize = 1024 * 512;
        ByteBuffer buf = ByteBuffer.allocate(MediaBlockSize);

        public StreamingServer(int port) throws UnknownHostException {
            super(new InetSocketAddress(port));

        }


        private long INTERVAL = System.currentTimeMillis();

        public boolean sendMedia(byte[] data, int length) {
            //Log.e(TAG, "sendMedia() ");
            boolean ret = false;

            if (inStreaming == true) {
                buf.clear();
                buf.put(data, 0, length);
                buf.flip();
            }

            if (inStreaming == true) {

                mediaSocket.send(buf);
                ret = true;

                /*
                long newTime = System.currentTimeMillis();
                long intv = newTime - INTERVAL;
                INTERVAL = newTime;

                Log.e(TAG, "sending:" + length + " byte. TIMEINMILLIS:" + intv);//mediaSocket.send("camW:"+mCameraPreviewWidth+" camH:"+mCameraPreviewHeight);
                */
            }

            return ret;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {

            Log.e("onOpen:", " instreaming=" + inStreaming);


            if (inStreaming == true) {
                /*
                inStreaming = false;//mario
                mediaSocket.close();//mario
                mediaSocket = conn;//mario
                inStreaming = true;
                */
                conn.close();
            } else {
                Log.e("onOpen:", conn.getRemoteSocketAddress().toString());
                resetMediaBuffer();
                mediaSocket = conn;
                inStreaming = true;
            }
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            Log.e("onClose:", conn.getRemoteSocketAddress().toString());
            if (conn == mediaSocket) {
                inStreaming = false;
                mediaSocket = null;
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            if (conn == mediaSocket) {
                inStreaming = false;
                mediaSocket = null;
            }
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer blob) {
            Log.e("onMessage:", "bytebuffer");
        }

        @Override
        public void onMessage(WebSocket conn, String message) {

            Log.e("received", message);
            conn.send("I can hear you.");

        }

    }




}
