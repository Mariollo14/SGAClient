package com.msali.AR.sga.location;

import android.location.Location;

/**
 * Created by Mario Salierno on 11/07/2016.
 */

//https://en.wikipedia.org/wiki/Moving_average
public class ExponentialMovingAverageFilter implements LocationFilter{


    private final float alpha = 0.25f;
    private Location lastLoc = null;

    public void filter(Location loc){

        if(loc==null)return;

        if(lastLoc==null){
            lastLoc=loc;
            return;
        }

        double lat = alpha * loc.getLatitude() + (1-alpha)*lastLoc.getLatitude();
        double lng = alpha * loc.getLongitude() + (1-alpha)*lastLoc.getLongitude();

        loc.setLatitude(lat);
        loc.setLongitude(lng);

        lastLoc=new Location(loc);

    }

}
