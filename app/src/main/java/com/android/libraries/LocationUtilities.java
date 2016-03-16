package com.android.libraries;

/**
 * Created by Mario Salierno on 14/03/2016.
 */
public class LocationUtilities {

    /*
    All these formulas are for calculations on the basis of a spherical earth (ignoring ellipsoidal effects)
    using a spherical model gives errors typically up to 0.3%
    */
    /*
    When I tested haversine vs equirectangular over much larger distances (1000km), the difference was on the order of 0.1%.
    So for use equirectangular for speed unless you have a need for maximum accuracy.
    */


    //R = earth’s radius (mean radius = 6371km)
    private static final int R = 6371;


    /**
     * This is the implementation Haversine Distance Algorithm between two places
     *  R = earth’s radius (mean radius = 6,371km)
        Δlat = lat2− lat1
        Δlong = long2− long1
        a = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
        c = 2.atan2(√a, √(1−a))
        d = R.c
     *
     */
    public static double getHaversineDistance(double lat1, double lon1,
                                              double lat2, double lon2){
        Double latDistance = toRad(lat2-lat1);
        Double lonDistance = toRad(lon2-lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        Double distance = R * c;
        return distance;
        //kilometers
    }

    private static Double toRad(Double value) {
        return value * Math.PI / 180;
    }

    /*
    private static double toDegree(double value){

    //    double pk = (double) (180.f/Math.PI);
    }
    */

    /*
     lat, longs are in radians
     */
    public static double getSphericalLawOfCosinesDistance(double lat1, double lon1,
                                                          double lat2, double lon2) {

        return R * Math.acos(Math.sin(lat1) * Math.sin(lat2) +
               Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));
    }


    public static double getEquirectangularApproximationDistance(double lat1, double lon1,
                                                                 double lat2, double lon2) {

        Double latDistance = toRad(lat2-lat1);//convert lat/lon to radians
        Double lonDistance = toRad(lon2-lon1);//convert lat/lon to radians


        double p1 = (lonDistance)* Math.cos ( 0.5*(toRad(lat1+lat2)) );//convert lat/lon to radians
        double p2 = latDistance;
        double distance = R * Math.sqrt( p1*p1 + p2*p2);
        return distance;

    }

    /*
    public static double getEquirectangularApproximationDistance(double lat1, double lon1,
                                                                 double lat2, double lon2) {



        double p1 = (lon1 - lon2)* Math.cos ( 0.5*(lat1+lat2) );//convert lat/lon to radians
        double p2 = (lat1 - lat2);
        double distance = R * Math.sqrt( p1*p1 + p2*p2);
        return distance;

    }
    */
}
