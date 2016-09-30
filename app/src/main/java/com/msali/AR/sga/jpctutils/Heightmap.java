package com.msali.AR.sga.jpctutils;

import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.TextureInfo;
import com.threed.jpct.TextureManager;

/**
 * Created by Mario Salierno on 28/06/2016.
 */
public class Heightmap {




    public void  build(int X_SIZE, int Z_SIZE, TextureManager texMan) {

        float [][] terrain = new float[X_SIZE][Z_SIZE];
        /**
         * This is where the terrain-array is filled with some data, i.e. the
         * terrain is build. Keep in mind that this approach to build a terrain
         * is quite simple and slow. But it's easy to understand and the results
         * are not that bad, so....
         */
        for (int x = 0; x < X_SIZE; x++) {
            for (int z = 0; z < Z_SIZE; z++) {
                terrain[x][z] = -20 + (float) Math.random() * 40f; // Very
                // simple....
            }
        }

        /**
         * We are now smoothing the terrain heights. This looks better because
         * it removes sharp edges from the terrain.
         */
        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int z = 0; z < Z_SIZE - 1; z++) {
                terrain[x][z] = (terrain[x][z] + terrain[x + 1][z] + terrain[x][z + 1] + terrain[x + 1][z + 1]) / 4;
            }
        }

        /**
         * The terrain heights are calculated now. Now we have to build an
         * Object3D from them.
         */
        Object3D ground = new Object3D(X_SIZE * Z_SIZE * 2);

        /**
         * We have 50 tiles in x and z direction and 2 polygons per tile.
         */
        float xSizeF = (float) X_SIZE;
        float zSizeF = (float) Z_SIZE;

        int id = texMan.getTextureID("base");

        for (int x = 0; x < X_SIZE - 1; x++) {
            for (int z = 0; z < Z_SIZE - 1; z++) {
                /**
                 * We are now taking the heights calculated above and build the
                 * actual triangles using this data. The terrain's grid is fixed
                 * in x and z direction and so are the texture coordinates. The
                 * only part that varies is the height, represented by the data
                 * in terrain[][]. jPCT automatically takes care of vertex
                 * sharing and mesh optimizations (this is why building objects
                 * this way isn't blazing fast...but it pays out in the
                 * end...:-)) The Mesh is build with triangle strips in mind
                 * here. The format for these strips is equal to that of
                 * OpenGL's triangle strips.
                 */

                TextureInfo ti = new TextureInfo(id, (x / xSizeF), (z / zSizeF), ((x + 1) / xSizeF), (z / zSizeF), (x / xSizeF), ((z + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * 10, terrain[x][z], z * 10), new SimpleVector((x + 1) * 10, terrain[x + 1][z], z * 10),
                        new SimpleVector(x * 10, terrain[x][z + 1], (z + 1) * 10), ti);

                ti = new TextureInfo(id, (x / xSizeF), ((z + 1) / zSizeF), ((x + 1) / xSizeF), (z / zSizeF), ((x + 1) / xSizeF), ((z + 1) / zSizeF));
                ground.addTriangle(new SimpleVector(x * 10, terrain[x][z + 1], (z + 1) * 10), new SimpleVector((x + 1) * 10, terrain[x + 1][z], z * 10), new SimpleVector((x + 1) * 10,
                        terrain[x + 1][z + 1], (z + 1) * 10), ti);
            }
        }


    }

}
