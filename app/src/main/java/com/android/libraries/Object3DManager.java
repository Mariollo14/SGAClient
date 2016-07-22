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
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by Mario Salierno on 15/07/2016.
 */
public class Object3DManager {

    private Activity activity;
    private String TAG = "Object3DManager";


    public Object3DManager(Activity act){

        this.activity=act;

    }

    /*
    public Object3D createObject3D(){
        TexturedObject tObj = new TexturedObject("chair.3ds",//id
                R.drawable.chair,//p.e R.drawable.bigoffice//textureID
                0.025f,//scale
                -4,//x
                0,//y
                -2,//z
                1);//dim

        tObj.obj3D.rotateX((float)Math.toRadians(90.0));

        return tObj.obj3D;

    }
    */

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



    ////
    public static byte[] serializeTexture(Texture txt) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        serializeTexture(out, txt);

        byte[] serTxt = out.toByteArray();

        // close the stream
        out.close();

        return serTxt;
    }


    public static Texture deserializeTexture(byte[] serTxt) throws IOException, ClassNotFoundException {

        ByteArrayInputStream in = new ByteArrayInputStream(serTxt);

        return deserializeTexture(in);

    }




    public static void serializeTexture(OutputStream out, Texture txt) throws IOException {


        ObjectOutputStream oout = new ObjectOutputStream(out);

        // write something in the file
        oout.writeObject(txt);

        // close the stream
        oout.close();

    }


    public static Texture deserializeTexture(InputStream in) throws IOException, ClassNotFoundException {


        ObjectInputStream input = new ObjectInputStream(in);

        Texture txt = (Texture) input.readObject();

        return txt;

    }



    ////



}
