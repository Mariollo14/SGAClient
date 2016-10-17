package com.msali.AR.sga.location;

//http://stackoverflow.com/questions/1134579/smooth-gps-data

import android.location.Location;

//use the code to process the coordinates that you get from onLocationChanged()
public class KalmanLatLongFilter implements LocationFilter{


    private String TAG = "KalmanLatLongFilter";
    private final float MinAccuracy = 1;

    /*
    The code has a single free parameter Q, expressed in metres per second,
    which describes how quickly the accuracy decays in the absence of any new location estimates.
    A higher Q parameter means that the accuracy decays faster.
    Kalman filters generally work better when the accuracy decays a bit quicker than one might expect,
    so for walking around with an Android phone I find that Q=3 metres per second works fine,
    even though I generally walk slower than that.
    But if travelling in a fast car a much larger number should obviously be used.
    */
    private float Q_metres_per_second = 3;
    private long TimeStamp_milliseconds;
    private double lat;
    private double lng;
    private float variance;//=-1; // P matrix.  Negative means object uninitialised.  NB: units irrelevant, as long as same units used throughout
    boolean firstLocationState = true;


    public KalmanLatLongFilter(){ variance = -1; }

    public KalmanLatLongFilter(float Q_metres_per_second) { this.Q_metres_per_second = Q_metres_per_second; variance = -1; }

    public long get_TimeStamp() { return TimeStamp_milliseconds; }
    public double get_lat() { return lat; }
    public double get_lng() { return lng; }
    public float get_accuracy() { return (float)Math.sqrt(variance); }

    public void SetState(double lat, double lng, float accuracy, long TimeStamp_milliseconds) {
        //Log.e(TAG,"SetState");
        this.lat=lat; this.lng=lng; variance = accuracy * accuracy; this.TimeStamp_milliseconds=TimeStamp_milliseconds;
    }


    /// <summary>
    /// Kalman filter processing for lattitude and longitude
    /// </summary>
    /// <param name="lat_measurement_degrees">new measurement of lattidude</param>
    /// <param name="lng_measurement">new measurement of longitude</param>
    /// <param name="accuracy">measurement of 1 standard deviation error in metres</param>
    /// <param name="TimeStamp_milliseconds">time of measurement</param>
    /// <returns>new state</returns>
    private void Process(double lat_measurement, double lng_measurement, float accuracy, long TimeStamp_milliseconds) {

        if (accuracy < MinAccuracy){
            //Log.e(TAG,"accuracy < MinAccuracy  so: "+"accuracy = MinAccuracy");
            accuracy = MinAccuracy;
        }
        //Log.e(TAG,"Process");
        if (variance < 0) {
            // if variance < 0, object is unitialised, so initialise with current values
            //Log.e(TAG,"variance<0:"+variance);
            this.TimeStamp_milliseconds = TimeStamp_milliseconds;
            lat=lat_measurement;
            lng = lng_measurement;
            variance = accuracy*accuracy;
            //Log.e(TAG,"variance:"+variance);
        } else {
            // else apply Kalman filter methodology
            //Log.e(TAG,"variance>0:"+variance);
            long TimeInc_milliseconds = TimeStamp_milliseconds - this.TimeStamp_milliseconds;
            if (TimeInc_milliseconds > 0) {
                // time has moved on, so the uncertainty in the current position increases
                variance += TimeInc_milliseconds * Q_metres_per_second * Q_metres_per_second / 1000;
                this.TimeStamp_milliseconds = TimeStamp_milliseconds;
                // TO DO: USE VELOCITY INFORMATION HERE TO GET A BETTER ESTIMATE OF CURRENT POSITION
            }


            // Kalman gain matrix K = Covarariance * Inverse(Covariance + MeasurementVariance)
            // NB: because K is dimensionless, it doesn't matter that variance has different units to lat and lng
            float K = variance / (variance + accuracy * accuracy);
            // apply K
            //lat += K * (lat_measurement - lat);
            //lng += K * (lng_measurement - lng);
            double latD = K * (lat_measurement - lat);
            double lngD = K * (lng_measurement - lng);

            lat = lat + latD;
            lng = lng + lngD;

            //Log.e(TAG,"K:"+K);
            //Log.e(TAG,"latm:"+lat_measurement+" lgm:"+lng_measurement);
            //Log.e(TAG,"latD:"+latD+" lg:"+lngD);
            //Log.e(TAG,"lat:"+lat+" lg:"+lng);
            // new Covarariance  matrix is (IdentityMatrix - K) * Covarariance
            variance = (1 - K) * variance;
        }
    }

    @Override
    public void filter(Location loc) {

        if(loc==null)return;

        if(firstLocationState){
            SetState(loc.getLatitude(),loc.getLongitude(),loc.getAccuracy(),System.currentTimeMillis());
            firstLocationState=false;
            return;
        }

        Process(loc.getLatitude(),loc.getLongitude(),loc.getAccuracy(),loc.getTime()/*System.currentTimeMillis()*/);

        //Log.e(TAG, "pre  k: lt:"+loc.getLatitude()+ " lg:"+loc.getLongitude());



        loc.setLatitude(this.get_lat());
        loc.setLongitude(this.get_lng());
        loc.setAccuracy(this.get_accuracy());

        //Log.e(TAG, "post k: lt:"+loc.getLatitude()+ " lg:"+loc.getLongitude());
        //Log.e(TAG,"Kalman Filter applied");

    }
}
