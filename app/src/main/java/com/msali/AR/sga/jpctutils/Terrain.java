package com.msali.AR.sga.jpctutils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.android.sga.R;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.TextureInfo;
import com.threed.jpct.TextureManager;

public class Terrain {

    public static float HighestPoint = 0;


    static Bitmap bmp = null;
    static String pixel = null;




    public static Object3D generateDynamicGround(int X_SIZE, int Y_SIZE, Resources resources, TextureManager tm, String TextureID, Integer groundAltitude){

        int SCALE_FACTOR = 10;//(X_SIZE*Y_SIZE * 10) / (128*128);
        int SQUARE_SIDE_LEN = 1;

        bmp = BitmapFactory.decodeResource(resources, R.drawable.office); // the heightmap texture (128x128 due to memory limitations?)

        float[][] terrain = new float[X_SIZE][Y_SIZE];

        for (int x = 0; x < X_SIZE-1; x++) {
            for (int y = 0; y < Y_SIZE-1; y++) {

                pixel = Integer.toString(bmp.getPixel(x, y), 16);
                //Log.e("Terrain", "pix:"+pixel+" pxlen:"+pixel.length());
                //terrain[x][z] = Integer.parseInt(pixel.charAt(1) + "" + pixel.charAt(2), 16);
                char c1 = pixel.charAt(0);
                char c2 = pixel.charAt(1);
                terrain[x][y] = Integer.parseInt(c1 + "" + c2, 16);


                if(terrain[x][y] > HighestPoint){
                    HighestPoint = terrain[x][y];
                }
            }

        }
        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int y = 0; y < Y_SIZE - 1; y++) {
                terrain[x][y] = (terrain[x][y] + terrain[x + 1][y] + terrain[x][y + 1] + terrain[x + 1][y + 1]) / 4;
            }
        }
        Object3D ground = new Object3D(X_SIZE * Y_SIZE * 2);

        float xSizeF = (float) X_SIZE;
        float zSizeF = (float) Y_SIZE;

        int id = tm.getTextureID(TextureID); // I used a gray 128x128 texture

        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int y = 0; y < Y_SIZE - 1; y++) {

                TextureInfo ti = new TextureInfo(id, (x / xSizeF), (y / zSizeF), ((x + 1) / xSizeF), (y / zSizeF), (x / xSizeF), ((y + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * SCALE_FACTOR, y * SCALE_FACTOR, terrain[x][y] +groundAltitude ),
                        new SimpleVector((x + SQUARE_SIDE_LEN) * SCALE_FACTOR,  y * SCALE_FACTOR, terrain[x + 1][y]+groundAltitude),
                        new SimpleVector(x * SCALE_FACTOR, (y + SQUARE_SIDE_LEN) * SCALE_FACTOR,terrain[x][y + 1]+groundAltitude),
                        ti);

                ti = new TextureInfo(id, (x / xSizeF), ((y + 1) / zSizeF), ((x + 1) / xSizeF), (y / zSizeF), ((x + 1) / xSizeF), ((y + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * SCALE_FACTOR, (y + SQUARE_SIDE_LEN) * SCALE_FACTOR, terrain[x][y + 1]+groundAltitude),
                        new SimpleVector((x + SQUARE_SIDE_LEN) * SCALE_FACTOR, y * SCALE_FACTOR, terrain[x + 1][y]+groundAltitude),
                        new SimpleVector((x + SQUARE_SIDE_LEN) * SCALE_FACTOR, (y + SQUARE_SIDE_LEN) * SCALE_FACTOR, terrain[x + 1][y + 1]+groundAltitude),
                        ti);
            }
        }

        return ground;
    }



    public static Object3D generateGround(int X_SIZE, int Y_SIZE, Resources resources, TextureManager tm, String TextureID, Integer groundAltitude){

        bmp = BitmapFactory.decodeResource(resources, R.drawable.office); // the heightmap texture (128x128 due to memory limitations?)

        float[][] terrain = new float[X_SIZE][Y_SIZE];

        for (int x = 0; x < X_SIZE-1; x++) {
            for (int y = 0; y < Y_SIZE-1; y++) {

                pixel = Integer.toString(bmp.getPixel(x, y), 16);
                //Log.e("Terrain", "pix:"+pixel+" pxlen:"+pixel.length());
                //terrain[x][z] = Integer.parseInt(pixel.charAt(1) + "" + pixel.charAt(2), 16);
                char c1 = pixel.charAt(0);
                char c2 = pixel.charAt(1);
                terrain[x][y] = Integer.parseInt(c1 + "" + c2, 16);


                if(terrain[x][y] > HighestPoint){
                    HighestPoint = terrain[x][y];
                }
            }

        }
        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int y = 0; y < Y_SIZE - 1; y++) {
                terrain[x][y] = (terrain[x][y] + terrain[x + 1][y] + terrain[x][y + 1] + terrain[x + 1][y + 1]) / 4;
            }
        }
        Object3D ground = new Object3D(X_SIZE * Y_SIZE * 2);

        float xSizeF = (float) X_SIZE;
        float zSizeF = (float) Y_SIZE;

        int id = tm.getTextureID(TextureID); // I used a gray 128x128 texture

        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int y = 0; y < Y_SIZE - 1; y++) {

                TextureInfo ti = new TextureInfo(id, (x / xSizeF), (y / zSizeF), ((x + 1) / xSizeF), (y / zSizeF), (x / xSizeF), ((y + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * 10, y * 10, terrain[x][y] +groundAltitude ),
                                    new SimpleVector((x + 1) * 10,  y * 10, terrain[x + 1][y]+groundAltitude),
                                    new SimpleVector(x * 10, (y + 1) * 10,terrain[x][y + 1]+groundAltitude),
                                    ti);

                ti = new TextureInfo(id, (x / xSizeF), ((y + 1) / zSizeF), ((x + 1) / xSizeF), (y / zSizeF), ((x + 1) / xSizeF), ((y + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * 10, (y + 1) * 10, terrain[x][y + 1]+groundAltitude),
                                    new SimpleVector((x + 1) * 10, y * 10, terrain[x + 1][y]+groundAltitude),
                                    new SimpleVector((x + 1) * 10, (y + 1) * 10, terrain[x + 1][y + 1]+groundAltitude),
                                    ti);
            }
        }

        return ground;
    }

    public static Object3D generateWall(int X_SIZE, int Z_SIZE, Resources resources, TextureManager tm, String TextureID){

        bmp = BitmapFactory.decodeResource(resources, R.drawable.office); // the heightmap texture (128x128 due to memory limitations?)

        float[][] terrain = new float[X_SIZE][Z_SIZE];

        for (int x = 0; x < X_SIZE-1; x++) {
            for (int z = 0; z < Z_SIZE-1; z++) {

                pixel = Integer.toString(bmp.getPixel(x, z), 16);
                //Log.e("Terrain", "pix:"+pixel+" pxlen:"+pixel.length());
                //terrain[x][z] = Integer.parseInt(pixel.charAt(1) + "" + pixel.charAt(2), 16);
                char c1 = pixel.charAt(0);
                char c2 = pixel.charAt(1);
                terrain[x][z] = Integer.parseInt(c1 + "" + c2, 16);


                if(terrain[x][z] > HighestPoint){
                    HighestPoint = terrain[x][z];
                }
            }

        }
        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int z = 0; z < Z_SIZE - 1; z++) {
                terrain[x][z] = (terrain[x][z] + terrain[x + 1][z] + terrain[x][z + 1] + terrain[x + 1][z + 1]) / 4;
            }
        }
        Object3D ground = new Object3D(X_SIZE * Z_SIZE * 2);

        float xSizeF = (float) X_SIZE;
        float zSizeF = (float) Z_SIZE;

        int id = tm.getTextureID(TextureID); // I used a gray 128x128 texture

        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int z = 0; z < Z_SIZE - 1; z++) {

                TextureInfo ti = new TextureInfo(id, (x / xSizeF), (z / zSizeF), ((x + 1) / xSizeF), (z / zSizeF), (x / xSizeF), ((z + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * 10, terrain[x][z], z * 10),
                        new SimpleVector((x + 1) * 10, terrain[x + 1][z], z * 10),
                        new SimpleVector(x * 10, terrain[x][z + 1], (z + 1) * 10), ti);

                ti = new TextureInfo(id, (x / xSizeF), ((z + 1) / zSizeF), ((x + 1) / xSizeF), (z / zSizeF), ((x + 1) / xSizeF), ((z + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * 10, terrain[x][z + 1], (z + 1) * 10),
                        new SimpleVector((x + 1) * 10, terrain[x + 1][z], z * 10),
                        new SimpleVector((x + 1) * 10, terrain[x + 1][z + 1], (z + 1) * 10), ti);
            }
        }

        return ground;
    }


    /*                                                        //activity.getResources
    public static Object3D Generate(int X_SIZE, int Z_SIZE, Resources resources, TextureManager tm, String TextureID){

        //bmp = BitmapFactory.decodeResource(SpaceGrabber.resources, R.drawable.office); // the heightmap texture (128x128 due to memory limitations?)
        bmp = BitmapFactory.decodeResource(resources, R.drawable.office); // the heightmap texture (128x128 due to memory limitations?)

        float[][] terrain = new float[X_SIZE][Z_SIZE];

        for (int x = 0; x < X_SIZE-1; x++) {
            for (int z = 0; z < Z_SIZE-1; z++) {

                pixel = Integer.toString(bmp.getPixel(x, z), 16);
                Log.e("Terrain", "pix:"+pixel+" pxlen:"+pixel.length());
                terrain[x][z] = Integer.parseInt(pixel.charAt(1) + "" + pixel.charAt(2), 16);

                if(terrain[x][z] > HighestPoint){
                    HighestPoint = terrain[x][z];
                }
            }

        }
        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int z = 0; z < Z_SIZE - 1; z++) {
                terrain[x][z] = (terrain[x][z] + terrain[x + 1][z] + terrain[x][z + 1] + terrain[x + 1][z + 1]) / 4;
            }
        }
        Object3D ground = new Object3D(X_SIZE * Z_SIZE * 2);

        float xSizeF = (float) X_SIZE;
        float zSizeF = (float) Z_SIZE;

        int id = tm.getTextureID(TextureID); // I used a gray 128x128 texture

        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int z = 0; z < Z_SIZE - 1; z++) {

                TextureInfo ti = new TextureInfo(id, (x / xSizeF), (z / zSizeF), ((x + 1) / xSizeF), (z / zSizeF), (x / xSizeF), ((z + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * 10, terrain[x][z], z * 10), new SimpleVector((x + 1) * 10, terrain[x + 1][z], z * 10),
                        new SimpleVector(x * 10, terrain[x][z + 1], (z + 1) * 10), ti);

                ti = new TextureInfo(id, (x / xSizeF), ((z + 1) / zSizeF), ((x + 1) / xSizeF), (z / zSizeF), ((x + 1) / xSizeF), ((z + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * 10, terrain[x][z + 1], (z + 1) * 10), new SimpleVector((x + 1) * 10, terrain[x + 1][z], z * 10), new SimpleVector((x + 1) * 10,
                        terrain[x + 1][z + 1], (z + 1) * 10), ti);
            }
        }

        return ground;
    }
    */
}
