package com.msali.AR.sga.jpctutils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.threed.jpct.Loader;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

/**
 * Created by Mario Salierno on 21/07/2016.
 */
//http://stackoverflow.com/questions/15298130/load-3d-models-with-jpct-ae
public class ModelObject3D {

    private String TAG = "ModelObject3D";
    private Activity activity;
    public String modelFileName;
    public String basename = "";
    public String model_format = "";
    public int textureId;
    public float x,y,z;
    public int dimension;
    public float scale;
    public Texture texture;
    public Object3D obj3D;

    //public String texturePath;


    /*
    Just loads the Object 3D no testuring involved since the object has to be sent
     */
    public ModelObject3D(Activity activity,
                         String modelFile,
                         //int textureId,///*p.e R.drawable.bigoffice*/
                         //String texturePath,
                         float scale,
                         float x,
                         float y,
                         float z,
                         int dim){
        this.modelFileName=modelFileName;
        //this.textureId = textureId;
        //this.texturePath=texturePath; //path = assets/
        this.activity=activity;
        this.scale=scale;
        this.x=x;
        this.y=y;
        this.z=z;
        dimension=dim;
        obj3D = loadObject(modelFile, scale);
    }


    /*
   Loads an object, loads the texture and uses it to texture the loaded Object3D.
    */
    public ModelObject3D(Activity activity,
                         String modelFile,
                         int textureId,///*p.e R.drawable.bigoffice*/
                         //String texturePath,
                         float scale,
                         float x,
                         float y,
                         float z,
                         int dim){
        this.modelFileName=modelFileName;
        this.textureId = textureId;
        //this.texturePath=texturePath; //path = assets/
        this.activity=activity;
        this.scale=scale;
        this.x=x;
        this.y=y;
        this.z=z;
        dimension=dim;
        obj3D = loadObjectAndTexture(modelFile, textureId, scale);
    }



    /*
    Just loads the Object 3D no texturing involved since the object has to be sent
     */
    //texture and model file has to match the same basename p.e. chair.3ds and chair.jpg
    private Object3D loadObject(String modName, float thingScale /*= 1*/)
    {

        StringTokenizer sToken = new StringTokenizer(modName,".");
        int ntok = sToken.countTokens();
        for(int i = 0; i<ntok; i++){
            if(i==ntok-1) {
                model_format = sToken.nextToken();
                break;
            }
            else
                basename=basename+sToken.nextToken();
        }
        Log.e(TAG, "basename:"+basename);
        Log.e(TAG, "mod_form:"+model_format);


        //Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), textureID);
        //texture = new Texture(bm);
        //TextureManager.getInstance().addTexture(basename, texture);

        try {

            Log.e(TAG, "model file name:"+modName);
            Object3D objT = loadModel(basename, model_format, thingScale);

            Log.e(TAG, "objT==null :"+Boolean.toString(objT==null));
            objT.build();
            //objT.setTexture(basename);
            objT.setName(basename);
            objT.translate(x, y, z);

            return objT;
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }



    /*
    Loads an object, loads the texture and uses it to texture the loaded Object3D.
     */
    //texture and model file has to match the same basename p.e. chair.3ds and chair.jpg
    private Object3D loadObjectAndTexture(String modName, int textureID/*p.e R.drawable.bigoffice*/, float thingScale /*= 1*/)
    {

        StringTokenizer sToken = new StringTokenizer(modName,".");
        int ntok = sToken.countTokens();
        for(int i = 0; i<ntok; i++){
            if(i==ntok-1) {
                model_format = sToken.nextToken();
                break;
            }
            else
                basename=basename+sToken.nextToken();
        }
        Log.e(TAG, "basename:"+basename);
        Log.e(TAG, "mod_form:"+model_format);


        Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), textureID);
        texture = new Texture(bm);
        TextureManager.getInstance().addTexture(basename, texture);

        try {

            Log.e(TAG, "model file name:"+modName);
            Object3D objT = loadModel(basename, model_format, thingScale);

            Log.e(TAG, "objT==null :"+Boolean.toString(objT==null));
            objT.build();
            objT.setTexture(basename);
            objT.setName(basename);
            objT.translate(x, y, z);

            return objT;
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    //http://stackoverflow.com/questions/15298130/load-3d-models-with-jpct-ae
    private Object3D loadModel(String filename, String format, float scale) throws UnsupportedEncodingException {

        //InputStream stream = new ByteArrayInputStream(filename.getBytes("UTF-8"));
        //InputStream stream = mContext.getAssets().open("FILENAME.3DS")
        InputStream stream = null;
        try {
            stream = activity.getApplicationContext().getAssets().open(filename+"."+format);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Object3D[] model=null;
        Log.e(TAG,"switch format:"+format);
        switch (format){
            case "3ds":
                model = Loader.load3DS(stream, scale);
                break;
            case "3DS":
                model = Loader.load3DS(stream, scale);
                break;
            case "obj":
                Loader.loadOBJ(stream,null,scale);
                break;
            case "OBJ":
                Loader.loadOBJ(stream,null,scale);
                break;
            case "md2":
                Loader.loadMD2(stream,scale);
                break;
            case "MD2":
                Loader.loadMD2(stream,scale);
                break;
            case "asc":
                Loader.loadASC(stream,scale,false);
                break;
            case "ASC":
                Loader.loadASC(stream,scale,false);
                break;
            default:
                    /*
                    Loads an object in serialized format.
                    This format can't be created by jPCT-AE but by using the desktop version of jPCT by using the DeSerializer.
                    This is the fastest and least memory intense way to load an object in jPCT-AE.
                    It doesn't matter the extension. You can name the file as you like, because you are loading them via an input stream anyway.
                    Just keep in mind that Android has some limits for the size of various file types.
                    If it complains about a file being too large, just name it .mp3... Yes, seriously...
                     */
                Loader.loadSerializedObject(stream);
                break;
                /*
                default:
                    return null;
                */
        }

        if(model==null){
            Log.e(TAG,"model==null");
            return null;
        }

        Object3D o3d = new Object3D(0);
        Object3D temp = null;
        for (int i = 0; i < model.length; i++) {
            temp = model[i];
            temp.setCenter(SimpleVector.ORIGIN);
            temp.rotateX((float)( -.5*Math.PI));
            temp.rotateMesh();
            temp.setRotationMatrix(new Matrix());
            o3d = Object3D.mergeObjects(o3d, temp);
            o3d.build();
        }
        return o3d;
    }

        /*
        //http://www.jpct.net/forum2/index.php/topic,2168.15.html?PHPSESSID=2963dbbdcd6472ebe013778ea71482ec
        ////txtrName for example for a named bman.3ds, it would be just bman
        private Object3D load3DSObject(String txtrName, int textureID, float thingScale)
        {

            //TextureManager.getInstance().addTexture(txtrName + ".jpg", new Texture("res/" + txtrName + ".jpg"));
            // Create a texture out of the icon...:-)
            //texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(activity.getResources().getDrawable(R.drawable.ic_launcher)), 64, 64));
            Log.e("TEXTUREchairID","chair:"+textureID);
            //Drawable image = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.bigoffice , null);

            //texture = new Texture(image);

            //Bitmap bmp = BitmapFactory.decodeFile( "/drawable/" );
            Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), textureID);
            //texture = new Texture(activity.getResources().openRawResource(textureID));
            texture = new Texture(bm);
            //texture = new Texture(200,200);
            //texture = new Texture(BitmapHelper.convert(image));
            TextureManager.getInstance().addTexture(txtrName, texture);


            try {
                String fname = txtrName+".3ds";
                //Log.e(TAG, "model file name:"+fname);
                Object3D objT = loadModel(fname, thingScale);
                //Primitives.getCube(10);
                //cube.calcTextureWrapSpherical();
                //cube.setTexture("texture");
                //cube.strip();
                objT.build();

                return objT;
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            return null;
        }


        //http://stackoverflow.com/questions/15298130/load-3d-models-with-jpct-ae
        private Object3D loadModel(String filename, float scale) throws UnsupportedEncodingException {

            //InputStream stream = new ByteArrayInputStream(filename.getBytes("UTF-8"));
            //InputStream stream = mContext.getAssets().open("FILENAME.3DS")
            InputStream stream = null;
            try {
                stream = activity.getApplicationContext().getAssets().open(filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Object3D[] model;
            if(filename.endsWith(".3ds")){
                model = Loader.load3DS(stream, scale);
            }
            else
                return null;

            Object3D o3d = new Object3D(0);
            Object3D temp = null;
            for (int i = 0; i < model.length; i++) {
                temp = model[i];
                temp.setCenter(SimpleVector.ORIGIN);
                temp.rotateX((float)( -.5*Math.PI));
                temp.rotateMesh();
                temp.setRotationMatrix(new Matrix());
                o3d = Object3D.mergeObjects(o3d, temp);
                o3d.build();
            }
            return o3d;
        }
        */
}
