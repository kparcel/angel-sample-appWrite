package com.angel.sample_app;

/**
 * Created by Kayci on 7/11/2016.
 * Used to create a StepCount object that holds the
 * step count value and the timestamp.
 * @autor: Kayci Parcells
 */
public class StepCount {
    int msteps;
    long mCurrentTime;

    public StepCount (long currentTime, int steps){
        msteps = steps;
        mCurrentTime = currentTime;
    }

    public long getCurrentTime(){
        return mCurrentTime;
    }

    public int getSteps(){
        return msteps;
    }
}
