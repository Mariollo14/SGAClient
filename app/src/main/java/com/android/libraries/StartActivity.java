package com.android.libraries;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.sga.R;
import com.msali.AR.sga.SimParameters;
import com.msali.AR.sga.location.LocationFusionStrategy;

import java.lang.ref.WeakReference;


public class StartActivity extends AppCompatActivity {


    private static final String TAG = "StartActivity";
    /*
    The following constants are used to identify permission request by onRequestPermissionResult callback
    */
    //Id to identify fine location permission request.
    public static final int REQUEST_FINE_LOC = 0;
    //Id to identify coarse location permission request.
    public static final int REQUEST_COARSE_LOC = 1;
    //Id to identify a camera permission request.
    private static final int REQUEST_CAMERA = 2;
    //Id to identify a camera permission request.
    private static final int REQUEST_INTERNET = 3;


    public boolean hasCameraPermission = false;
    public boolean hasLocalizationPermission = false;
    public boolean hasInternetPermission = false;


    private TextView startTV;
    private UIHandler startUIHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);


        startTV = (TextView) this.findViewById(R.id.start_act_textv);


        startUIHandler = new UIHandler(this);

    }


    @Override
    protected void onResume() {

        super.onResume();


        requestCameraPermission();
        requestInternetPermission();
        requestLocalizationPermissions();


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Custom message handler for main UI thread.
     * <p/>
     * Receives messages from the renderer thread with UI-related updates, like the camera
     * parameters (which we show in a text message on screen).
     */
    private static class UIHandler extends Handler {
        private static final int MSG_START = 0;

        private WeakReference<StartActivity> mWeakActivity;

        public UIHandler(StartActivity activity) {
            mWeakActivity = new WeakReference<StartActivity>(activity);
        }

        public void updateStartMsgMessage(String mex) {
            sendMessage(obtainMessage(MSG_START, mex));
        }

        @Override
        public void handleMessage(Message msg) {
            final StartActivity activity = mWeakActivity.get();
            if (activity == null) {
                return;
            }

            switch (msg.what) {

                case MSG_START: {
                    //Log.e(TAG, "cam:"+activity.hasCameraPermission+" loc:"+activity.hasLocalizationPermission+" int:"+activity.hasInternetPermission);
                    if (activity.hasCameraPermission && activity.hasLocalizationPermission && activity.hasInternetPermission) {
                        Log.e(TAG, "msg.arg1:" + (String) msg.obj);
                        activity.startTV.setText((String) msg.obj);
                        activity.startTV.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                Log.e(TAG,"Start Clicked");
                                activity.startTV.setClickable(false);
                                // The Very Basic
                                new AsyncTask<Void, Void, Void>() {
                                    protected void onPreExecute() {
                                        // Pre Code
                                        Log.e(TAG,"Start Executed");
                                    }

                                    protected Void doInBackground(Void... unused) {
                                        // Background Code


                                        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
                                        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                            // TODO: Consider calling
                                            //    ActivityCompat#requestPermissions
                                            // here to request the missing permissions, and then overriding
                                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            //                                          int[] grantResults)
                                            // to handle the case where the user grants the permission. See the documentation
                                            // for ActivityCompat#requestPermissions for more details.
                                            return null;
                                        }
                                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                        if(lastKnownLocation==null)
                                            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);


                                        if(lastKnownLocation==null){
                                            lastKnownLocation = new Location(TAG);
                                            lastKnownLocation.setLatitude(SimParameters.LAT_ORIGIN);
                                            lastKnownLocation.setLongitude(SimParameters.LON_ORIGIN);
                                            lastKnownLocation.setAltitude(SimParameters.ALT_ORIGIN);
                                        }
                                        //LocationFusionStrategy.getAltitudeThroughService(lastKnownLocation.getLongitude(),lastKnownLocation.getLatitude());
                                        LocationFusionStrategy.getAltitudeFromGoogleMaps(lastKnownLocation.getLongitude(),lastKnownLocation.getLatitude());
                                        return null;
                                    }
                                    protected void onPostExecute(Void unused) {
                                        // Post Code
                                        Intent intent = new Intent(activity, TextureFromCameraActivity.class);

                                        activity.startActivity(intent);

                                        activity.startTV.setClickable(true);
                                    }
                                }.execute();


                            }
                        });
                        activity.startTV.setClickable(true);
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Unknown message " + msg.what);
            }
        }
    }





    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    hasLocalizationPermission=true;
                    startUIHandler.updateStartMsgMessage(getString(R.string.start_message));

                } else {
                    Log.e(TAG, "Fine Location Permission denied. Handle it at onRequestPermissionResult");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    makeToast("Fine location permission denied");

                    //onPause();
                    //finish();

                }
                //return;
            }
            break;
            case REQUEST_COARSE_LOC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    hasLocalizationPermission=true;
                    startUIHandler.updateStartMsgMessage(getString(R.string.start_message));

                } else {
                    Log.e(TAG, "Coarse Location Permission denied. Handle it at onRequestPermissionResult");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    makeToast("Coarse location permission denied");
                    //onPause();
                    //finish();
                }
                //return;
            }
            break;
            case REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    hasCameraPermission=true;
                    startUIHandler.updateStartMsgMessage(getString(R.string.start_message));

                } else {

                    Log.e(TAG, "Camera Permission denied. Handle it at onRequestPermissionResult");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    makeToast("Camera Permission denied");
                    //onPause();
                    //finish();
                }
                //return;
            }
            break;
            case REQUEST_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    hasInternetPermission=true;
                    startUIHandler.updateStartMsgMessage(getString(R.string.start_message));

                } else {

                    Log.e(TAG, "INTERNET Permission denied. Handle it at onRequestPermissionResult");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    makeToast("Internet Permission denied");
                    //onPause();
                    //finish();
                }
                //return;
            }
            break;

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    private void requestCameraPermission() {


        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(StartActivity.this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {


            //requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);



            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(StartActivity.this,
                    Manifest.permission.CAMERA)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                //ActivityCompat.requestPermissions(StartActivity.this, new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA);

                hasCameraPermission=false;
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else{

            hasCameraPermission=true;
            startUIHandler.updateStartMsgMessage(getString(R.string.start_message));

        }


    }


    private void requestInternetPermission() {


        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(StartActivity.this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            //requestPermissions(new String[] {Manifest.permission.INTERNET}, REQUEST_INTERNET);


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET);

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(StartActivity.this,
                    Manifest.permission.INTERNET)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                //ActivityCompat.requestPermissions(StartActivity.this,new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET);

                hasInternetPermission=false;
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else{

            hasInternetPermission=true;
            startUIHandler.updateStartMsgMessage(getString(R.string.start_message));
        }


    }

    public void requestLocalizationPermissions(){

        /*

        requestPermissions(new String[] {Manifest.permission.WRITE_CONTACTS},
                REQUEST_COARSE_LOC);

        requestPermissions(new String[] {Manifest.permission.WRITE_CONTACTS},
                REQUEST_FINE_LOC);
        */
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOC);


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            }
            else {

                // No explanation needed, we can request the permission.

                //ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOC);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else{
            hasLocalizationPermission=true;
            startUIHandler.updateStartMsgMessage(getString(R.string.start_message));
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOC);


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.


            }
            else {

                // No explanation needed, we can request the permission.

                //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOC);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else{

            hasLocalizationPermission=true;
            startUIHandler.updateStartMsgMessage(getString(R.string.start_message));
        }


    }

    public void makeToast(String s) {
        Context context = getApplicationContext();
        CharSequence text = s;
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }


}
