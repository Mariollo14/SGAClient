package com.msali.AR.sga.location;

import android.location.Location;

/**
 * Created by Mario Salierno on 20/10/2016.
 */
public class StaticPositionLocator implements GeoLocator {

    private Location loc = null;

    public StaticPositionLocator(Location l){
        loc = l;
        loc.setAccuracy(1);
    }

    @Override
    public void startUpdates() {

    }

    @Override
    public void stopUpdates() {

    }

    @Override
    public Location getCurrentLocation() {

        loc.setTime(System.currentTimeMillis());
        return loc;
    }

    @Override
    public Location requestAsynchLocationUpdate() {
        loc.setTime(System.currentTimeMillis());
        return loc;
    }

    @Override
    public boolean canGetLocation() {
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        loc = location;
        loc.setTime(System.currentTimeMillis());

    }
}
