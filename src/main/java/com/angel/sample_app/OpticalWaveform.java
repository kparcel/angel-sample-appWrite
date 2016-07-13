package com.angel.sample_app;

import com.angel.sdk.ChOpticalWaveform;

import java.util.ArrayList;

/**
 * Created by Kayci on 7/12/2016.
 * Used to create an OpticalWaveform object that can
 * hold the wave values and a timestamp.
 * @author: Kayci Parcells
 */
public class OpticalWaveform {

    int greenData;
    int blueData;
    long timeStamp;

    public OpticalWaveform(long currentTime, int green, int blue){
        timeStamp = currentTime;
        greenData = green;
        blueData = blue;

    }

    public long getTimeStamp(){
        return timeStamp;
    }

    public int getGreenOpticalWaveform(){
        return greenData;
    }

    public int getBlueOpticalWaveform(){
        return blueData;
    }



}
