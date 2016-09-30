package com.msali.AR.sga.location.kalman;

/*
 * KalmanLocationManager
 *
 * Copyright (c) 2014 Renato Villone
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


        import android.content.Context;
        import android.location.Location;
        import android.location.LocationListener;
        import android.location.LocationManager;
        import android.location.LocationProvider;
        import android.os.Bundle;
        import android.util.Log;

        import com.msali.AR.sga.location.GeoLocator;
        import com.android.libraries.TextureFromCameraActivity;
        import com.google.android.gms.maps.LocationSource;

        import java.util.HashMap;
        import java.util.Map;

/**
 * Provides a means of requesting location updates.
 * <p>
 * Similar to Android's {@link android.location.LocationManager LocationManager}.
 */
//http://stackoverflow.com/questions/1134579/smooth-gps-data/15657798#15657798
//https://github.com/villoren/KalmanLocationManager/blob/master/app/src/main/java/com/villoren/android/kalmanlocationmanager/app/MainActivity.java
public class KalmanLocator implements GeoLocator {

    /**
     * Specifies which of the native location providers to use, or a combination of them.
     */
    public enum UseProvider { GPS, NET, GPS_AND_NET }

    /**
     * Provider string assigned to predicted Location objects.
     */
    public static final String KALMAN_PROVIDER = "kalman";

    /**
     * Logger tag.
     */
    private static final String TAG = KalmanLocator.class.getSimpleName();


    /**
     * Map that associates provided LocationListeners with created LooperThreads.
     */
    private final Map<LocationListener, LooperThread> mListener2Thread;


    // GoogleMaps own OnLocationChangedListener (not android's LocationListener)
    private LocationSource.OnLocationChangedListener mOnLocationChangedListener;

    private Location currentLocation=null;
    /**
     * The Context the KalmanLocationManager is running in.
     */
    private final Context mContext;
    private final TextureFromCameraActivity activity;
    // Constant

    /**
     * Request location updates with the highest possible frequency on gps.
     * Typically, this means one update per second for gps.
     */
    private static final long GPS_TIME = 1000;

    /**
     * For the network provider, which gives locations with less accuracy (less reliable),
     * request updates every 5 seconds.
     */
    private static final long NET_TIME = 5000;

    /**
     * For the filter-time argument we use a "real" value: the predictions are triggered by a timer.
     * Lets say we want 5 updates (estimates) per second = update each 200 millis.
     */
    private static final long FILTER_TIME = 200;


    /**
     * Constructor.
     *
     * @param context The Context for this KalmanLocationManager.
     */
    public KalmanLocator(Context context, TextureFromCameraActivity act) {

        mContext = context;
        mListener2Thread = new HashMap<LocationListener, LooperThread>();
        activity = act;



    }

    /**
     * Register for {@link android.location.Location Location} estimates using the given LocationListener callback.
     *
     *
     * @param useProvider Specifies which of the native location providers to use, or a combination of them.
     *
     * @param minTimeFilter Minimum time interval between location estimates, in milliseconds.
     *                      Indicates the frequency of predictions to be calculated by the filter,
     *                      thus the frequency of callbacks to be received by the given location listener.
     *
     * @param minTimeGpsProvider Minimum time interval between GPS readings, in milliseconds.
     *                           If {@link UseProvider#NET UseProvider.NET} was set, this value is ignored.
     *
     * @param minTimeNetProvider Minimum time interval between Network readings, in milliseconds.
     *                           If {@link UseProvider#GPS UseProvider.GPS} was set, this value is ignored.
     *
     * @param listener A {@link android.location.LocationListener LocationListener} whose
     *                 {@link android.location.LocationListener#onLocationChanged(android.location.Location) onLocationChanged(Location)}
     *                 method will be called for each location estimate produced by the filter. It will also receive
     *                 the status updates from the native providers.
     *
     * @param forwardProviderReadings Also forward location readings from the native providers to the given listener.
     *                                Note that <i>status</i> updates will always be forwarded.
     *
     */
    public void requestLocationUpdates(
            UseProvider useProvider,
            long minTimeFilter,
            long minTimeGpsProvider,
            long minTimeNetProvider,
            LocationListener listener,
            boolean forwardProviderReadings)
    {
        // Validate arguments
        if (useProvider == null)
            throw new IllegalArgumentException("useProvider can't be null");

        if (listener == null)
            throw new IllegalArgumentException("listener can't be null");

        if (minTimeFilter < 0) {

            Log.w(TAG, "minTimeFilter < 0. Setting to 0");
            minTimeFilter = 0;
        }

        if (minTimeGpsProvider < 0) {

            Log.w(TAG, "minTimeGpsProvider < 0. Setting to 0");
            minTimeGpsProvider = 0;
        }

        if (minTimeNetProvider < 0) {

            Log.w(TAG, "minTimeNetProvider < 0. Setting to 0");
            minTimeNetProvider = 0;
        }

        // Remove this listener if it is already in use
        if (mListener2Thread.containsKey(listener)) {

            Log.d(TAG, "Requested location updates with a listener that is already in use. Removing.");
            removeUpdates(listener);
        }

        LooperThread looperThread = new LooperThread(
                mContext, useProvider, minTimeFilter, minTimeGpsProvider, minTimeNetProvider,
                listener, forwardProviderReadings);

        mListener2Thread.put(listener, looperThread);
    }

    /**
     * Removes location estimates for the specified LocationListener.
     * <p>
     * Following this call, updates will no longer occur for this listener.
     *
     * @param listener Listener object that no longer needs location estimates.
     */
    public void removeUpdates(LocationListener listener) {

        LooperThread looperThread = mListener2Thread.remove(listener);

        if (looperThread == null) {

            Log.d(TAG, "Did not remove updates for given LocationListener. Wasn't registered in this instance.");
            return;
        }

        looperThread.close();
    }


    @Override
    public void startUpdates() {

        requestLocationUpdates(
                UseProvider.GPS_AND_NET, FILTER_TIME, GPS_TIME, NET_TIME, mLocationListener, true);
    }

    @Override
    public void stopUpdates() {
        removeUpdates(mLocationListener);
    }

    @Override
    public Location getCurrentLocation() {
        return currentLocation;
    }

    @Override
    public Location requestAsynchLocationUpdate() {
        return currentLocation;
    }

    @Override
    public boolean canGetLocation() {
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocationListener.onLocationChanged(location);
    }

    /**
     * Listener used to get updates from KalmanLocationManager (the good old Android LocationListener).
     */
    private LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location newLocation) {

            //Log.e("LOCATION CHANGED!!", loc );

            //if(newLocation.getAccuracy()<50){}
            if (newLocation != null) {
                String loc = "lat:" + newLocation.getLatitude() + " long:" + newLocation.getLongitude() + " alt:" + newLocation.getAltitude();
                currentLocation = newLocation;
                TextureFromCameraActivity.MainHandler mainH = activity.getMainHandler();
                if (mainH != null)
                    mainH.sendLocalization(loc);
                //Log.e("LOCATION UPDATED", loc );
            }


            //LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // GPS location
            if (newLocation.getProvider().equals(LocationManager.GPS_PROVIDER)) {

            }

            // Network location
            if (newLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {


            }

            // If Kalman location and google maps activated the supplied mLocationSource
            if (newLocation.getProvider().equals(KalmanLocator.KALMAN_PROVIDER)
                    && mOnLocationChangedListener != null) {


                mOnLocationChangedListener.onLocationChanged(newLocation);


            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

            String statusString = "Unknown";

            switch (status) {

                case LocationProvider.OUT_OF_SERVICE:
                    statusString = "Out of service";
                    break;

                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    statusString = "Temporary unavailable";
                    break;

                case LocationProvider.AVAILABLE:
                    statusString = "Available";
                    break;
            }

            //Toast.makeText(MainActivity.this, String.format("Provider '%s' status: %s", provider, statusString), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {

            //Toast.makeText(MainActivity.this, String.format("Provider '%s' enabled", provider), Toast.LENGTH_SHORT).show();

            // Remove strike-thru in label
            if (provider.equals(LocationManager.GPS_PROVIDER)) {


            }

            if (provider.equals(LocationManager.NETWORK_PROVIDER)) {


            }
        }

        @Override
        public void onProviderDisabled(String provider) {

            //Toast.makeText(MainActivity.this, String.format("Provider '%s' disabled", provider), Toast.LENGTH_SHORT).show();

            // Set strike-thru in label and hide accuracy circle
            if (provider.equals(LocationManager.GPS_PROVIDER)) {


            }

            if (provider.equals(LocationManager.NETWORK_PROVIDER)) {


            }
        }
    };


    /**
     * Location Source for google maps 'my location' layer.
     */
    private LocationSource mLocationSource = new LocationSource() {

        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {

            mOnLocationChangedListener = onLocationChangedListener;
        }

        @Override
        public void deactivate() {

            mOnLocationChangedListener = null;
        }
    };


}