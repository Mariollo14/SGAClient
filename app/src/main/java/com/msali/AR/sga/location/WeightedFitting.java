package com.msali.AR.sga.location;


import android.util.Pair;

/**
 * 008.
 * The class for weighted least squares fitting. Performs linear least squares
 * 009.
 * fitting to a collection of 2D points. Each point has an associated weight.
 * 010.
 * <br> <p/> Finds <b>a</b> and <b>b</b> such that function f(x)=a+bx minimizes
 * 011.
 * the sum SUM[w<sub>i</sub>*(y<sub>i</sub>-f(x<sub>i</sub>))]<sup>2</sup> where
 * 012.
 * <b>i</b> iterates over the input points
 * 013.
 * <p/>
 * 014.
 *
 * @author Kirill Grouchnikov
 *         015.
 */


//https://java.net/projects/ixent/sources/svn/content/trunk/src/org/jvnet/ixent/math/leastsquares/WeightedFitting.java?rev=33
//http://www.codegur.online/5684282/weighted-linear-regression-in-java
public class WeightedFitting {

    private double a;

    private double b;

    private boolean hasFit;

    private boolean isFitVertical;

    public static final double EPS = 1.0e-07;

    public static final double EPS_BIG = 1.0e-04;


    /**
     * 023.
     *
     * @param latlon  input points
     *
     * @param weights input point weights
     *
     */

    public WeightedFitting(Pair<Double,Double>[] latlon, double[] weights) {

        this.hasFit = false;


        if ((latlon == null) || (weights == null)) {

            return;

        }

        assert (latlon.length == weights.length) : "arrays are of different size";


        // compute sums

        double sw = 0.0;

        double swx = 0.0;

        double swy = 0.0;

        double swxy = 0.0;

        double swxx = 0.0;

        int count = latlon.length;

        for (int i = 0; i < count; i++) {

            double w = weights[i];

            double x = latlon[i].first;

            double y = latlon[i].second;

            sw += w;

            swx += (w * x);

            swy += (w * y);

            swxy += (w * x * y);

            swxx += (w * x * x);

        }

        double denom = sw * swxx - swx * swx;

        if (Math.abs(denom) < EPS) {

            this.isFitVertical = true;

        } else {

            this.a = (swy * swxx - swx * swxy) / denom;

            this.b = (sw * swxy - swx * swy) / denom;

            this.isFitVertical = false;

        }

        this.hasFit = true;

    }

    /**
     * 065.
     *
     * @return free coefficient of fit
     * 066.
     */

    public double getA() {

        return a;

    }

    /**
     * 072.
     *
     * @return linear coefficient of fit
     * 073.
     */

    public double getB() {

        return b;

    }

    /**
     * 079.
     *
     * @return true if the fit has been computed
     * 080.
     */

    public boolean isHasFit() {

        return hasFit;

    }

    /**
     * 086.
     * Return the inclination of the fit line in degrees
     * 087.
     * <p/>
     * 088.
     *
     * @return inclination of the fit line in degrees
     * 089.
     * @throws IllegalStateException is thrown if no fit is available
     *                               090.
     */
    public double getInclinationAngleInDegrees() {

        if (!this.hasFit) {

            throw new IllegalStateException("No fit for this input setLocation");

        }


        if (this.isFitVertical) {

            return 90.0;

        }

        double dx = 1.0;

        double dy = -this.b;

        double direction = Math.atan2(dy, dx);

        // This direction is in -pi..pi range. We need to convert


// it to 0..180 range

        direction *= (180.0 / Math.PI);

        while (direction < 0.0) {

            direction += 360.0;

        }

        while (direction > 180.0) {

            direction -= 180.0;

        }

        return direction;

    }

}
