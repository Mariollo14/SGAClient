package com.msali.AR.sga.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.android.libraries.TextureFromCameraActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


/**
 * Created by Mario Salierno on 05/07/2016.
 */

//
public class GoogleServicesLocator implements GoogleApiClient.ConnectionCallbacks,
                                                //GooglePlayServicesClient.ConnectionCallbacks,
                                                GoogleApiClient.OnConnectionFailedListener,
                                                //GooglePlayServicesClient.OnConnectionFailedListener,
                                                com.google.android.gms.location.LocationListener,
                                                GeoLocator{


    private TextureFromCameraActivity activity;
    private final String TAG = "GoogleServicesLocator";


    /*
    Notice that the below code snippet refers to a boolean flag, mRequestingLocationUpdates,
    used to track whether the user has turned location updates on or off.
    */
    private boolean mRequestingLocationUpdates = false;
    private LocationRequest mLocationRequest;
    private static long INTERVAL = 2000;
    private static long FATEST_INTERVAL = 500;
    private GoogleApiClient mGoogleApiClient;
    private Location currLocation=null;
    private static int ACCURACY_THRESHOLD = 300;
    private static final int MIN_ACCURACY = 30;




    public GoogleServicesLocator(TextureFromCameraActivity activity, boolean mRequestingLocationUpdates){
        this.activity=activity;
        this.mRequestingLocationUpdates=mRequestingLocationUpdates;
        init();
        this.connect();
    }

    public void setRequestingLocationUpdates(boolean request){
        mRequestingLocationUpdates=request;
    }

    public boolean isRequestingLocationUpdates(){
        return mRequestingLocationUpdates;
    }

    @Override
    public void startUpdates() {

        if (isConnected() && !isRequestingLocationUpdates()) {

            startLocationUpdates();
        }

    }

    @Override
    public void stopUpdates() {
        stopLocationUpdates();
    }

    public Location getCurrentLocation(){
        return currLocation;
    }

    @Override
    public Location requestAsynchLocationUpdate() {
        if(!mGoogleApiClient.isConnected()){
            connect();
        }
        long lastI = INTERVAL;
        long lastFI= FATEST_INTERVAL;
        if(!mGoogleApiClient.isConnected())return currLocation;

        FATEST_INTERVAL=10;
        INTERVAL=10;

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
            INTERVAL = lastI;
            FATEST_INTERVAL= lastFI;

        }
        INTERVAL = lastI;
        FATEST_INTERVAL= lastFI;

        return currLocation;
    }

    @Override
    public boolean canGetLocation() {
        return mGoogleApiClient.isConnected();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location!=null){

            if(location.getAccuracy()<=Math.max(ACCURACY_THRESHOLD,MIN_ACCURACY)) {
                //String loc = "lat:" + location.getLatitude() + " long:" + location.getLongitude() + " alt:" + location.getAltitude();
                //Log.e(TAG,loc);
                currLocation = location;
                ACCURACY_THRESHOLD=(int)location.getAccuracy();
                //Log.e("LOCATION UPDATED acc:"+ACCURACY_THRESHOLD, loc );
            }
        }

    }



    /*
    Notice that the below code snippet refers to a boolean flag, mRequestingLocationUpdates,
    used to track whether the user has turned location updates on or off.
    */
    @Override
    public void onConnected(@Nullable Bundle bundle) {


        Log.e(TAG, "connection done");
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.e(TAG, "connection failed");
    }


    private void init(){
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        createLocationRequest();
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void connect(){

        mGoogleApiClient.connect();

    }

    public boolean isConnected(){
        return mGoogleApiClient.isConnected();
    }

    public void disconnect(){
        mGoogleApiClient.disconnect();
    }

    public boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, activity, 0).show();
            return false;
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

            Log.e(TAG, "Location update started ..............: ");
            mRequestingLocationUpdates=true;
        }
        else
            Log.e(TAG, "Unable to start google location update ..............: ");
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        mRequestingLocationUpdates=false;
    }

}
