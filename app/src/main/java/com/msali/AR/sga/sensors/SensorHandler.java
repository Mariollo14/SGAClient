package com.msali.AR.sga.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import com.android.libraries.TextureFromCameraActivity;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Mario Salierno on 12/07/2016.
 */
public class SensorHandler implements SensorEventListener {

    //activity context
    TextureFromCameraActivity activity;

    private SensorManager mSensorManager;
    private Sensor rotationSensor;
    private Sensor lightSensor;
    // accelerometer and magnetometer based rotation matrix
    //private float[] rotation = new float[9];
    private float[] quaternion = new float[4];
    private float[] lightValues=null;
    //private float[] gyroMatrix = new float[9];



    /*
    To have more control over its output, we execute the filtering in a separate timed thread.
    The quality of the sensor signal strongly depends on the sampling frequency, that is, how often the filter method is called per second.
    That’s why we put all the calculations in a TimerTask and define later the time interval between each call.
    */
    /*
    FILTER_COEFFICIENT: value determined heuristically.
    A value of 0.98 with a sampling rate of 33Hz (this yields a time period of 30ms) worked quite well.
    You can increase the sampling rate to get a better time resolution, but then you have to adjust the FILTER_COEFFICIENT
    to improve the signal quality.
     */
    //public static final float FILTER_COEFFICIENT = 0.98f;
    public static final int TIME_CONSTANT = 50;
    private Timer sampleTimer = new Timer();


    public SensorHandler(TextureFromCameraActivity act){
        this.activity=act;

        // initialise gyroMatrix with identity matrix
        //rotation[0] = 1.0f; rotation[1] = 0.0f; rotation[2] = 0.0f;
        //rotation[3] = 0.0f; rotation[4] = 1.0f; rotation[5] = 0.0f;
        //rotation[6] = 0.0f; rotation[7] = 0.0f; rotation[8] = 1.0f;

        // initialize the rotation matrix to identity
        //rotation[ 0] = 1;
        //rotation[ 4] = 1;
        //rotation[ 8] = 1;
        //rotation[12] = 1;

        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) activity.getSystemService(activity.SENSOR_SERVICE);
        initListeners();


        //timer for the filter task
        //see class calculateFusedOrientationTask
        // wait for one second until gyroscope and magnetometer/accelerometer
        // data is initialised then scedule the complementary filter task
        sampleTimer.scheduleAtFixedRate(new SampleOrientationTask(),
                1000, TIME_CONSTANT);
    }

    /*
    https://developer.android.com/guide/topics/sensors/sensors_motion.html
    The rotation vector sensor and the gravity sensor are the most frequently used sensors for motion detection and monitoring.
    The rotational vector sensor is particularly versatile and can be used for a wide range of motion-related tasks, such as detecting gestures,
    monitoring angular change, and monitoring relative orientation changes. For example, the rotational vector sensor is ideal if you are developing a game,
    an augmented reality application, a 2-dimensional or 3-dimensional compass, or a camera stabilization app.
    In most cases, using these sensors is a better choice than using the accelerometer and geomagnetic field sensor or the orientation sensor.

    https://source.android.com/devices/sensors/sensor-types.html
    This sensor also uses accelerometer and magnetometer input to make up for gyroscope drift, and it cannot be implemented using only the accelerometer and magnetometer.
    */

    @Override
    public void onSensorChanged(SensorEvent event) {


        switch(event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                //float[] lastRotVal = new float[5];
                //System.arraycopy(event.values, 0, lastRotVal, 0, event.values.length);

                //SensorManager.getRotationMatrixFromVector(rotation, lastRotVal);
                synchronized (SensorHandler.this) {
                    SensorManager.getQuaternionFromVector(quaternion, event.values);
                }
                //Log.e("ROT ACCURACY:","deg:"+Math.toDegrees(lastRotVal[4]));
                /*
                Log.e("SENSORHANDLER","len:"+event.values.length);
                try{
                    System.arraycopy(event.values, 0, lastRotVal, 0, event.values.length);
                } catch (IllegalArgumentException e) {
                    //Hardcode the size to handle a bug on Samsung devices running Android 4.3
                    System.arraycopy(event.values, 0, lastRotVal, 0, 3);
                }
                */
                //gyroMatrix= matrixMultiplication(gyroMatrix,rotation);
                //SensorManager.getOrientation(rotation, orientation);

                //double pitch = orientation[1];
                //double roll = orientation[2];
                //double azimuth = orientation[3];

                //Use them for something!
                break;
            case Sensor.TYPE_LIGHT:
                if(lightValues==null)lightValues=new float[event.values.length];
                System.arraycopy(event.values, 0, lightValues, 0, event.values.length);
                /*
                for(int i=0; i< event.values.length; i++){
                    Log.e("LIGHT",i+") "+event.values[i]);
                }
                */
                break;

        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private HandlerThread sensorListenerThread;
    private Handler mSensorHandler;
    //The initialisation of the sensor listeners happens in the initListeners() method:
    public void initListeners(){


        rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorListenerThread=new HandlerThread("sensorListenerThread", Thread.MAX_PRIORITY);
        sensorListenerThread.start();
        mSensorHandler= new Handler(sensorListenerThread.getLooper());
        mSensorManager.registerListener(this,
                rotationSensor,
                SensorManager.SENSOR_DELAY_FASTEST,
                mSensorHandler);
        /*
        mSensorManager.registerListener(this,
                rotationSensor,
                SensorManager.SENSOR_DELAY_FASTEST);
        */
        lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(
                    this,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_UI,
                    mSensorHandler);

    }


    public void unregisterListeners(){

        if(rotationSensor!=null)
            mSensorManager.unregisterListener(this, rotationSensor);

        if(lightSensor!=null)
            mSensorManager.unregisterListener(this,lightSensor);

        sensorListenerThread.quitSafely();
    }




    class SampleOrientationTask extends TimerTask {
        @Override
        public void run() {

            float[] rotation;
            synchronized (SensorHandler.this) {
                rotation = quatToMatrix(new Quat4d(quaternion[0], quaternion[1], quaternion[2], quaternion[3]));
            }
            activity.onNewOrientationMatrixComputed(rotation);


            //activity.onNewOrientationAnglesComputed(f2f.floatValue(),f1f.floatValue(),f0f.floatValue(),inclination>90);



            float [] orientation = new float[3];
            SensorManager.getOrientation(rotation, orientation);

            //double pitch = orientation[0];
            //double roll = orientation[1];
            //double azimuth = orientation[2];
            double roll = Math.toDegrees(orientation[0]);
            double pitch= Math.toDegrees(orientation[1]);
            double azimuth= Math.toDegrees(orientation[2]);


            // Convert the heading (which is relative to magnetic north) to one that is
            // relative to true north, using the user's current location to compute this.
            float magneticHeading = (float) Math.toDegrees(azimuth);
            //float mHeading = MathUtils.mod(computeTrueNorth(magneticHeading), 360.0f) - ARM_DISPLACEMENT_DEGREES;

            // update sensor output GUI
            activity.showOrientationFusedAngle(roll,pitch,azimuth);



            if(lightValues==null)
                return;
            else if(lightValues.length==3)
                activity.onNewLightValues(lightValues[0],lightValues[1],lightValues[2]);
            else if(lightValues.length==1)
                activity.onNewLightValues(lightValues[0],0,0);


        }


        private float[] quatToMatrix(Quat4d q){
            float[] rotation = new float[9];

            double sqw = q.w*q.w;
            double sqx = q.x*q.x;
            double sqy = q.y*q.y;
            double sqz = q.z*q.z;


            //rotation[0] = 1.0f; rotation[1] = 0.0f; rotation[2] = 0.0f;
            //rotation[3] = 0.0f; rotation[4] = 1.0f; rotation[5] = 0.0f;
            //rotation[6] = 0.0f; rotation[7] = 0.0f; rotation[8] = 1.0f;

            // invs (inverse square length) is only required if quaternion is not already normalised
            double invs = 1 / (sqx + sqy + sqz + sqw);
            rotation[0] = (float)(( sqx - sqy - sqz + sqw)*invs) ; // since sqw + sqx + sqy + sqz =1/invs*invs
            rotation[4] = (float)((-sqx + sqy - sqz + sqw)*invs) ;
            rotation[8] = (float)((-sqx - sqy + sqz + sqw)*invs) ;

            double tmp1 = q.x*q.y;
            double tmp2 = q.z*q.w;



            rotation[3] = (float)(2.0 * (tmp1 + tmp2)*invs) ;
            rotation[1] = (float)(2.0 * (tmp1 - tmp2)*invs) ;

            tmp1 = q.x*q.z;
            tmp2 = q.y*q.w;
            rotation[6] = (float)(2.0 * (tmp1 - tmp2)*invs) ;
            rotation[2] = (float)(2.0 * (tmp1 + tmp2)*invs) ;
            tmp1 = q.y*q.z;
            tmp2 = q.x*q.w;
            rotation[7] = (float)(2.0 * (tmp1 + tmp2)*invs) ;
            rotation[5] = (float)(2.0 * (tmp1 - tmp2)*invs) ;

            return rotation;
        }

        private class Quat4d{

            public float x,y,z,w;
            public Quat4d(float w,float x,float y,float z){

                this.x=x;
                this.y=y;
                this.z=z;
                this.w=w;

            }

        }

    }


    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }








}
