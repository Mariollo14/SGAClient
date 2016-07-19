package com.android.libraries.location;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.android.libraries.SimParameters;
import com.android.libraries.TextureFromCameraActivity;

/**
 * Created by Mario Salierno on 13/03/2016.
 */
public class GPSLocator implements LocationListener, GeoLocator {

    private String TAG = "GPSLocator";

    private LocationManager locationManager;
    private TextureFromCameraActivity activity;
    private SimParameters simulation;

    // Flag for GPS status
    boolean isGPSEnabled = false;
    // Flag for network status
    boolean isNetworkEnabled = false;
    // Flag for GPS status
    boolean canGetLocation = false;

    Location location; // Location
    double latitude; // Latitude
    double longitude; // Longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 2 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000; //1sec

    private static final int ATTEMPT_TO_GET_BEST_INITIAL_ACCURACY = 10;

    private static int ACCURACY_THRESHOLD = 300;
    private static final int MIN_ACCURACY = 30;

    private boolean askedOnce = false;


    public GPSLocator(TextureFromCameraActivity act, SimParameters simulation) {
        activity = act;
        this.simulation = simulation;

        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);


        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
        criteria.setBearingRequired(true);
        criteria.setBearingAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setAltitudeRequired(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.NO_REQUIREMENT);

        locationManager.getBestProvider(criteria, true);

        Location temp = null;
        for (int i = 0; i < ATTEMPT_TO_GET_BEST_INITIAL_ACCURACY; i++) {
            temp = requestLocationUpdate();

            if (temp != null && location != null) {
                Log.e("GPS ACCURACY", "ray:" + temp.getAccuracy());
                if (temp.getAccuracy() < location.getAccuracy()) location = temp;
                continue;
            }
            if (location == null) location = temp;
        }
        if (temp == null) Log.e(TAG, "temp=NULL");
        if (location == null) {
            location = getLastKnownLocation();
            //Log.e(TAG,"location=NULL");
        }
        //location = requestLocationUpdate();


    }


    @Override
    public void startUpdates() {

        requestLocationUpdate();
    }

    @Override
    public void stopUpdates() {

        stopUsingGPS();
        askedOnce = false;
    }

    @Override
    public Location getCurrentLocation() {
        return location;
    }

    @Override
    public Location requestAsynchLocationUpdate() {
        return requestLocationUpdate();
    }

    public Location getLastKnownLocation() {
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

        Location temp = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(temp!=null)
            return temp;
        else
            return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app.
     * */
    private void stopUsingGPS() {
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(GPSLocator.this);
            }

        }
        catch(SecurityException se){
            //this.checkPermissions();
            se.printStackTrace();
        }
    }


    @Override
    public void onLocationChanged(Location newLocation) {
        //Log.e("LOCATION CHANGED!!", loc );


        //if(newLocation.getAccuracy()<50){}
        if(newLocation!=null) {
            if(newLocation.getAccuracy()<=Math.max(ACCURACY_THRESHOLD,MIN_ACCURACY)) {
                String loc = "lat:" + newLocation.getLatitude() + " long:" + newLocation.getLongitude() + " alt:" + newLocation.getAltitude();
                location = newLocation;
                ACCURACY_THRESHOLD=(int)newLocation.getAccuracy();
                //Log.e("LOCATION UPDATED acc:"+ACCURACY_THRESHOLD, loc );
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    private Location requestLocationUpdate() {
        try {
            locationManager = (LocationManager) activity
                    .getSystemService(Context.LOCATION_SERVICE);

            // Getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            //Log.e("GPSENABLED",isGPSEnabled+"");
            /*
            Are you sure you mean signal strength vs. accuracy? What good is the signal strength? Since the GPS position is determined via many satellites, you don't have "one" signal strength.
            So assuming that you really mean signal strength, you can get the GpsStatus via LocationManager.getGpsStatus(), and that gives you a list of satellites via getSatellites()', and each one of those has a signal-to-noise ratio (getSnr()).
            Assuming you mean accuracy, try Location.getAccuracy().
            */

            // Getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


            if(!isGPSEnabled&&!askedOnce) {

                TextureFromCameraActivity.MainHandler mainH = activity.getMainHandler();
                if (mainH != null)
                    mainH.sendSettingsAlertMessage();
                askedOnce=true;
            }
            if (!isGPSEnabled && !isNetworkEnabled) {
                // No network provider is enabled
                Log.e("GPSLocator", "nor gps or network is enabled");
            }
            else {
                this.canGetLocation = true;
                // If GPS enabled, get latitude/longitude using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        //checkPermissions();

                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                return location;
                            }
                        }
                    }
                }
                else if (isNetworkEnabled) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            return location;
                        }
                    }
                }
            }
        }
        catch(SecurityException se){
            se.printStackTrace();
            //checkPermissions();
        }

        return location;
    }



    /**
     * Function to get latitude
     * */
    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }


    /**
     * Function to get longitude
     * */
    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }

    /**
     * Function to check GPS/Wi-Fi enabled
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }



}
