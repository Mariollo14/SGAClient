package com.msali.AR.sga.location;

import android.location.Location;

/**
 * Created by Mario Salierno on 11/07/2016.
 */
//https://en.wikipedia.org/wiki/Moving_average
public class WeightedMovingAverageFilter implements LocationFilter {

    private Double lat = null;
    private Double lng = null;
    private Double weights = null;

    private final int MAX_SAMPLE_NUMBER = 100;
    private int SAMPLE_NUMBER = 1;

    @Override
    public void filter(Location loc) {


        if(loc==null)return;

        if(lat==null||lng==null){
            double p = 1/loc.getAccuracy();
            lat=loc.getLatitude()*p;
            lng=loc.getLongitude()*p;
            weights = p;
            SAMPLE_NUMBER=1;
            return;
        }

        float p = 1/loc.getAccuracy();
        lat += loc.getLatitude() * p;
        lng += loc.getLongitude() * p;
        weights += p;

        double newLat = lat / weights;
        double newLng = lng / weights;

        loc.setLatitude(newLat);
        loc.setLongitude(newLng);


        if (SAMPLE_NUMBER >= MAX_SAMPLE_NUMBER) {
            lat = null;
            lng = null;
            weights = null;
            SAMPLE_NUMBER=1;
            return;
        }

        SAMPLE_NUMBER++;

    }
}
