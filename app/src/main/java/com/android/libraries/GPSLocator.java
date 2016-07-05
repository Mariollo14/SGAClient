package com.android.libraries;

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
import android.support.v4.content.ContextCompat;
import android.util.Log;

/**
 * Created by Mario Salierno on 13/03/2016.
 */
public class GPSLocator implements LocationListener {

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
    private static final long MIN_TIME_BW_UPDATES = 200; //200ms

    private static final int ATTEMPT_TO_GET_BEST_INITIAL_ACCURACY = 10;

    private static final int ACCURACY_THRESHOLD = 10;



    public GPSLocator(TextureFromCameraActivity act, SimParameters simulation) {
        activity = act;
        this.simulation = simulation;

        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);


        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
        locationManager.getBestProvider(criteria, true);

        Location temp=null;
        for(int i = 0; i < ATTEMPT_TO_GET_BEST_INITIAL_ACCURACY; i++){
            temp=requestLocationUpdate();

            if(temp!=null && location!=null){
                Log.e("GPS ACCURACY","ray:"+temp.getAccuracy());
                if(temp.getAccuracy()<location.getAccuracy())location=temp;
                continue;
            }
            if(location==null)location=temp;
        }
        //location = requestLocationUpdate();


    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app.
     * */
    public void stopUsingGPS() {
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
            if(newLocation.getAccuracy()<=ACCURACY_THRESHOLD) {
                String loc = "lat:" + newLocation.getLongitude() + " long:" + newLocation.getLongitude() + " alt:" + newLocation.getAltitude();
                location = newLocation;
                TextureFromCameraActivity.MainHandler mainH = activity.getMainHandler();
                if (mainH != null)
                    mainH.sendLocalization(loc);
                //Log.e("LOCATION UPDATED", loc );
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

    public Location getLocation(){
        return location;
    }

    public Location requestLocationUpdate() {
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
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);



            if(!isGPSEnabled)
                showSettingsAlert();

            if (!isGPSEnabled && !isNetworkEnabled) {
                // No network provider is enabled
                Log.d("GPSLocator", "nor gps or network is enabled");
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
        catch (Exception e) {
            e.printStackTrace();
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


    /**
     * Function to show settings alert dialog.
     * On pressing the Settings button it will launch Settings Options.
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing the Settings button.
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
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
}
