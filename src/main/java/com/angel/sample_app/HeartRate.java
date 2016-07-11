package com.angel.sample_app;

/**
 * Created by Kayci on 7/11/2016.
 */
public class HeartRate {

    int mHeartRate;
    int mEnergyExpended;
    int[] mRRIntervals;
    long mCurrentTime;

    public HeartRate(long currentTime, int heartRate, int energyExp, int[] rrIntervals){
        mHeartRate = heartRate;
        mEnergyExpended = energyExp;
        mRRIntervals = rrIntervals;
        mCurrentTime = currentTime;
    }

    public long getCurrentTime(){
        return mCurrentTime;
    }

    public int getHeartRate(){
        return mHeartRate;
    }

    public int getEnergyExpended(){
        return mEnergyExpended;
    }

    public int[] getRRIntervals(){
        return mRRIntervals;
    }
}
