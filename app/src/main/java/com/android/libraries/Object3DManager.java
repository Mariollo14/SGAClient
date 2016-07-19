package com.android.libraries;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by Mario Salierno on 15/07/2016.
 */
public class Object3DManager {

    private Activity activity;
    private String TAG = "Object3DManager";


    public Object3DManager(Activity act){

        this.activity=act;

    }


    public Object3D createObject3D(){
        TexturedObject tObj = new TexturedObject("chair",//id
                R.drawable.chair,//p.e R.drawable.bigoffice//textureID
                0.025f,//scale
                -4,//x
                0,//y
                -2,//z
                1);//dim

        tObj.obj3D.rotateX((float)Math.toRadians(90.0));

        //tObj.obj3D.rotateY((float)Math.toRadians(90.0));
        //tObj.obj3D.rotateZ((float)Math.toRadians(90.0));
        manageObjectCreationFromModel(tObj);

        return tObj.obj3D;

    }

    public static byte[] serializeObject3D(Object3D obj3d) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        serializeObject3D(out, obj3d);

        byte[] serObj = out.toByteArray();

        // close the stream
        out.close();

        return serObj;
    }


    public static Object3D deserializeObject3D(byte[] serObj) throws IOException, ClassNotFoundException {

        ByteArrayInputStream in = new ByteArrayInputStream(serObj);

        return deserializeObject3D(in);

    }




    public static void serializeObject3D(OutputStream out, Object3D obj3d) throws IOException {


        ObjectOutputStream oout = new ObjectOutputStream(out);

        // write something in the file
        oout.writeObject(obj3d);

        // close the stream
        oout.close();

    }


    public static Object3D deserializeObject3D(InputStream in) throws IOException, ClassNotFoundException {


        ObjectInputStream input = new ObjectInputStream(in);

        Object3D obj3d = (Object3D) input.readObject();

        return obj3d;

    }

    //http://stackoverflow.com/questions/15298130/load-3d-models-with-jpct-ae
    public class TexturedObject{

        public String id;
        public int textureId;
        public float x,y,z;
        public int dimension;
        public float scale;
        public Texture texture;
        public Object3D obj3D;

        //public String texturePath;

        public TexturedObject(String id,
                              int textureId,///*p.e R.drawable.bigoffice*/
                              //String texturePath,
                              float scale,
                              float x,
                              float y,
                              float z,
                              int dim){
            this.id=id;
            this.textureId = textureId;
            //this.texturePath=texturePath; //path = assets/
            this.scale=scale;
            this.x=x;
            this.y=y;
            this.z=z;
            dimension=dim;
            obj3D = load3DSObject(id, textureId, scale);
        }



        //http://www.jpct.net/forum2/index.php/topic,2168.15.html?PHPSESSID=2963dbbdcd6472ebe013778ea71482ec
        ////txtrName for example for a named bman.3ds, it would be just bman
        private Object3D load3DSObject(String txtrName, int textureID/*p.e R.drawable.bigoffice*/, float thingScale /*= 1*/)
        {

            //TextureManager.getInstance().addTexture(txtrName + ".jpg", new Texture("res/" + txtrName + ".jpg"));
            // Create a texture out of the icon...:-)
            //texture = new Texture(BitmapHelper.rescale(BitmapHelper.convert(activity.getResources().getDrawable(R.drawable.ic_launcher)), 64, 64));
            //Log.e("TEXTUREchairID","chair:"+textureID);
            //Drawable image = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.bigoffice   /*textureID*/ , null);

            //texture = new Texture(image);

            //Bitmap bmp = BitmapFactory.decodeFile( "/drawable/" );
            Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), textureID);
            //texture = new Texture(activity.getResources().openRawResource(textureID));
            texture = new Texture(bm);
            //texture = new Texture(200,200);
            //texture = new Texture(BitmapHelper.convert(image));
            TextureManager.getInstance().addTexture(txtrName, texture);


            try {
                String fname = /*"assets/" + */txtrName+".3ds";
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
    }

    public void manageObjectCreationFromModel(TexturedObject tObj){
        //Log.e("object:"+tObj.id, "CREATED at:"+tObj.x+" y:"+tObj.y+" z:"+tObj.z);
        TextureManager txtManager = TextureManager.getInstance();
        Texture txt;
        if(!txtManager.containsTexture(tObj.id)) {

            txt = tObj.texture;


        }
        else
            txt = txtManager.getTexture(tObj.id);


        //txtManager.addTexture(tObj.id, txt);

        //Object3D cube = Primitives.getCube(tObj.dimension);
        Object3D obj3D = tObj.obj3D;
        obj3D.translate(tObj.x, tObj.y, tObj.z);
        obj3D.setTexture(tObj.id);
        obj3D.setName(tObj.id);
        //world.addObject(obj3D);

    }


}
