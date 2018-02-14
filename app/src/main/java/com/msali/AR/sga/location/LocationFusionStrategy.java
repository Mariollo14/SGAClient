package com.msali.AR.sga.location;

import android.location.Location;
import android.os.AsyncTask;

import com.android.libraries.TextureFromCameraActivity;
import com.msali.AR.sga.location.filters.JKalmanFilter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mario Salierno on 06/07/2016.
 */
public class LocationFusionStrategy {



    public LocationFusionStrategy(TextureFromCameraActivity activity){

        this.activity=activity;

        //to be fixed filters.add(new JKalmanFilter());
        //filters.add(new KalmanLatLongFilter());
        //filters.add(new WeightedMovingAverageFilter());

    }

    /*
    public LocationFusionStrategy(ArrayList<GeoLocator> locators){

        this.locators=locators;
        if(altitudeSmoothServiceEnabled) {
            Location loc = getBestLocationAmongLocators();
            if(loc!=null)
                getAltitudeThroughService(loc.getLongitude(), loc.getLatitude());
        }

    }
    */

    private String TAG = "LocationFusionStrategy";
    private TextureFromCameraActivity activity;


    private ArrayList<GeoLocator> locators=new ArrayList<GeoLocator>();
    private List<LocationFilter> filters = new LinkedList<LocationFilter>();
    private int RELEVANCE_INTERVAL = 10 * 1000; //10 seconds
    private int ACCURACY_SIGNIFICANT_DELTA = 50;
    private boolean altitudeSmoothServiceEnabled = true;
    private boolean weightedLeastSquareEnabled = true;
    //private boolean kalmanFilterEnabled = true;

    //private int KALMAN_Q_METRES_PER_SECOND = 3;
    //private KalmanLatLongFilter kalmanFilter = new KalmanLatLongFilter(KALMAN_Q_METRES_PER_SECOND);

    private static Double latestAltitude = null;
    private Long lastTime = System.currentTimeMillis();
    private int ALTITUDE_SERVICE_UPDATE_INTERVAL = 60000; //60 sec

    //push a new filter
    public void pushNewFilter(LocationFilter l){
        if(l!=null)
            filters.add(l);

    }

    public void setAltitudeSmoothServiceEnabled(boolean enabled){
        altitudeSmoothServiceEnabled=enabled;
    }


    public void addGeoLocator(GeoLocator locator, boolean startLocator){

        if(startLocator)locator.startUpdates();

        locators.add(locator);
    }

    public void resumeLocators(){

        for(GeoLocator loc: locators){
            loc.startUpdates();
        }

    }

    public void pauseLocators(){


        for(GeoLocator loc: locators){
            loc.stopUpdates();
        }

    }


    public Location getBestLocationAmongLocators(){

        Location best = null;
        Location lastKnown = null;

        for(GeoLocator loc: locators){


            if(loc instanceof  GPSLocator)lastKnown=((GPSLocator) loc).getLastKnownLocation();

            Location l = loc.requestAsynchLocationUpdate();
            Location temp=null;
            if(l!=null) {
                temp = new Location(l);
                loc.onLocationChanged(temp);
            }

            if(isBetterLocation(temp,best))
                best=temp;

        }

        if(best == null)best = lastKnown;

        smoothAltitudeValueThroughService(best);

        applyFilters(best);
        //smoothLatLongWithKalmanFilter(best);


        String loc = "lat:" + best.getLatitude() + " long:" + best.getLongitude() + " alt:" + best.getAltitude();
        TextureFromCameraActivity.MainHandler mainH = activity.getMainHandler();
        if (mainH != null)
            mainH.sendLocalization(loc);

        return best;

    }

    public Location getAsynchBestLocationAmongLocators(){

        Location best = null;
        Location lastKnown = null;
        for(GeoLocator loc: locators){

            if(loc instanceof  GPSLocator)lastKnown=((GPSLocator) loc).getLastKnownLocation();

            Location l = loc.requestAsynchLocationUpdate();
            Location temp=null;
            if(l!=null) {
                temp = new Location(l);
                loc.onLocationChanged(temp);
            }

            if(isBetterLocation(temp,best))
                best=temp;

        }

        if(best == null)best = lastKnown;

        //if(best!=null)
            //lastLocations.add(best);

        //smoothLatLongWithWeightedLinearRegression();

        smoothAltitudeValueThroughService(best);

        applyFilters(best);
        //smoothLatLongWithKalmanFilter(best);


        String loc = "lat:" + best.getLatitude() + " long:" + best.getLongitude() + " alt:" + best.getAltitude();
        TextureFromCameraActivity.MainHandler mainH = activity.getMainHandler();
        if (mainH != null)
            mainH.sendLocalization(loc);

        return best;

    }



    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        if(location==null){
            return false;
        }
        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > RELEVANCE_INTERVAL;
        boolean isSignificantlyOlder = timeDelta < -RELEVANCE_INTERVAL;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > ACCURACY_SIGNIFICANT_DELTA;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    /*
    private ArrayList<Location> lastLocations = new ArrayList<Location>();
    private void smoothLatLongWithWeightedLinearRegression(){

        if(!weightedLeastSquareEnabled)return;

        double[][] x = {
                { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                },
                { 0,  0.1 , 0.2 , 0.3 , 0.4, 0.5 , 0.6 , 0.7 , 0.8 , 0.9,
                        1,  1.1 , 1.2 , 1.3 , 1.4, 1.5 , 1.6 , 1.7 , 1.8 , 1.9,
                },
        };

        //double[] ylat = new double[lastLocations.size()];
        //double[] ylon = new double[lastLocations.size()];
        Pair<Double,Double>[] latlon = new Pair[lastLocations.size()];
        double[] weights = new double[lastLocations.size()];


        for(int i = 0; i<lastLocations.size(); i++){
            Location temp = lastLocations.get(i);
            //ylat[i] = temp.getLatitude();
            //ylon[i] = temp.getLongitude();
            latlon[i]= new Pair(temp.getLatitude(),temp.getAltitude());
            weights[i]=1/temp.getAccuracy();
        }

        WeightedFitting wf = new WeightedFitting(latlon,weights);
        Log.e(TAG,"WEIGHTED a:"+wf.getA());
        Log.e(TAG,"WEIGHTED b:"+wf.getB());


        //LinearRegression lr = new LinearRegression();
        //lr.regress(ylat, x, weight);
        //double[] coef = lr.getCoefficients();
        //System.out.println("Coeffecient 0: " + coef[0]);
        //System.out.println("Coeffecient 1: " + coef[1]);
        //System.out.println("Y = " + coef[0] + " + " + coef[1] + " x");
        //System.out.println("-------------------------");


    }
    */



    private void applyFilters(Location loc){

        for(LocationFilter lfilt : filters){
            lfilt.filter(loc);
        }

    }


    /*
    private void smoothLatLongWithKalmanFilter(Location loc){
        if(!kalmanFilterEnabled)return;

        kalmanFilter.filter(loc);
    }
    */


    private void smoothAltitudeValueThroughService(final Location loc){

        /*
        if(true) {
            loc.setAltitude(248);
            return;
        }
        */
        if(!altitudeSmoothServiceEnabled)return;

        if(loc==null)return;


        if(latestAltitude==null)return;

        //double newAltitude = (loc.getAltitude() + latestAltitude)/2;

        loc.setAltitude(latestAltitude);


        if(lastTime!=null) {
            long currTime = System.currentTimeMillis();
            long delta = currTime-lastTime;
            //Log.e(TAG,"delta="+delta);

            if(delta<ALTITUDE_SERVICE_UPDATE_INTERVAL)return;

            lastTime=currTime;
        }

        // The Very Basic
        new AsyncTask<Void, Void, Void>() {
            protected void onPreExecute() {
                // Pre Code
            }
            protected Void doInBackground(Void... unused) {
                // Background Code
                //getAltitudeThroughService(loc.getLongitude(),loc.getLatitude());
                getAltitudeFromGoogleMaps(loc.getLongitude(),loc.getLatitude());

                return null;
            }
            protected void onPostExecute(Void unused) {
                // Post Code
            }
        }.execute();



    }

    //http://stackoverflow.com/questions/32607257/cannot-resolve-symbol-httpget-httpclient-httpresponce-in-android-studio
    //ATMOSPHERE PRESSURE   http://stackoverflow.com/questions/8749719/how-can-i-capture-altitude-on-an-android-phone
    //http://stackoverflow.com/questions/11168306/is-androids-gps-altitude-incorrect-due-to-not-including-geoid-height
    //http://stackoverflow.com/questions/29749930/kalman-filter-to-smooth-accelerometer-signals-using-rotation-matrix
    public static void getAltitudeThroughService(Double longitude, Double latitude) {
        double result = Double.NaN;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        String url = "http://gisdata.usgs.gov/"
                + "xmlwebservices2/elevation_service.asmx/"
                + "getElevation?X_Value=" + String.valueOf(longitude)
                + "&Y_Value=" + String.valueOf(latitude)
                + "&Elevation_Units=METERS&Source_Layer=-1&Elevation_Only=true";
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet, localContext);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                int r = -1;
                StringBuffer respStr = new StringBuffer();
                while ((r = instream.read()) != -1)
                    respStr.append((char) r);
                String tagOpen = "<double>";
                String tagClose = "</double>";
                if (respStr.indexOf(tagOpen) != -1) {
                    int start = respStr.indexOf(tagOpen) + tagOpen.length();
                    int end = respStr.indexOf(tagClose);
                    String value = respStr.substring(start, end);
                    result = Double.parseDouble(value);
                }
                instream.close();
                if(result!=Double.NaN)return;
                else{
                    latestAltitude = result;
                    //Log.e("LocationFusion", "setting service altitude:"+latestAltitude);
                }
            }
        } catch (ClientProtocolException e) {}
        catch (IOException e) {}
        //return result;
    }


    public static void getAltitudeFromGoogleMaps(double longitude, double latitude) {
        double result = Double.NaN;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        String url = "http://maps.googleapis.com/maps/api/elevation/"
                + "xml?locations=" + String.valueOf(latitude)
                + "," + String.valueOf(longitude)
                + "&sensor=true";
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet, localContext);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                int r = -1;
                StringBuffer respStr = new StringBuffer();
                while ((r = instream.read()) != -1)
                    respStr.append((char) r);
                String tagOpen = "<elevation>";
                String tagClose = "</elevation>";
                if (respStr.indexOf(tagOpen) != -1) {
                    int start = respStr.indexOf(tagOpen) + tagOpen.length();
                    int end = respStr.indexOf(tagClose);
                    String value = respStr.substring(start, end);
                    result = (double)(Double.parseDouble(value));
                }
                instream.close();
                if(result!=Double.NaN){
                    latestAltitude=result;
                    //Log.e("LocationFusion", "setting service altitude:"+latestAltitude);

                    return;
                }
                else
                    return;
            }
        } catch (ClientProtocolException e) {}
        catch (IOException e) {}


    }

}
