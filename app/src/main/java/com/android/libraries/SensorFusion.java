package com.android.libraries;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Mario Salierno on 12/03/2016.
 */
public class SensorFusion implements SensorEventListener {

    //activity context
    TextureFromCameraActivity activity;


    private SensorManager mSensorManager = null;

    // angular speeds from gyro
    private float[] gyro = new float[3];

    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];

    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];

    // magnetic field vector
    private float[] magnet = new float[3];

    // accelerometer vector
    private float[] accel = new float[3];

    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];

    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];



    public SensorFusion(TextureFromCameraActivity act) {
        this.activity=act;

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) activity.getSystemService(activity.SENSOR_SERVICE);
        initListeners();


        //timer for the filter task
        //see class calculateFusedOrientationTask
        // wait for one second until gyroscope and magnetometer/accelerometer
        // data is initialised then scedule the complementary filter task
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                1000, TIME_CONSTANT);
    }


    private Sensor accSensor, gyroSensor, magnSensor;
    //The initialisation of the sensor listeners happens in the initListeners() method:
    public void initListeners(){
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this,
                accSensor,
                SensorManager.SENSOR_DELAY_FASTEST);

        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this,
                gyroSensor,
                SensorManager.SENSOR_DELAY_FASTEST);

        magnSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this,
                magnSensor,
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregisterListeners(){
        if(accSensor!=null)
            mSensorManager.unregisterListener(this, accSensor);
        if(gyroSensor!=null)
            mSensorManager.unregisterListener(this, gyroSensor);
        if(magnSensor!=null)
            mSensorManager.unregisterListener(this, magnSensor);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array
                // then calculate new orientation
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                // process gyro data
                gyroFunction(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                // copy new magnetometer data into magnet array
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;
        }

        activity.sensorChanged(event);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

        activity.accuracyChanged(sensor, accuracy);
    }


    /*
    The Android API provides us with very handy functions to get the absolute orientation from the accelerometer and magnetometer.
    This is all we need to do to get the accelerometer/magnetometer based orientaion:
     */
    // calculates orientation angles from accelerometer and magnetometer output
    public void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }


    public static final float EPSILON = 0.000000001f;


    /*
     As described in the Android reference page shows, how to get a rotation vector from the gyroscope data (see Sensor.TYPE_GYROSCOPE).
     The function creates a rotation vector which is similar to a quaternion.
     It expresses the rotation interval of the device between the last and the current gyroscope measurement.
     The rotation speed is multiplied with the time interval — here it’s the parameter timeFactor — which passed since the last measurement.
     this function is then called in the actual gyroFunction() for gyro sensor data processing.
     This is where the gyroscope rotation intervals are added to the absolute gyro based orientation. But since we have rotation matrices instead of angles this can’t be done by simply adding the rotation intervals. We need to apply the rotation intervals by mytrix multiplication:
     */
    // This function is borrowed from the Android reference
    // at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
    // It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }



    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }


    /*
    The gyroscope data is not processed until orientation angles from the accelerometer and magnetometer is available (in the member variable accMagOrientation).
    This data is required as the initial orientation for the gyroscope data. Otherwise, our orientation matrix will contain undefined values.
    The device’s current orientation and the calcuated gyro rotation vector are transformed into a rotation matrix.
    The gyroMatrix is the total orientation calculated from all hitherto processed gyroscope measurements.
    The deltaMatrix holds the last rotation interval which needs to be applied to the gyroMatrix in the next step.
    This is done by multiplying gyroMatrix with deltaMatrix. This is equivalent to the Rotation of gyroMatrix about deltaMatrix.
    The matrixMultiplication method is described further below. Do not swap the two parameters of the matrix multiplication, since matrix multiplications are not commutative.
     */

    /*
    The rotation vector can be converted into a matrix by calling the conversion function getRotationMatrixFromVector from the SensoManager.
    The same thing is done by this method getRotationMatrixFromOrientation:
     */
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
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
    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer = new Timer();

    /*
    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            fusedOrientation[0] =
                    FILTER_COEFFICIENT * gyroOrientation[0]
                            + oneMinusCoeff * accMagOrientation[0];

            fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1]
                            + oneMinusCoeff * accMagOrientation[1];

            fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2]
                            + oneMinusCoeff * accMagOrientation[2];

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        }
    }
    */

    class calculateFusedOrientationTask extends TimerTask {

        private DecimalFormat dformat = new DecimalFormat("#.##");

        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

            /*
             * Fix for 179° <--> -179° transition problem:
             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360° (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360° from the result
             * if it is greater than 180°. This stabilizes the output in positive-to-negative-transition cases.
             */

            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }

            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }

            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
                fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);


            //mAzimuthView
            double f0f = fusedOrientation[0] * 180 / Math.PI;
            String f0 = dformat.format(f0f);
            //mPitchView
            double f1f = fusedOrientation[1] * 180 / Math.PI;
            String f1 = dformat.format(f1f);
            //mRollView
            double f2f = fusedOrientation[2] * 180 / Math.PI;
            String f2 = dformat.format(f2f);
            // update sensor output in GUI
            //mHandler.post(updateOreintationDisplayTask);
            activity.showOrientationFusedAngle(f0f,f1f,f2f);
        }
    }
}
