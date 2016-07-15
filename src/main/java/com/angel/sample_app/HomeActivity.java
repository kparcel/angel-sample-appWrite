/*
 * Copyright (c) 2015, Seraphim Sense Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 *    and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific prior written
 *    permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.angel.sample_app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.angel.sdk.BleCharacteristic;
import com.angel.sdk.BleDevice;
import com.angel.sdk.ChAccelerationEnergyMagnitude;
import com.angel.sdk.ChAccelerationWaveform;
import com.angel.sdk.ChBatteryLevel;
import com.angel.sdk.ChHeartRateMeasurement;
import com.angel.sdk.ChOpticalWaveform;
import com.angel.sdk.ChStepCount;
import com.angel.sdk.ChTemperatureMeasurement;
import com.angel.sdk.SrvActivityMonitoring;
import com.angel.sdk.SrvBattery;
import com.angel.sdk.SrvHealthThermometer;
import com.angel.sdk.SrvHeartRate;
import com.angel.sdk.SrvWaveformSignal;

import junit.framework.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class HomeActivity extends Activity implements View.OnClickListener {

    public ArrayList<Temperature> temperatureList;
    public ArrayList<StepCount> stepCountList;
    public ArrayList<HeartRate> heartRateList;
    public ArrayList<OpticalWaveform> opticalWaveformList;
    public ArrayList<AccelerationWaveform> accelerationWaveformList;
    public ArrayList<AccelerationMagnitude> accelerationMagnitudeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements);
        orientation = getResources().getConfiguration().orientation;
        activityButton = (Button) this.findViewById(R.id.button);
        activityButton.setOnClickListener(this);

        mHandler = new Handler(this.getMainLooper());
        temperatureList = new ArrayList<Temperature>();
        stepCountList = new ArrayList<StepCount>();
        heartRateList = new ArrayList<HeartRate>();
        opticalWaveformList = new ArrayList<OpticalWaveform>();
        accelerationWaveformList = new ArrayList<AccelerationWaveform>();
        accelerationMagnitudeList = new ArrayList<AccelerationMagnitude>();
        mContext = this;

        mPeriodicReader = new Runnable() {
            @Override
            public void run() {
                mBleDevice.readRemoteRssi();
                if (mChAccelerationEnergyMagnitude != null) {
                    mChAccelerationEnergyMagnitude.readValue(mAccelerationEnergyMagnitudeListener);

                }

                mHandler.postDelayed(mPeriodicReader, RSSI_UPDATE_INTERVAL);
            }
        };

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mGreenOpticalWaveformView = (GraphView) findViewById(R.id.graph_green);
            mGreenOpticalWaveformView.setStrokeColor(0xffffffff);
            mBlueOpticalWaveformView = (GraphView) findViewById(R.id.graph_blue);
            mBlueOpticalWaveformView.setStrokeColor(0xffffffff);
            mAccelerationWaveformView = (GraphView) findViewById(R.id.graph_acceleration);
            mAccelerationWaveformView.setStrokeColor(0xfff7a300);
        }

    }

    protected void onStart() {
        super.onStart();

        Bundle extras = getIntent().getExtras();
        assert(extras != null);
        mBleDeviceAddress = extras.getString("ble_device_address");

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            connectGraphs(mBleDeviceAddress);
        } else {
            connect(mBleDeviceAddress);
        }


        //if the thread for data plotting and saving has not yet started
        //or if it has been interrupted by something like a lost BLE connection
        //restart the thread
        if(mThread == null) {
            mThread = new Thread(new DataPlotAndSave());
            mThread.start();
        } else if(!mThread.isAlive() || mThread.isInterrupted()){
            mThread.interrupt();
            mThread = null;
            mThread = new Thread(new DataPlotAndSave());
            mThread.start();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            displaySignalStrength(0);
        }
        unscheduleUpdaters();
        mBleDevice.disconnect();
    }

    private void connectGraphs(String deviceAddress) {

        if (mBleDevice != null) {
            mBleDevice.disconnect();
        }
        mBleDevice = new BleDevice(this, mDeviceGraphLifecycleCallback, mHandler);

        try {
            mBleDevice.registerServiceClass(SrvWaveformSignal.class);
            // Added these in so it would still record data to file when phone is landscape
            mBleDevice.registerServiceClass(SrvHeartRate.class);
            mBleDevice.registerServiceClass(SrvHealthThermometer.class);
            mBleDevice.registerServiceClass(SrvBattery.class);
            mBleDevice.registerServiceClass(SrvActivityMonitoring.class);

        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        } catch (InstantiationException e) {
            throw new AssertionError();
        }

        mBleDevice.connect(deviceAddress);

    }


    private void connect(String deviceAddress) {
        // A device has been chosen from the list. Create an instance of BleDevice,
        // populate it with interesting services and then connect

        if (mBleDevice != null) {
            mBleDevice.disconnect();
        }
        mBleDevice = new BleDevice(this, mDeviceLifecycleCallback, mHandler);

        try {
            mBleDevice.registerServiceClass(SrvHeartRate.class);
            mBleDevice.registerServiceClass(SrvHealthThermometer.class);
            mBleDevice.registerServiceClass(SrvBattery.class);
            mBleDevice.registerServiceClass(SrvActivityMonitoring.class);
            mBleDevice.registerServiceClass(SrvWaveformSignal.class);


        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        } catch (InstantiationException e) {
            throw new AssertionError();
        }

        mBleDevice.connect(deviceAddress);

        scheduleUpdaters();
        displayOnDisconnect();

    }

    private final BleDevice.LifecycleCallback mDeviceGraphLifecycleCallback = new BleDevice.LifecycleCallback() {
        @Override
        public void onBluetoothServicesDiscovered(BleDevice bleDevice) {
            bleDevice.getService(SrvWaveformSignal.class).getAccelerationWaveform().enableNotifications(mAccelerationWaveformListener);
            bleDevice.getService(SrvWaveformSignal.class).getOpticalWaveform().enableNotifications(mOpticalWaveformListener);
            // Added for recording to file when phone is landscape
            bleDevice.getService(SrvHeartRate.class).getHeartRateMeasurement().enableNotifications(mHeartRateListener);
            bleDevice.getService(SrvHealthThermometer.class).getTemperatureMeasurement().enableNotifications(mTemperatureListener);
            bleDevice.getService(SrvBattery.class).getBatteryLevel().enableNotifications(mBatteryLevelListener);
            bleDevice.getService(SrvActivityMonitoring.class).getStepCount().enableNotifications(mStepCountListener);
        }

        @Override
        public void onBluetoothDeviceDisconnected() {
            unscheduleUpdaters();
            connectGraphs(mBleDeviceAddress);
        }

        @Override
        public void onReadRemoteRssi(int i) {

        }
    };


    /**
     * Upon Heart Rate Service discovery starts listening to incoming heart rate
     * notifications. {@code onBluetoothServicesDiscovered} is triggered after
     * {@link BleDevice#connect(String)} is called.
     */
    private final BleDevice.LifecycleCallback mDeviceLifecycleCallback = new BleDevice.LifecycleCallback() {
        @Override
        public void onBluetoothServicesDiscovered(BleDevice device) {
            device.getService(SrvHeartRate.class).getHeartRateMeasurement().enableNotifications(mHeartRateListener);
            device.getService(SrvHealthThermometer.class).getTemperatureMeasurement().enableNotifications(mTemperatureListener);
            device.getService(SrvBattery.class).getBatteryLevel().enableNotifications(mBatteryLevelListener);
            device.getService(SrvActivityMonitoring.class).getStepCount().enableNotifications(mStepCountListener);
            device.getService(SrvWaveformSignal.class).getAccelerationWaveform().enableNotifications(mAccelerationWaveformListener);
            device.getService(SrvWaveformSignal.class).getOpticalWaveform().enableNotifications(mOpticalWaveformListener);
            mChAccelerationEnergyMagnitude = device.getService(SrvActivityMonitoring.class).getChAccelerationEnergyMagnitude();
            Assert.assertNotNull(mChAccelerationEnergyMagnitude);
        }


        @Override
        public void onBluetoothDeviceDisconnected() {
            displayOnDisconnect();
            unscheduleUpdaters();

            // Re-connect immediately
            connect(mBleDeviceAddress);
        }

        @Override
        public void onReadRemoteRssi(final int rssi) {
            displaySignalStrength(rssi);
        }
    };


    //This is the Acceleration waveform listener for the horizontal graph view & portrait
    // file storage
    private final BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue> mAccelerationWaveformListener = new BleCharacteristic.ValueReadyCallback<ChAccelerationWaveform.AccelerationWaveformValue>() {
        @Override
        public void onValueReady(ChAccelerationWaveform.AccelerationWaveformValue accelerationWaveformValue) {
            if (accelerationWaveformValue != null && accelerationWaveformValue.wave != null) {
                for (Integer item : accelerationWaveformValue.wave) {
                    //grab current time stamp for data logging in file
                    currentTime = System.currentTimeMillis();
                    AccelerationWaveform newAccelerationWaveform =
                            new AccelerationWaveform(currentTime, item);
                    accelerationWaveformList.add(newAccelerationWaveform);

                }

            }

            if (accelerationWaveformValue != null && accelerationWaveformValue.wave != null && mAccelerationWaveformView != null)
                for (Integer item : accelerationWaveformValue.wave) {
                    mAccelerationWaveformView.addValue(item);
                }

        }
    };


    // This is the listener used when the phone has a landscape orientation
    private final BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue> mOpticalWaveformListener = new BleCharacteristic.ValueReadyCallback<ChOpticalWaveform.OpticalWaveformValue>() {
        @Override
        public void onValueReady(ChOpticalWaveform.OpticalWaveformValue opticalWaveformValue) {
            if (opticalWaveformValue != null && opticalWaveformValue.wave != null) {
                // for saving to file in portrait orientation
                for (ChOpticalWaveform.OpticalSample item : opticalWaveformValue.wave) {
                    //grab current time stamp for data logging in file
                    currentTime = System.currentTimeMillis();
                    OpticalWaveform newOpticalWaveform =
                            new OpticalWaveform(currentTime, item.green, item.blue);
                    opticalWaveformList.add(newOpticalWaveform);
                }
            }
            // for making graphs & saving to file in landscape orientation
            if (opticalWaveformValue != null && opticalWaveformValue.wave != null && orientation == Configuration.ORIENTATION_LANDSCAPE){
                for (ChOpticalWaveform.OpticalSample item : opticalWaveformValue.wave) {
                    mGreenOpticalWaveformView.addValue(item.green);
                    mBlueOpticalWaveformView.addValue(item.blue);

                    //grab current time stamp for data logging in file
                    currentTime = System.currentTimeMillis();
                    OpticalWaveform newOpticalWaveform =
                            new OpticalWaveform(currentTime, item.green, item.blue);
                    opticalWaveformList.add(newOpticalWaveform);
                }
            }
        }
    };



    private final BleCharacteristic.ValueReadyCallback<ChHeartRateMeasurement.HeartRateMeasurementValue> mHeartRateListener = new BleCharacteristic.ValueReadyCallback<ChHeartRateMeasurement.HeartRateMeasurementValue>() {
        @Override
        public void onValueReady(final ChHeartRateMeasurement.HeartRateMeasurementValue hrMeasurement) {
            displayHeartRate(hrMeasurement.getHeartRateMeasurement());
            //grab current time stamp for data logging in file
            currentTime = System.currentTimeMillis();
            HeartRate newHeartRate =
                    new HeartRate(currentTime, hrMeasurement.getHeartRateMeasurement(), hrMeasurement.getEnergyExpended(), hrMeasurement.getRRIntervals());
            heartRateList.add(newHeartRate);
        }
    };

    private final BleCharacteristic.ValueReadyCallback<ChBatteryLevel.BatteryLevelValue> mBatteryLevelListener =
        new BleCharacteristic.ValueReadyCallback<ChBatteryLevel.BatteryLevelValue>() {
        @Override
        public void onValueReady(final ChBatteryLevel.BatteryLevelValue batteryLevel) {
            displayBatteryLevel(batteryLevel.value);
        }
    };

    private final BleCharacteristic.ValueReadyCallback<ChTemperatureMeasurement.TemperatureMeasurementValue> mTemperatureListener =
        new BleCharacteristic.ValueReadyCallback<ChTemperatureMeasurement.TemperatureMeasurementValue>() {

            @Override
            public void onValueReady(final ChTemperatureMeasurement.TemperatureMeasurementValue temperature) {
                displayTemperature(temperature.getTemperatureMeasurement());
                //grab current time stamp for data logging in file
                currentTime = System.currentTimeMillis();
                Temperature newTempValue = new Temperature(currentTime, temperature.getTemperatureMeasurement(), temperature.getTimeStamp());
                temperatureList.add(newTempValue);

                //if the thread for data plotting and saving has not yet started
                //or if it has been interrupted by something like a lost BLE connection
                //restart the thread
                if(mThread == null) {
                    mThread = new Thread(new DataPlotAndSave());
                    mThread.run();
                } else if(!mThread.isAlive() || mThread.isInterrupted()){
                    mThread.interrupt();
                    mThread = null;
                    mThread = new Thread(new DataPlotAndSave());
                    mThread.run();
                }

            }
        };

    private final BleCharacteristic.ValueReadyCallback<ChStepCount.StepCountValue> mStepCountListener =
        new BleCharacteristic.ValueReadyCallback<ChStepCount.StepCountValue>() {
            @Override
            public void onValueReady(final ChStepCount.StepCountValue stepCountValue) {
                displayStepCount(stepCountValue.value);
                //grab current time stamp for data logging in file
                currentTime = System.currentTimeMillis();
                StepCount newStepValue = new StepCount(currentTime, stepCountValue.value);
                stepCountList.add(newStepValue);
            }
        };

    private final BleCharacteristic.ValueReadyCallback<ChAccelerationEnergyMagnitude.AccelerationEnergyMagnitudeValue> mAccelerationEnergyMagnitudeListener =
        new BleCharacteristic.ValueReadyCallback<ChAccelerationEnergyMagnitude.AccelerationEnergyMagnitudeValue>() {
            @Override
            public void onValueReady(final ChAccelerationEnergyMagnitude.AccelerationEnergyMagnitudeValue accelerationEnergyMagnitudeValue) {
                displayAccelerationEnergyMagnitude(accelerationEnergyMagnitudeValue.value);
            }
        };

    private void displayHeartRate(final int bpm) {
        TextView textView = (TextView)findViewById(R.id.textview_heart_rate);
        textView.setText(bpm + " bpm");

        ScaleAnimation effect =  new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);

        View heartView = findViewById(R.id.imageview_heart);
        heartView.startAnimation(effect);
    }

    private void displaySignalStrength(int db) {
        int iconId;
        if (db > -70) {
            iconId = R.drawable.ic_signal_4;
        } else if (db > - 80) {
            iconId = R.drawable.ic_signal_3;
        } else if (db > - 85) {
            iconId = R.drawable.ic_signal_2;
        } else if (db > - 87) {
            iconId = R.drawable.ic_signal_1;
        } else {
            iconId = R.drawable.ic_signal_0;
        }
        ImageView imageView = (ImageView)findViewById(R.id.imageview_signal);
        imageView.setImageResource(iconId);
        TextView textView = (TextView)findViewById(R.id.textview_signal);
        textView.setText(db + "db");
    }

    private void displayBatteryLevel(int percents) {
        int iconId;
        if (percents < 20) {
            iconId = R.drawable.ic_battery_0;
        } else if (percents < 40) {
            iconId = R.drawable.ic_battery_1;
        } else if (percents < 60) {
            iconId = R.drawable.ic_battery_2;
        } else if (percents < 80) {
            iconId = R.drawable.ic_battery_3;
        } else {
            iconId = R.drawable.ic_battery_4;
        }

        ImageView imageView = (ImageView)findViewById(R.id.imageview_battery);
        imageView.setImageResource(iconId);
        TextView textView = (TextView)findViewById(R.id.textview_battery);
        textView.setText(percents + "%");
    }

    private void displayTemperature(final float degreesCelsius) {
        TextView textView = (TextView)findViewById(R.id.textview_temperature);
        textView.setText(degreesCelsius + "\u00b0C");

        ScaleAnimation effect =  new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);
        View thermometerTop = findViewById(R.id.imageview_thermometer_top);
        thermometerTop.startAnimation(effect);

    }


    private void displayStepCount(final int stepCount) {
        TextView textView = (TextView)findViewById(R.id.textview_step_count);
        Assert.assertNotNull(textView);
        textView.setText(stepCount + " steps");

        TranslateAnimation moveDown = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_PARENT, 0.25f);
        moveDown.setDuration(ANIMATION_DURATION);
        moveDown.setRepeatMode(Animation.REVERSE);
        moveDown.setRepeatCount(1);
        View stepLeft = findViewById(R.id.imageview_step_left);
        stepLeft.startAnimation(moveDown);

        TranslateAnimation moveUp = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_PARENT, -0.25f);
        moveUp.setDuration(ANIMATION_DURATION);
        moveUp.setRepeatMode(Animation.REVERSE);
        moveUp.setRepeatCount(1);
        View stepRight = findViewById(R.id.imageview_step_right);
        stepRight.startAnimation(moveUp);
    }

    private void displayAccelerationEnergyMagnitude(final int accelerationEnergyMagnitude) {
        TextView textView = (TextView) findViewById(R.id.textview_acceleration);
        Assert.assertNotNull(textView);
        textView.setText(accelerationEnergyMagnitude + "g");

        ScaleAnimation effect =  new ScaleAnimation(1f, 0.5f, 1f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        effect.setDuration(ANIMATION_DURATION);
        effect.setRepeatMode(Animation.REVERSE);
        effect.setRepeatCount(1);

        View imageView = findViewById(R.id.imageview_acceleration);
        imageView.startAnimation(effect);
    }

    private void displayOnDisconnect() {
        displaySignalStrength(-99);
        displayBatteryLevel(0);
    }


    @Override
    public void onClick(View v)
    {
        activityButton.setText("Recorded");

        try{

            //put angel file in Android/data/com.angel.sample_app
            File traceFile = new File(mContext.getExternalFilesDir(null), "angelData.txt");
            File temperatureFile = new File(mContext.getExternalFilesDir(null), "temperatureData.txt");
            File stepsFile = new File(mContext.getExternalFilesDir(null), "stepCountFile.txt");
            File heartFile = new File(mContext.getExternalFilesDir(null), "heartInfoFile.txt");
            File accelMagFile = new File(mContext.getExternalFilesDir(null), "accelerationMagnitudeFile.txt");
            File opticalWaveformFile = new File(mContext.getExternalFilesDir(null), "opticalWaveformFile.txt");
            File accelerationWaveformFile = new File(mContext.getExternalFilesDir(null), "accelerationWaveform.txt");
            //if the file does not already exist, create it
            if (!traceFile.exists()){
                traceFile.createNewFile();
            }

            Date now;
            String strDate;
            FileWriter angelSensorFileWriter = new FileWriter(traceFile, true /*append*/);
            FileWriter temperatureFileWriter = new FileWriter(temperatureFile, true);
            FileWriter stepCountFileWriter = new FileWriter(stepsFile, true);
            FileWriter heartFileWriter = new FileWriter(heartFile, true);
            FileWriter accelMagnitudeFileWriter = new FileWriter(accelMagFile, true);
            FileWriter opticalFileWriter = new FileWriter(opticalWaveformFile, true);
            FileWriter accelerationFileWriter = new FileWriter(accelerationWaveformFile, true);


            temperatureFileWriter.write("NEW ACTIVITY STARTED" + "\n");
            stepCountFileWriter.write("NEW ACTIVITY STARTED" + "\n");
            heartFileWriter.write("NEW ACTIVITY STARTED" + "\n");
            accelerationFileWriter.write("NEW ACTIVITY STARTED" + "\n");
            opticalFileWriter.write("NEW ACTIVITY STARTED" + "\n");
            accelerationFileWriter.write("NEW ACTIVITY STARTED" + "\n");



            temperatureFileWriter.close();
            stepCountFileWriter.close();
            heartFileWriter.close();
            opticalFileWriter.close();
            accelerationFileWriter.close();
            accelerationFileWriter.close();
            angelSensorFileWriter.close(); // close file writer

            activityButton.setText("New Activity");

        } catch (Exception e){
            Log.e(TAG, "ERROR with file manipulation or plotting!\n" + e.getMessage());
        }

    }












    private void scheduleUpdaters() {
        mHandler.post(mPeriodicReader);
    }

    private void unscheduleUpdaters() {
        mHandler.removeCallbacks(mPeriodicReader);
    }

    private static final int RSSI_UPDATE_INTERVAL = 1000; // Milliseconds
    private static final int ANIMATION_DURATION = 500; // Milliseconds

    private int orientation;
    private Button activityButton;

    private GraphView mAccelerationWaveformView, mBlueOpticalWaveformView, mGreenOpticalWaveformView;

    private BleDevice mBleDevice;
    private String mBleDeviceAddress;

    private Handler mHandler;
    private Runnable mPeriodicReader;
    private ChAccelerationEnergyMagnitude mChAccelerationEnergyMagnitude = null;


    public Context mContext;
    /* Thread for plotting and saving data */
    public Thread mThread;
    private long currentTime; //timestamp for file saving


    /* Tag for LogCat purposes */
    private final static String TAG = HomeActivity.class.getSimpleName();


    public class DataPlotAndSave implements Runnable {

        @Override
        public void run() {


            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sdfDate.setTimeZone(TimeZone.getDefault());

            try{

                //put angel file in Android/data/com.angel.sample_app
                File traceFile = new File(mContext.getExternalFilesDir(null), "angelData.txt");
                File temperatureFile = new File(mContext.getExternalFilesDir(null), "temperatureData.txt");
                File stepsFile = new File(mContext.getExternalFilesDir(null), "stepCountFile.txt");
                File heartFile = new File(mContext.getExternalFilesDir(null), "heartInfoFile.txt");
                File accelMagFile = new File(mContext.getExternalFilesDir(null), "accelerationMagnitudeFile.txt");
                File opticalWaveformFile = new File(mContext.getExternalFilesDir(null), "opticalWaveformFile.txt");
                File accelerationWaveformFile = new File(mContext.getExternalFilesDir(null), "accelerationWaveform.txt");
                //if the file does not already exist, create it
                if (!traceFile.exists()){
                    traceFile.createNewFile();
                }

                Date now;
                String strDate;
                FileWriter angelSensorFileWriter = new FileWriter(traceFile, true /*append*/);
                FileWriter temperatureFileWriter = new FileWriter(temperatureFile, true);
                FileWriter stepCountFileWriter = new FileWriter(stepsFile, true);
                FileWriter heartFileWriter = new FileWriter(heartFile, true);
                FileWriter accelMagnitudeFileWriter = new FileWriter(accelMagFile, true);
                FileWriter opticalFileWriter = new FileWriter(opticalWaveformFile, true);
                FileWriter accelerationFileWriter = new FileWriter(accelerationWaveformFile, true);


                if (temperatureList.size() > 0){
                    for (int i = 0; i < temperatureList.size(); i++){
                        Temperature temperature = temperatureList.get(i);
                        long currentTime = temperature.getCurrentTime();
                        float value = temperature.getMeasurement();
                        // The data values' time stamp
                        // I don't recommend using it. It's hideous.
                        GregorianCalendar timeStamp = temperature.getTime();

                        //StepCount steps = stepCountList.get(i);
                        //int stepValue = steps.getSteps();
                        //write time stamp and filtered value to file
                        now = new Date(currentTime);
                        strDate = sdfDate.format(now);
                        temperatureFileWriter.write(strDate + ", " + "||" + ", " + value + ", " + "||" + ", " + "00" + "\n");
                        temperatureList.remove(i);
                        //stepCountList.remove(i);
                    }
                }

                if (stepCountList.size() > 0){
                    for (int i = 0; i < stepCountList.size(); i++){
                        StepCount steps = stepCountList.get(i);
                        long currentTime = steps.getCurrentTime();
                        int stepValue = steps.getSteps();

                        now = new Date(currentTime);
                        strDate = sdfDate.format(now);
                        stepCountFileWriter.write(strDate + ", " + stepValue + "\n");
                        stepCountList.remove(i);
                    }
                }

                if (heartRateList.size() > 0){
                    for (int i = 0; i < heartRateList.size(); i++){
                        HeartRate heartRate = heartRateList.get(i);
                        long currentTime = heartRate.getCurrentTime();
                        int heartRateValue = heartRate.getHeartRate();
                        int energyExValue = heartRate.getEnergyExpended();
                        int[] rrInterval = heartRate.getRRIntervals();

                        now = new Date(currentTime);
                        strDate = sdfDate.format(now);
                        heartFileWriter.write(strDate + " ||" + heartRateValue + "||" + energyExValue + "||" + rrInterval + "\n");
                        heartRateList.remove(i);
                    }
                }

                if (accelerationMagnitudeList.size() > 0){
                    for (int i = 0; i < accelerationMagnitudeList.size(); i++){
                        AccelerationMagnitude magnitude = accelerationMagnitudeList.get(i);
                        long currentTime = magnitude.getTimeStamp();
                        int accelerationMagnitude = magnitude.getAccelerationMag();

                        now = new Date(currentTime);
                        strDate = sdfDate.format(now);
                        accelMagnitudeFileWriter.write(strDate + ", " + accelerationMagnitude + "\n");
                        accelerationMagnitudeList.remove(i);
                    }
                }

                if (opticalWaveformList.size() > 0){
                    for (int i = 0; i < opticalWaveformList.size(); i++) {
                        OpticalWaveform opticalWaveform = opticalWaveformList.get(i);
                        long currentTime = opticalWaveform.getTimeStamp();
                        int greenValue = opticalWaveform.getGreenOpticalWaveform();
                        int blueValue = opticalWaveform.getBlueOpticalWaveform();

                        now = new Date(currentTime);
                        strDate = sdfDate.format(now);
                        opticalFileWriter.write(strDate + " ||" + greenValue + "||" + blueValue + "||" + "\n");
                        opticalWaveformList.remove(i);
                    }
                }

                if (accelerationWaveformList.size() > 0){
                    for (int i = 0; i < accelerationWaveformList.size(); i++) {
                        AccelerationWaveform accelerationWaveform = accelerationWaveformList.get(i);
                        long currentTime = accelerationWaveform.getTimeStamp();
                        int accelerationValue = accelerationWaveform.getAccelerationWaveformValue();

                        now = new Date(currentTime);
                        strDate = sdfDate.format(now);
                        accelerationFileWriter.write(strDate + " ||" + accelerationValue + "||" + "\n");
                        accelerationWaveformList.remove(i);
                    }
                }


                temperatureFileWriter.close();
                stepCountFileWriter.close();
                heartFileWriter.close();
                opticalFileWriter.close();
                accelerationFileWriter.close();
                accelerationFileWriter.close();
                angelSensorFileWriter.close(); // close file writer

            } catch (Exception e){
                Log.e(TAG, "ERROR with file manipulation or plotting!\n" + e.getMessage());
            }


        }

    }


}
