package com.angel.sample_app;

/**
 * Created by Kayci on 7/11/2016.
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
