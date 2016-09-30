package com.msali.AR.sga.location;

import android.location.Location;

/**
 * Created by Mario Salierno on 06/07/2016.
 */
public interface GeoLocator {



    public void startUpdates();
    public void stopUpdates();
    public Location getCurrentLocation();
    public Location requestAsynchLocationUpdate();
    public boolean canGetLocation();
    public void onLocationChanged(Location location);

}
