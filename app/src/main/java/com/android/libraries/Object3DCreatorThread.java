package com.android.libraries;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Log;

import com.threed.jpct.Object3D;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Mario Salierno on 21/07/2016.
 */
public class Object3DCreatorThread extends Thread {

    private String TAG = "Object3DCreatorThread";

    private JSONObject job;
    private JPCTWorldManager jpctWorldManager;
    private Location loc;


    public Object3DCreatorThread(JSONObject job, JPCTWorldManager jpctWorldManager, Location loc) {

        this.job=job;
        this.jpctWorldManager = jpctWorldManager;
        this.loc = loc;

    }


    @Override
    public void run() {

        try {
            Log.e(TAG, "run()");
            //Log.e(TAG, "message len:" + message.length());
            //Log.e(TAG, message.substring(0, 20));
            //Log.e(TAG, message.substring(20,40));
            //Log.e(TAG, message.substring(40,60));
            //Log.e(TAG, message.substring(1398155,1398170));
            //job = new JSONObject(message);

            //Boolean successful = job.getBoolean("successful");
            Boolean successful = (Boolean) job.get("successful");
            Log.e(TAG, "successful not blocking");
            Log.e(TAG, "successful:" + successful.toString());
            if (successful) {
                //String basename = job.getString("basename");
                //Log.e("basename", basename);
                //String extension= job.getString("extension");
                //String txtFile = job.getString("file");

                try {
                    //Location loc = locationFusion.getAsynchBestLocationAmongLocators();
                    //loc.setLongitude(loc.getLongitude()+0.00002);
                    //byte[] obj3Ddata =  org.apache.commons.codec.binary.Base64.decodeBase64(job.getString("obj3d"));
                    byte[] obj3Ddata = (org.apache.commons.codec.binary.Base64.decodeBase64(job.getString("obj3d").getBytes()));
                    Log.e(TAG, "obj3Ddata len:" + obj3Ddata.length);
                    Object3D o3d = Object3DManager.deserializeObject3D(/*blob.array()*/obj3Ddata);

                    Log.e(TAG, "o3d received name:" + o3d.getName());


                    byte[] txtr = (org.apache.commons.codec.binary.Base64.decodeBase64(job.getString("file").getBytes()));
                    //byte[] txtr = job.getString("file").getBytes();
                    Log.e(TAG, "txtr len:" + txtr.length);

                    Bitmap bmp = BitmapFactory.decodeByteArray(txtr, 0, txtr.length);
                    //Bitmap bmp = Bit
                    if(bmp==null)
                        Log.e(TAG,"bmp==null");
                    //Bitmap mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);

                    Texture texture = new Texture(bmp);

                    Log.e(TAG, "textured!");
                    if (texture == null)
                        Log.e(TAG, "texture==null");
                    //texture = new Texture(200,200);
                    //texture = new Texture(BitmapHelper.convert(image));
                    TextureManager.getInstance().addTexture(o3d.getName() /*basename*/, texture);
                    Log.e(TAG, "texture added to texturemanager");


                    jpctWorldManager.addObjectToCreationQueue(o3d, loc);
                } catch (IOException e) {
                    Log.e(TAG, "IOException");
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "ClassNotFoundException");
                    Log.e(TAG, e.getMessage());
                }

            } else {
                Log.e(TAG, job.getString("message"));
                return;
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException");
            Log.e(TAG, e.getMessage());
        }

    }


    /*
    public void run2() {

        try {
            Log.e(TAG, "run()");
            Log.e(TAG, "message len:" + message.length());
            Log.e(TAG, message.substring(0, 20));
            //Log.e(TAG, message.substring(20,40));
            //Log.e(TAG, message.substring(40,60));
            //Log.e(TAG, message.substring(1398155,1398170));
            JSONObject job = new JSONObject(message);

            //Boolean successful = job.getBoolean("successful");
            Boolean successful = (Boolean) job.get("successful");
            Log.e(TAG, "successful not blocking");
            Log.e(TAG, "successful:" + successful.toString());
            if (successful) {
                //String basename = job.getString("basename");
                //Log.e("basename", basename);
                //String extension= job.getString("extension");
                //String txtFile = job.getString("file");

                try {
                    //Location loc = locationFusion.getAsynchBestLocationAmongLocators();
                    //loc.setLongitude(loc.getLongitude()+0.00002);
                    //byte[] obj3Ddata =  org.apache.commons.codec.binary.Base64.decodeBase64(job.getString("obj3d"));
                    byte[] obj3Ddata = (org.apache.commons.codec.binary.Base64.decodeBase64(job.getString("obj3d").getBytes()));
                    Log.e(TAG, "obj3Ddata len:" + obj3Ddata.length);
                    Object3D o3d = Object3DManager.deserializeObject3D(obj3Ddata);

                    Log.e(TAG, "o3d received name:" + o3d.getName());


                    byte[] txtr = (org.apache.commons.codec.binary.Base64.decodeBase64(job.getString("file").getBytes()));
                    //byte[] txtr = job.getString("file").getBytes();
                    Log.e(TAG, "txtr len:" + txtr.length);
                    Texture texture = Object3DManager.deserializeTexture(txtr);


                    Log.e(TAG, "textured!");
                    if (texture == null)
                        Log.e(TAG, "texture==null");
                    //texture = new Texture(200,200);
                    //texture = new Texture(BitmapHelper.convert(image));
                    TextureManager.getInstance().addTexture(o3d.getName(), texture);
                    Log.e(TAG, "texture added to texturemanager");


                    jpctWorldManager.addObjectToCreationQueue(o3d, loc);
                } catch (IOException e) {
                    Log.e(TAG, "IOException");
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "ClassNotFoundException");
                    Log.e(TAG, e.getMessage());
                }

            } else {
                Log.e(TAG, job.getString("message"));
                return;
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException");
            Log.e(TAG, e.getMessage());
        }

    }
        */


}
