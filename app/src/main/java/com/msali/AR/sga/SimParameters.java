package com.msali.AR.sga;

import android.location.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mario Salierno on 14/03/2016.
 */
//http://www.coordinate-gps.it/
public class SimParameters {


    private String TAG = "SimParameters";
    private Map<String,Location> targetLocations = new HashMap<String,Location>();

    public SimParameters(){

        //test
        //this.addNewTarget("asobrero",45.0808178,7.6655203, 245);
        //this.addNewTarget("csoregina",45.082234,7.666427, 240);
        /*
        this.addNewTarget("pirandello",41.128718,14.794887,174);
        this.addNewTarget("rosina",41.1254395,14.7934637,177);
        this.addNewTarget("flora",41.1287446,14.7892335,172);
        this.addNewTarget("duomo",41.1314897,14.7742939,136);
        this.addNewTarget("traiano",41.1325468,14.7791313,141);
        this.addNewTarget("moscovio",41.132284,14.779063,141);
        this.addNewTarget("nikila",41.131527,14.780288,141);
        this.addNewTarget("pirandello10",41.1292366,14.7941399, 173);
        */
        int altezzaGrattacielo = 247+166;
        this.addNewTarget("sanpaolo", 45.06971,7.662823,altezzaGrattacielo);
        this.addNewTarget("tobkcf",45.064171, 7.659963,250);
        //this.addNewTarget("tobkpraca",45.075859, 7.664095, 251);
        //this.addNewTarget("trueN", 89.99, 0.01, 250);
        /*
        Location asobrero = new Location("asobrero");
        asobrero.setLatitude(45.0808178);
        asobrero.setLongitude(7.6655203);

        Location csoregina = new Location("csoregina");
        csoregina.setLatitude(45.082234);
        csoregina.setLongitude(7.666427);
        */

        //Location
        //distanceTo will give you the distance in meters between the two given location ej target.distanceTo(destination).
        /*
            Location location1=new Location("locationA");
            near_locations.setLatitude(17.372102);
            near_locations.setLongitude(78.484196);
            Location location2=new Location("locationA");
            near_locations.setLatitude(17.375775);
            near_locations.setLongitude(78.469218);
            double distance=selected_location.distanceTo(near_locations);
        */
        /*
            There is only one user Location, so you can iterate List of nearby places
            can call the distanceTo() function to get the distance, you can store in an array if you like.

            From what I understand, distanceBetween() is for far away places, it's output is a WGS84 ellipsoid.
        */
        //public float bearingTo (Location dest)
        //distanceBetween

    }

    public void addNewTarget(String id, double lat, double lon, double alt){
        Location loc = new Location(id);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setAltitude(alt);
        targetLocations.put(id,loc);
    }

    public Map<String,Location> getTargetLocations() {

        return targetLocations;

    }



}
