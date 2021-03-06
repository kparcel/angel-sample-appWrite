package com.angel.sample_app;

import com.angel.sdk.ChTemperatureMeasurement;

import java.util.GregorianCalendar;

/**
 * Created by Kayci on 7/11/2016.
 * Used to create a Temperature object that holds the
 * system timestamp (mCurrentTime), the temperature value,
 * and the timestamp associated with the measurement (timeStamp)
 * which is an incredibly long time stamp.
 * @author: Kayci Parcells
 */
public class Temperature {
    public float measurementValue;
    public GregorianCalendar timeStamp;
    long mCurrentTime;

    public Temperature (long currentTime, float temp, GregorianCalendar time){
        measurementValue = temp;
        timeStamp = time;
        mCurrentTime = currentTime;
    }

    public long getCurrentTime(){
        return mCurrentTime;
    }

    public float getMeasurement(){
        return measurementValue;
    }

    public GregorianCalendar getTime(){
        return timeStamp;
    }
}
