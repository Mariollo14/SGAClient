package com.android.libraries;

import android.location.Location;
import android.util.Log;

/**
 * Created by Mario Salierno on 14/03/2016.
 */
public class SimParameters {

    private Location targetLocation;

    public SimParameters(){

        //test
        Location asobrero = new Location("asobrero");
        asobrero.setLatitude(45.0808178);
        asobrero.setLongitude(7.6655203);

        Location csoregina = new Location("csoregina");
        csoregina.setLatitude(45.082234);
        csoregina.setLongitude(7.666427);


        //porta susa
        //targetLocation = new Location("target Point");

        //targetLocation.setLatitude(45.08008);
        //targetLocation.setLongitude(7.67046);

        targetLocation=csoregina;

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

    public Location getTargetLocation() {

        return targetLocation;

    }

    public void testDistance(Location yourLoc){

        String dmia,dmiahav, dTO;

        dmia = "dmia: " + LocationUtilities.getEquirectangularApproximationDistance(yourLoc.getLatitude(),yourLoc.getLongitude(),
                                                                              targetLocation.getLatitude(),targetLocation.getLongitude());


        dmiahav = "dmiahav: " + LocationUtilities.getHaversineDistance(yourLoc.getLatitude(),yourLoc.getLongitude(),
                targetLocation.getLatitude(),targetLocation.getLongitude());


        dTO = "dTO: " +yourLoc.distanceTo(targetLocation);

        Log.d("DISTANCE!",dmia + "\n "+ dTO+"\n "+ dmiahav);
    }


}
