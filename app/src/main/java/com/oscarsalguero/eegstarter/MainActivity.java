/***
 * Copyright (c) 2015 Oscar Salguero www.oscarsalguero.com
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oscarsalguero.eegstarter;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGRawMulti;

/**
 * Main Activity
 * <p/>
 * Connects to a MindWave EEG device via bluetooth and starts listening for attention, meditation, heart rate and blink readings.
 * It logs raw data to LOG CAT when the rawEnabled flag is set to true.
 *
 * @author RacZo
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 100;
    private BluetoothAdapter bluetoothAdapter;
    private TextView textViewAttention;
    private TextView textViewMeditation;
    private TextView textViewHeartRate;
    private TextView textViewBlink;
    private TextView textViewRawData;
    private TGDevice tgDevice;
    private final boolean rawEnabled = true;
    private static final String LOG_TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewAttention = (TextView) findViewById(R.id.text_view_attention);
        textViewMeditation = (TextView) findViewById(R.id.text_view_meditation);
        textViewHeartRate = (TextView) findViewById(R.id.text_view_heart_rate);
        textViewBlink = (TextView) findViewById(R.id.text_view_blink);
        textViewRawData = (TextView) findViewById(R.id.text_view_raw_data);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_not_available), Toast.LENGTH_LONG).show();
            finish();
            return;
        } else {
            /* Creates the TGDevice */
            tgDevice = new TGDevice(bluetoothAdapter, new Handler() {

                @Override
                public void handleMessage(Message message) {
                    switch (message.what) {
                        case TGDevice.MSG_STATE_CHANGE:

                            switch (message.arg1) {
                                case TGDevice.STATE_IDLE:
                                    break;
                                case TGDevice.STATE_CONNECTING:
                                    Log.d(LOG_TAG, "Connecting...");
                                    break;
                                case TGDevice.STATE_CONNECTED:
                                    Log.d(LOG_TAG, "Connected!");
                                    tgDevice.start();
                                    break;
                                case TGDevice.STATE_NOT_FOUND:
                                    Log.e(LOG_TAG, "Device not found");
                                    break;
                                case TGDevice.STATE_NOT_PAIRED:
                                    Log.w(LOG_TAG, "Device not paired");
                                    break;
                                case TGDevice.STATE_DISCONNECTED:
                                    Log.d(LOG_TAG, "Device disconnected.");
                            }

                            break;
                        case TGDevice.MSG_POOR_SIGNAL:
                            if(message.arg1 > 0) {
                                Log.w(LOG_TAG, "Poor signal: " + message.arg1);
                            }
                            break;
                        case TGDevice.MSG_RAW_DATA:
                            if(rawEnabled) {
                                updateRawData(message.arg1);
                            }
                            break;
                        case TGDevice.MSG_HEART_RATE:
                            updateHeartRate(message.arg1); // Never updates (may depend on the device model)
                            break;
                        case TGDevice.MSG_ATTENTION:
                            updateAttention(message.arg1);
                            break;
                        case TGDevice.MSG_MEDITATION:
                            updateMeditation(message.arg1);
                            break;
                        case TGDevice.MSG_BLINK:
                            updateBlink(message.arg1);
                            break;
                        case TGDevice.MSG_RAW_COUNT:
                            //Log.i(LOG_TAG, "Raw count: " + message.arg1);
                            break;
                        case TGDevice.MSG_LOW_BATTERY:
                            showLowBatteryToast();
                            break;
                        case TGDevice.MSG_RAW_MULTI:
                            TGRawMulti tgRawMulti = (TGRawMulti)message.obj;
                            Log.i(LOG_TAG, "Raw channel 1: " + tgRawMulti.ch1);
                            Log.i(LOG_TAG, "Raw channel 2: " + tgRawMulti.ch2);
                            Log.i(LOG_TAG, "Raw channel 3: " + tgRawMulti.ch3);
                            Log.i(LOG_TAG, "Raw channel 4: " + tgRawMulti.ch4);
                            Log.i(LOG_TAG, "Raw channel 5: " + tgRawMulti.ch5);
                            Log.i(LOG_TAG, "Raw channel 6: " + tgRawMulti.ch6);
                            Log.i(LOG_TAG, "Raw channel 7: " + tgRawMulti.ch7);
                            Log.i(LOG_TAG, "Raw channel 8: " + tgRawMulti.ch8);
                        default:
                            break;
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForBluetooth();
    }

    @Override
    public void onDestroy() {
        tgDevice.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_connect:
                connect();
                break;
            case R.id.action_about:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.git_hub_repo_url)));
                startActivity(intent);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void connect() {
        if (tgDevice.getState() != TGDevice.STATE_CONNECTING && tgDevice.getState() != TGDevice.STATE_CONNECTED) {
            tgDevice.connect(rawEnabled);
        }
    }

    private void checkForBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void updateAttention(int attentionValue) {
        textViewAttention.setText(String.valueOf(attentionValue));
    }

    private void updateMeditation(int meditationValue) {
        textViewMeditation.setText(String.valueOf(meditationValue));
    }

    private void updateBlink(int blinkValue) {
        textViewBlink.setText(String.valueOf(blinkValue));
    }

    private void updateHeartRate(int heartRateValue) {
        textViewHeartRate.setText(String.valueOf(heartRateValue));
    }

    private void updateRawData(int rawData) {
        textViewRawData.setText(String.valueOf(rawData));
    }

    private void showLowBatteryToast(){
        Toast.makeText(this, getString(R.string.device_low_battery), Toast.LENGTH_LONG).show();
    }

}
