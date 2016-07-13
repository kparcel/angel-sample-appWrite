package com.angel.sample_app;

/**
 * Created by Kayci on 7/12/2016.
 * Used to create an object of the acceleration magnitude that
 * has the value and the time stamp stored in it.
 * @author: Kayci Parcells
 */
public class AccelerationMagnitude {

    long timeStamp;
    int magnitudeValue;

    public AccelerationMagnitude(long currentTime, int accelMagnitude){
        timeStamp = currentTime;
        magnitudeValue = accelMagnitude;
    }

    public long getTimeStamp(){
        return timeStamp;
    }

    public int getAccelerationMag(){
        return magnitudeValue;
    }
}
