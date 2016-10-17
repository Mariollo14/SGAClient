package com.msali.AR.sga.location.filters;

import android.location.Location;

import com.msali.AR.sga.location.LocationFilter;

import jama.Matrix;
import jkalman.JKalman;

/**
 * Created by Mario Salierno on 01/10/2016.
 */
public class JKalmanFilter implements LocationFilter{

    private int variables;
    private JKalman kalman;
    private Matrix state; // state [x, y, dx, dy, dxy]
    private Matrix corrected_state; // corrected state [x, y, dx, dy, dxy]
    private Matrix measurement; // measurement [x]


    boolean initialized = false;
    @Override
    public void filter(Location loc) {


        if(!initialized){
            try {
                initialize(loc);
                initialized=true;
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            push(loc.getLatitude(),loc.getLongitude());

            loc.setLatitude(corrected_state.get(0,0));
            loc.setLongitude(corrected_state.get(1,0));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    /*
     * Inicializa el filtro kalman con 2 variables
     */
    public void initialize(Location loc) throws Exception{
        double dx, dy;

        if(variables != 0){
            throw new RuntimeException();
        }
        variables = 2;
        kalman = new JKalman(4, 2);

        // constant velocity
        dx = 0.2;
        dy = 0.2;

        state = new Matrix(4, 1); // state [x, y, dx, dy, dxy]
        corrected_state = new Matrix(4, 1); // corrected state [x, y, dx, dy, dxy]

        measurement = new Matrix(2, 1); // measurement [x]
        measurement.set(0, 0, loc.getLatitude());
        measurement.set(1, 0, loc.getLongitude());

        // transitions for x, y, dx, dy
        double[][] tr = { {1, 0, dx, 0},
                {0, 1, 0, dy},
                {0, 0, 1, 0},
                {0, 0, 0, 1} };

        kalman.setTransition_matrix(new Matrix(tr));

        // 1s somewhere?
        kalman.setError_cov_post(kalman.getError_cov_post().identity());


        corrected_state = kalman.Correct(measurement);
        state = kalman.Predict();

    }




    /*
     * Inicializa el filtro kalman con 2 variables
     */
    public void initialize2() throws Exception{
        double dx, dy;

        if(variables != 0){
            throw new RuntimeException();
        }
        variables = 2;
        kalman = new JKalman(4, 2);

        // constant velocity
        dx = 0.2;
        dy = 0.2;

        state = new Matrix(4, 1); // state [x, y, dx, dy, dxy]
        corrected_state = new Matrix(4, 1); // corrected state [x, y, dx, dy, dxy]

        measurement = new Matrix(2, 1); // measurement [x]
        measurement.set(0, 0, 0);
        measurement.set(1, 0, 0);

        // transitions for x, y, dx, dy
        double[][] tr = { {1, 0, dx, 0},
                {0, 1, 0, dy},
                {0, 0, 1, 0},
                {0, 0, 0, 1} };
        kalman.setTransition_matrix(new Matrix(tr));

        // 1s somewhere?
        kalman.setError_cov_post(kalman.getError_cov_post().identity());




    }

    /*
     * Aplica Filtro a variables
     */
    public void push(double x,double y) throws Exception{
        measurement.set(0, 0, x);
        measurement.set(1, 0, y);

        corrected_state = kalman.Correct(measurement);
        state = kalman.Predict();
    }

    /*
     * obtiene arreglo con datos filtrados.
     */
    public double[] getKalmanPoint2() throws Exception{
        double[] point = new double[2];
        point[0] = corrected_state.get(0,0);
        point[1] = corrected_state.get(1,0);
        return point;
    }

    /*
     * obtiene arreglo con prediccion de punto.
     */
    public double[] getPredict2() throws Exception{
        double[] point = new double[2];
        point[0] = state.get(0,0);
        point[1] = state.get(1,0);
        return point;
    }

    /*
     * obtiene cantidad de variables del objeto
     */
    public int getNVariables() throws Exception{
        return this.variables;
    }


}
