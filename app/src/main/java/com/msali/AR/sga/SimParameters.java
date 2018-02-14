package com.msali.AR.sga;

import android.app.Activity;
import android.location.Location;

import com.android.libraries.TextureFromCameraActivity;
import com.android.sga.R;
//import com.android.sga.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mario Salierno on 14/03/2016.
 */
//http://www.coordinate-gps.it/
public class SimParameters {


    private String TAG = "SimParameters";
    private Map<String, Location> targetLocations = new HashMap<String, Location>();
    private Map<String, SimObject> targetObjects = new HashMap<String, SimObject>();

    public static final double LAT_ORIGIN = 45.063728;
    public static final double LON_ORIGIN = 7.661704;
    public static final double ALT_ORIGIN = 249;


    /*
    modelName including the extension p.e. chair.3ds
    textureId p.e R.drawable.chair
    float scale
    int dimension in metres
    loc = location
    */
    public class SimObject {

        public String modelName;
        public int textureId;
        public float scale;
        public Location loc;

        public SimObject(String modelName,
                         int textureId,///*p.e R.drawable.bigoffice*/
                         float scale,
                         Location loc) {

            this.modelName = modelName;
            this.textureId = textureId;
            this.scale = scale;
            this.loc = loc;

        }

    }

    public SimParameters() {


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

        int altezzaGrattacielo = 247 + 166;
        //this.addNewTargetCube("sanpaolo", 45.06971, 7.662823, altezzaGrattacielo);
        //this.addNewTargetCube("tobkcf", 45.064171, 7.659963, 250);



        Location sanpaolo = new Location(TAG);
        sanpaolo.setLatitude(45.06971);
        sanpaolo.setLongitude(7.662823);
        sanpaolo.setAltitude(altezzaGrattacielo);

        Location sobrero = new Location(TAG);
        sobrero.setLatitude(45.080377);
        sobrero.setLongitude(7.66552);
        sobrero.setAltitude(220);//247


        //mixto
        Location cla = new Location(TAG);
        //cla.setLatitude(45.063555441/*293595*/);//save
        //cla.setLongitude(7.662108913/*064003*/);//save
        //cla.setAltitude(248);//save

        //cla.setLatitude(45.063838195197064);//prev
        //cla.setLongitude(7.661729045212269);//prev
        //cla.setAltitude(242);//prev

        cla.setLatitude(45.063880347669254);//prev 45.063880347669254
        cla.setLongitude(7.661755867302418);
        cla.setAltitude(242);
        /*
        this.addNewTargetObject("chsobrero", "chair.3ds",
                R.drawable.chair,
                0.025f,//scale
                1,
                locObj);
        */
        this.addNewTargetObject("trex", "trex.obj",
                R.raw.trex,
                1.8f,//scale
                cla);

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

    }

    public void addNewTargetCube(String id, double lat, double lon, double alt) {
        Location loc = new Location(TAG);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setAltitude(alt);
        targetLocations.put(id, loc);
    }


    public void addNewTargetObject(String id, SimObject sObj) {

        this.targetObjects.put(id, sObj);
    }

    public void addNewTargetObject(String id, String modelName,
                                   int textureId,///*p.e R.drawable.bigoffice*/
                                   float scale,
                                   Location loc) {

        this.targetObjects.put(id, new SimObject(modelName, textureId, scale, loc));

    }

    public Map<String, Location> getTargetLocations() {

        return targetLocations;

    }

    public Map<String, SimObject> getTargetObjects() {

        return targetObjects;

    }

}
