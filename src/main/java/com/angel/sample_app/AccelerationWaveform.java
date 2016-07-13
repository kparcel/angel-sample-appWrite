package com.angel.sample_app;

/**
 * Created by Kayci on 7/12/2016.
 * Used to create an AccelerationWaveform object that holds the
 * timestamp and acceleration waveform value which is
 * the magnitude and is calculated as Euclidean norm of the
 * XYZ acceleration components, that is sqrt(X^2 + Y^2 + Z^2).
 * @author: Kayci Parcells
 */
public class AccelerationWaveform {

    long timeStamp;
    int accelValue;

    public AccelerationWaveform(long currentTime, int acceleration){
        timeStamp = currentTime;
        accelValue = acceleration;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getAccelerationWaveformValue() {
        return accelValue;
    }
}
