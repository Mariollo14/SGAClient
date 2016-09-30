package com.msali.AR.sga.videostream.utils;

public class MediaBlock {
    public int videoCount = 0;

    public int flag = 0;
    public int millis;

    private byte[] buffer;
    private int bufferLength;
    private int currentLength;



    public MediaBlock (int maxSize) {
        super();
        buffer = new byte[maxSize];
        bufferLength = maxSize;
        currentLength = 0;
    }

    public MediaBlock copy(){

        MediaBlock block = new MediaBlock(bufferLength);
        block.flag=this.flag;
        block.videoCount=this.videoCount;
        block.write(this.data(),bufferLength);
        return block;
    }


    public void reset() {
        synchronized ( this) {
            currentLength = 0;
            videoCount = 0;
            flag = 0;
        }
    }

    public int length() {
        return currentLength;
    }

    public byte[] data() {
        return buffer;
    }

    public int writeVideo(byte[] data, int length) {
        //Log.e("WRITE VIDEO", "length"+data.length);
        if ( currentLength + length  >= bufferLength) {
            return 0;
        }

        for(int i = 0; i < length; i++) {
            buffer[currentLength] = data[i];
            currentLength++;
        }
        videoCount++;
        return length;
    }

    public int write(byte[] data, int length) {
        if ( currentLength + length  >= bufferLength) {
            return 0;
        }

        for(int i = 0; i < length; i++) {
            buffer[currentLength] = data[i];
            currentLength++;
        }

        return length;
    }
}
