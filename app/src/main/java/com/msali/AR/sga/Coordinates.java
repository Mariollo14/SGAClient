package com.msali.AR.sga;

/**
 * Created by Mario Salierno on 05/03/2016.
 */
public class Coordinates {
    private double f0,f1,f2;

    public Coordinates(double f0, double f1, double f2){
        this.f0=f0;
        this.f1=f1;
        this.f2=f2;
    }

    public double getX() {
        return f0;
    }

    public double getY() {
        return f1;
    }

    public double getZ() {
        return f2;
    }
}
