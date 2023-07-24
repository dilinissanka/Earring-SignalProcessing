package com.example.ble_keyboard;

import static android.app.PendingIntent.getActivity;

import static java.sql.DriverManager.println;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    /***********************************************************************************************
     * These variables are for the UI componenets
     **********************************************************************************************/

    // This is the list where we will be saving the Necklaces in which we will be using
    private ArrayList<Necklace> necklaceList;

    // This is the recyclerView in which we will be showing the necklaces that you are
    // currently connected to
    private RecyclerView recyclerView;

    // This is the graph where you will be plotting points onto
    public GraphView ECGGraph;

    // This is the button for scanning
    public Button scanButton;

    // This will let you know if a button is clickable or not
    private boolean isButtonClickable = true;


    /***********************************************************************************************
     * Variables bellow are for connecting and for callbacks
     **********************************************************************************************/

    // This will let us know if the current device is active or not
    private BleDevice activeBleDevice;

    // This will let you know if we are currently scanning or not
    private boolean isScanning;
    // This will be used for the connecting message
    public Handler handler = new Handler();
    public Runnable runnable;

    // This is the notify callback
    public BleNotifyCallback callBack;

    // This is the ECG characteristic
    public BluetoothGattCharacteristic ecgCharacteristic;

    // This is the scan callback that we will be using for the connection
    public BleScanCallback scanCallback;

    // This is the manager that we will be using for connections
    public BluetoothGatt mConnect;



    /***********************************************************************************************
     * Variables for storage permissions
     **********************************************************************************************/

    // This is where we will get the Location Permission of the user
    final private int REQUEST_CODE_PERMISSION_LOCATION = 0;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    /***************************************************************************************************
     *                  This is onCreate which will happen when the program first runs                 *
     ***************************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        // ****************Get heart rate information******************************************
//        double heartRate = getHeartRateDouble();
//        System.out.println("This is the heart rate: " + heartRate);


        // ****************Get ECG classification**********************************************
//        String ecgClassification = getECGClassification();
//        System.out.println("This is the ECG classification: " + ecgClassification);


        // ****************Permission Checking*************************************************
        System.out.println("This is the permissions code");
        verifyStoragePermissions(this);
        checkPermissions();

        // ****************Scanning code*******************************************************
        System.out.println("This is the scanning code");
        // you have not pressed the scanning button
        this.isScanning = false;

        // This is to initalize the BLEManager
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);

        // ****************Displaying nessecary graph componenets*****************************
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // This button will be the button that you can press to draw the ECG graph
        Button graph_ECG = findViewById(R.id.ecg_button);

        // This will set the ECGGraph where you can put dots onto
        ECGGraph = findViewById(R.id.graph);

        // This is where you will set the titles of the graph
        ECGGraph.setTitle("ECG Graph");
        ECGGraph.setTitleTextSize(70);

        // This is where you will set the legend of the graph
        ECGGraph.getLegendRenderer().setVisible(false);
//        ECGGraph.getLegendRenderer().setVisible(true);
//        ECGGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
//        ECGGraph.getLegendRenderer().setWidth(400);
//        ECGGraph.getLegendRenderer().setSpacing(50);
//        ECGGraph.getLegendRenderer().setBackgroundColor(Color.GRAY);
//        ECGGraph.getLegendRenderer().setMargin(10);

        // This is where you will be setting the zooming and scrolling feature of the graph
        ECGGraph.getViewport().setScalable(true);
        ECGGraph.getViewport().setScalableY(true);


        //********************Initalize the layout components*********************************
        // This is to initialize the scannning button
        // scanButton = findViewById(R.id.start_connection);

        // This is to initalize the scanning text
        TextView connectingText = findViewById(R.id.connected_devices);
        int redColor = Color.rgb(255, 0, 0);
        connectingText.setTextColor(redColor);

        // This is to initalize the recyclerView that we are using
        recyclerView = findViewById(R.id.necklace_list);

        // These are the necklaces that we will be displaying in the recyclerView
        necklaceList = new ArrayList<>();


        // ***************This is where we will be setting the listeners for the buttons*********

        // This is where you will be initalizing the listener for the graph ECG button
        graph_ECG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("I clicked on the graph button!");
                try {
                    canGraphECG();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // This is where you will be initalizing the start connection button
        findViewById(R.id.start_connection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Starting connection!");
                checkNecklaceInput();
            }
        });
    }


    /***********************************************************************************************
     * These are the methods for connecting
     **********************************************************************************************/

    /**
     * This method checks to see if a person has inputted something for the necklace name or not
     */
    public void checkNecklaceInput() {

        // Here we will be getting the text that was inputted for the necklace name
        TextView inputNecklace = findViewById(R.id.neckalce_name);
        String necklaceName = inputNecklace.getText().toString();

        // This is the case when the user does not input anything
        if (necklaceName.equals("") && !isScanning) {
            Toast.makeText(getApplicationContext(), "Please input a name for the necklace!", Toast.LENGTH_SHORT).show();

            // This is the case when the user inputs something for the necklace name
        } else {
            scanStartOrStop();
        }
    }

    /**
     * Here we will determine whether we will need to start scanning or stop scanning
     */
    public void scanStartOrStop() {

        // This is the case when we don't have something scanning at the moment
        if (!isScanning) {

            // the scan button will be disabled since you do not want to
            // stop the connection midway
            disableButtonForDelay();

            // This is the runnable for the connecting... animation
            runnable = new Runnable() {
                int maxDotCounts = 4;
                int dotDelay = 250;
                int totalDuration = 10000;
                long startTime = System.currentTimeMillis();
                int dotCounts = 0;

                @Override
                public void run() {

                    // Here we will get the elapsed time
                    long elapsedTime = System.currentTimeMillis() - startTime;

                    // Here we will check to see if the connection is taking over 10 seconds
                    if (elapsedTime >= totalDuration) {

                        // Stop the execution of the runnable
                        handler.removeCallbacks(this::run);

                        // This is where we will need to stop the scanning and the blemanager instances
                        deviceNotFound();
                    } else {
                        // Here we will be setting the TextView and one more dot each time
                        TextView connectingText = findViewById(R.id.connected_devices);
                        String dots = new String(new char[dotCounts]).replace('\0', '.');
                        connectingText.setText("Connecting" + dots);

                        // Here we will set the connecting... icon to yellow
                        int yellowColor = Color.rgb(204, 204, 0);
                        connectingText.setTextColor(yellowColor);
                        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) connectingText.getLayoutParams();
                        layoutParams.setMargins(375, 475, layoutParams.rightMargin, layoutParams.bottomMargin);
                        connectingText.setLayoutParams(layoutParams);

                        // Here we will be updating the number of dots
                        dotCounts++;
                        if (dotCounts > maxDotCounts) {
                            dotCounts = 0;
                        }

                        // Schedule the next iteration of the Runnable after the dotDelay
                        handler.postDelayed(this, dotDelay);
                    }
                }
            };

            // Here we will start running the runnable
            handler.post(runnable);

            // set scanning to true, since we started scanning
            isScanning = true;

            // Here we will start the scanning
            startScan();
        } else {

            // This is where we will stop scanning
            stopScan();
        }
    }


    /**
     * This is the method that will be called when we are trying to connect to a device
     */
    private void startScan() {
        scanCallback = new BleScanCallback() {

            @Override
            public void onScanStarted(boolean success) {
                // just needed because it is BleScanCallBack
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            // This is so that we can connect if we find the right device, in other words the
            // device found has the same name as the device name that was inputted
            @Override
            public void onScanning(BleDevice bleDevice) {

                // This is the text that was given by the necklace name
                EditText editText = findViewById(R.id.neckalce_name);
                String text = editText.getText().toString();

                // This is the devices name
                if (bleDevice.getName() != null && bleDevice.getName().equals(text)) {
                    activeBleDevice = bleDevice;
                    connect(activeBleDevice);
                }
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                // just needed because it is BleScanCallBack
            }
        };

        // This is where we will be setting the scan instance for the scanner
        BleManager.getInstance().scan(scanCallback);
    }

    /**
     * This is a method that we will try to call when we are stopping a scan
     */
    public void stopScan() {

        // This is where we will be disconnecting
        BleManager.getInstance().disconnect(activeBleDevice);

        // Here we will be removing all of the series which are the lines in the graph
        ECGGraph.removeAllSeries();

        // This is where we will be setting the Graph ECG button text
        Button graphButton = findViewById(R.id.ecg_button);
        graphButton.setText("Graph ECG");

        // This is where we will have the button where you could start a connection
        Button scanButton = findViewById(R.id.start_connection);
        scanButton.setText("START CONNECTION");

        // This is where you can start scanning again
        isScanning = false;

        // This is where you will reset all of the necklaces in your list
        necklaceList = new ArrayList<>();

        // This is where you will be setting the ble device that you currently have to null
        activeBleDevice = null;

        // we are going to update the recycler in here
        setAdapter();

        // Here we will be updating all of the necessary components to
        // what they need to be when we have disconnected
        TextView connectingText = findViewById(R.id.connected_devices);
        int redColor = Color.rgb(255, 0, 0);
        connectingText.setTextColor(redColor);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) connectingText.getLayoutParams();
        layoutParams.setMargins(260, 475, layoutParams.rightMargin, layoutParams.bottomMargin);
        connectingText.setLayoutParams(layoutParams);
        connectingText.setText("No connected devices");

        // Here we will need to disconnect from the device
        if (ecgCharacteristic != null && activeBleDevice != null) {
            BleManager.getInstance().stopNotify(
                    activeBleDevice,
                    ecgCharacteristic.getService().getUuid().toString(),
                    ecgCharacteristic.getUuid().toString()
            );
        }
    }


    /**
     * This is where we will be connecting to the device,
     * We will first try to connect via the ATT protocol to determine how we will connect
     * between two devices and then GATT to determine how to read and write data between the devices
     * @param bleDevice: this is the bleDevice that we are trying to connect to
     */
    private void connect(final BleDevice bleDevice) {

        // Here we will be initalizing the callback for connect
        this.mConnect = BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                // just needed because we are using BleGattCallBack
            }

            // This is the case when the connection failed
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {

                // This is where we will be setting the UI componenets to their defaults since we
                // failed to connect
                Button scanButton = findViewById(R.id.start_connection);
                scanButton.setText("SCAN AGAIN");
                Toast.makeText(MainActivity.this, "Failed to connect.", Toast.LENGTH_LONG).show();
                Toast.makeText(MainActivity.this, exception.toString(), Toast.LENGTH_LONG).show();
                TextView connectingText = findViewById(R.id.connected_devices);
                int redColor = Color.rgb(255, 0, 0);
                connectingText.setTextColor(redColor);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) connectingText.getLayoutParams();
                layoutParams.setMargins(260, 475, layoutParams.rightMargin, layoutParams.bottomMargin);
                connectingText.setLayoutParams(layoutParams);
                connectingText.setText("No connected devices");

                println("connect fail");
                // Here we will stop the scanning for more ble devices
                stopScan();
            }

            /**
             * Here we will be dealing with the case when we have connected successfully to the device that
             * have chosen to connect to
             * @param bleDevice: this is the device that we are trying to connect to
             * @param gatt: This is the bluetooth instance that we will use for the connection
             * @param status: this is the status of the connection that we are currently on
             */
            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {

                // Here we will enable the button again in case that somebody wants to stop the
                // connection between devices
                buttonEnable();
                println("connect success");

                // Here we will make sure to set the scanning button to close connection
                // to let the user know that they can disconnect if they want to
                Button scanButton = findViewById(R.id.start_connection);
                scanButton.setText("CLOSE CONNECTION");

                // This is where we will be removing the runnable which stops the connection animation
                handler.removeCallbacks(runnable);

                // Here we will be setting the device that we are connecting to
                activeBleDevice = bleDevice;

                // This is where we will set the UI elements to indicate that we connected
                // to the device that we wanted to connect to
                TextView textView = findViewById(R.id.connected_devices);
                int greenColor = Color.rgb(0, 255, 51);
                textView.setText("Connected Devices: ");
                textView.setTextColor(greenColor);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
                layoutParams.setMargins(260, 475, layoutParams.rightMargin, layoutParams.bottomMargin);
                textView.setLayoutParams(layoutParams);

                // Here we will be adding to the list of devices
                Necklace newNecklace = new Necklace(bleDevice.getName(), true, "...");
                necklaceList.add(newNecklace);

                // This is where we will be updating the recycler view
                setAdapter();

                // This is where we will indicate the bluetooth device that we connected to has been
                // connected to
                Toast.makeText(MainActivity.this, "Connected: " + activeBleDevice.getName(), Toast.LENGTH_LONG).show();

                // This is where we will be looking over all of the services and characteristics
                for (BluetoothGattService bgs: gatt.getServices()) {
                    for (BluetoothGattCharacteristic bgc: bgs.getCharacteristics()) {
                        if (bgc.getUuid().toString().equals("6e400003-b5a3-f393-e0a9-e50e24dcca9e")) {
                            ecgCharacteristic = bgc;
                        }
                    }
                }

                // If the characteristic is not found, we will stop the scanning
                if (ecgCharacteristic == null) {
                    stopScan();
                }

                // Here we will be setting the notify callback which
                // track if our characteristic changes or not
                callBack = new BleNotifyCallback() {

                    /**
                     * This will deal with the case when the characteristic changes
                     * @param data: this is the data of the characteristic
                     */
                    @Override
                    public void onCharacteristicChanged(byte[] data) {

                        // Format the data in its correct form
                        String formattedData = HexUtil.formatHexString(data);
                        int ecg_data_length = (int)(data.length/3);
                        int[] ecg_data_array = new int[ecg_data_length];
                        int i = 0;
                        int ecg_data = 0;
                        for(i=0; i<data.length-2; i=i+3){
                            int data2 = (int) (data[i+2]);
                            data2 = data2 >>6;
                            data2 = data2 & 0x03;
                            ecg_data = data[i]<<24 | (data[i+1] & 0xFF)<<16 | data2;
                            ecg_data_array[i/3] = ecg_data;
                        }


                        // Here we will be setting the value of the necklace to the right value
                        Necklace dataRecievedNecklace = necklaceList.get(0);
                        dataRecievedNecklace.heartRate = "" + ecg_data;

                        // update this on the recyclerView
                        setAdapter();

                        // Here we will toast the value that we got from the necklace
                        Toast.makeText(MainActivity.this, Integer.toString(ecg_data), Toast.LENGTH_SHORT).show();

                        // Here we will be getting the current time
                        LocalDateTime now = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            now = LocalDateTime.now();
                        }

                        DateTimeFormatter formatter = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        }

                        String formattedDateTime = "";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            formattedDateTime = now.format(formatter);
                        }

//                        // Here we will be inserting the data we got into a file
//                        String stringToWrite = formattedDateTime + ", " + Long.toString(ecgdata) + "\n";
//                        try {
//                            //how do i clear a file in android
//                            File f = new File("/storage/emulated/0/henlo/", "ECG_data.txt");
//                            FileOutputStream fos = new FileOutputStream(f, true);
//                            fos.write((stringToWrite).getBytes());
//                        } catch (Exception e) {
//                            // System.out.println(e);
//                            e.printStackTrace();
//                        }

                        // Here we will be inserting the data we got into a file
                        for(i=0; i<10; i++){
                            String stringToWrite = formattedDateTime + ", " + Integer.toString(ecg_data_array[i]) + "\n";
                            try {
                                //how do i clear a file in android
                                File f = new File("/storage/emulated/0/henlo/", "ECG_data.txt");
                                FileOutputStream fos = new FileOutputStream(f, true);
                                fos.write((stringToWrite).getBytes());
                            } catch (Exception e) {
                                // System.out.println(e);
                                e.printStackTrace();
                            }
                        }


                    }

                    /**
                     * This is so that we can declare a success situation in the connection
                     */
                    @Override
                    public void onNotifySuccess() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "notify success", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    /**
                     * This deals with cases when there is an issue in the connection
                     * @param exception: we will return an exception for the case for when there is
                     *                  an issue with the connection
                     */
                    @Override
                    public void onNotifyFailure(final BleException exception) {

                        /**
                         * Here we will notify the user of the issue
                         */
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "notify failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                };

                /**
                 * Here we will set the notify instance of the connection, with the callback above
                 */
                BleManager.getInstance().notify(
                        bleDevice,
                        ecgCharacteristic.getService().getUuid().toString(),
                        ecgCharacteristic.getUuid().toString(),
                        callBack
                );

            }

            /**
             * This is the case when a user disconnects with the device
             * @param isActiveDisConnected: this tells us if we are actively disconnecting
             * @param bleDevice: this is the ble device we want to disconnect from
             * @param gatt: this is the read and write protocol between the two devices
             * @param status: this is the status of the connection
             */
            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {

                // Update the UI to deal with the case that we disconnected
                Button scanButton = findViewById(R.id.start_connection);
                scanButton.setText("SCAN AGAIN");
                // Here we will need to remove the device from the list of Necklaces
                TextView connectingText = findViewById(R.id.connected_devices);
                int redColor = Color.rgb(255, 0, 0);
                connectingText.setTextColor(redColor);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) connectingText.getLayoutParams();
                layoutParams.setMargins(260, 475, layoutParams.rightMargin, layoutParams.bottomMargin);
                connectingText.setLayoutParams(layoutParams);
                connectingText.setText("No connected devices");

                // Here we will be stopping the scan, so that the next time we scan we can scan for
                // new devices
                BleManager.getInstance().cancelScan();

            }
        });
    }

    /**
     * This is the function that will be called if a device is not found
     */
    public void deviceNotFound() {

        // Here we will enable the button again
        buttonEnable();

        // Here we will update the scan button
        Button scanButton = findViewById(R.id.start_connection);
        scanButton.setText("SCAN AGAIN");

        // Here we will say that we are no longer scanning anymore
        isScanning = false;

        // Here we will be making the necklaces that we have empty
        necklaceList = new ArrayList<>();

        // Here we will setAdapter to update the recylcer view with the right information
        setAdapter();

        // Here we will be updating the connection text
        TextView connectingText = findViewById(R.id.connected_devices);
        int redColor = Color.rgb(255, 0, 0);
        connectingText.setTextColor(redColor);
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) connectingText.getLayoutParams();
        layoutParams.setMargins(300, 550, layoutParams.rightMargin, layoutParams.bottomMargin);
        connectingText.setLayoutParams(layoutParams);
        connectingText.setText("Device not Found");

        // Here we will be stopping the connection here, given that we did not connect to anything
        if (ecgCharacteristic != null && activeBleDevice != null) {
            BleManager.getInstance().stopNotify(
                    activeBleDevice,
                    ecgCharacteristic.getService().getUuid().toString(),
                    ecgCharacteristic.getUuid().toString()
            );
        }
    }

    /**
     * This method is needed for the resuming of the activity
     */
    @Override
    protected void onResume() {
        super.onResume();
    }


    /**
     * This is what will happen if the acitivity stops
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    /**
     * This is when the scanning for a device is happening, so we need to disable the button
     */
    private void disableButtonForDelay() {

        // Here we will set the button to unclickable
        Button scanButton = findViewById(R.id.start_connection);
        isButtonClickable = false;
        scanButton.setEnabled(false);
    }

    /**
     * This is when the scanning has stopped, so the button can be enabled again
     */
    private void buttonEnable() {

        // After 2 seconds we will allow users to click on the button again
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Button scanButton = findViewById(R.id.start_connection);
                isButtonClickable = true;
                scanButton.setEnabled(true);
            }
        }, 2000);
    }



    /***************************************************************************************************
     *                   This is the main code for graphing the ECG data                               *
     ***************************************************************************************************/

    public void canGraphECG() throws IOException {
        graphECG();

//        if (activeBleDevice != null) {
//            graphECG();
//        } else {
//            Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_LONG).show();
//        }
    }

    /**
     * This method will graph the ECG graph
     * @throws IOException: This is if we get any issues with reading the file
     */
    public void graphECG() throws IOException {

        // Here we will say reGraph ECG to let the user know that they can regraph
        Button graphButton = findViewById(R.id.ecg_button);
        graphButton.setText("ReGraph ECG");

        // This is where we will be getting the ECG data from
        String fileName = "/storage/emulated/0/henlo/ECG_data.txt";

        // Here we will create a file instance
        File file = new File(fileName);

        // This is the data that we will be graphing
        List<String> contents = new ArrayList<>();

        // Here we will read in the data from the file in which we will graph
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        // read the whole file
        while ((line = bufferedReader.readLine()) != null) {
            contents.add(line);
        }

        // This is where we will be graphing the dots on to the graph
        graphDots(contents);

        // Close resources when complete
        fileReader.close();
        bufferedReader.close();
    }

    /**
     * This allow us to graph the data points of our ECG data
     * @param contents: this is the ECG data that was given
     */
//    public void graphDots(List<String> contents) {
//
//        // Make sure to clear the old graph
//        clearGraph();
//
//        // This is the minTime and the maxTime
//        String minTime = "";
//        String maxTime = "";
//
//        // Here we will get the current time
//        Date currentTime = new Date();
//        SimpleDateFormat currentSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String formattedCurrentDate = currentSDF.format(currentTime);
//        for (int i = 0; i < contents.size(); i++) {
//            String[] information = contents.get(i).split(", ");
//            if (i == 0) {
//                minTime = information[0];
//            } else if (i == contents.size() - 1) {
//                maxTime = information[0];
//            }
//        }
//
//        // Here we will be getting the min time
//        long minTimeNum = 0;
//        String dateTime = minTime; // Replace with your date and tploime string
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        try {
//            Date date = sdf.parse(dateTime);
//            Date currentDate = currentSDF.parse(formattedCurrentDate);
//            minTimeNum = date.getTime() - currentDate.getTime();
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//
//        // Here we will be getting the max time
//        long maxTimeNum = 0;
//        String dateTime2 = maxTime; // Replace with your date and time string
//        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        try {
//            Date date2 = sdf2.parse(dateTime2);
//            Date currentDate = currentSDF.parse(formattedCurrentDate);
//            maxTimeNum = date2.getTime() - currentDate.getTime();
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//
//        // This is the line of the graph that we will be putting onto the graph
//        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
//        boolean first = true;
//        float firstTime = 0;
//
//        // Here we will be tracking the min and the max voltages to know what the graphs
//        // axis bounds should be for the voltage
//        double minVoltage = Double.MAX_VALUE;
//        double maxVoltage = Double.MIN_VALUE;
//        for (int i = 0; i < contents.size(); i++) {
//
//            // Here we will be splitting the information in the file
//            String[] information = contents.get(i).split(", ");
//
//            // Here we will get the current time of the current data point
//            long currentTimeNum = 0;
//            String dateTime3 = information[0]; // Replace with your date and time string
//            SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            try {
//                Date date3 = sdf3.parse(dateTime3);
//
//                Date currentDate = currentSDF.parse(formattedCurrentDate);
//
//                currentTimeNum = date3.getTime() - currentDate.getTime();
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//
//            // This is the voltage of the current data point
//            float y = Integer.parseInt(information[1]);
//
//            // We only want to have data from the past 10 seconds
//            if (maxTimeNum - currentTimeNum < 10000) {
//                if (first) {
//                    firstTime = currentTimeNum;
//                    first = false;
//                }
//
//                // This will help us get the bounds of the current valid data points
//                if (minVoltage > y) {
//                    minVoltage = y;
//                }
//                if (y > maxVoltage) {
//                    maxVoltage = y;
//                }
//
//                // This will be the time value that we will be using
//                float xFormatted = (((currentTimeNum - firstTime)));
//                double seconds = (xFormatted * 1.0) / 1000.0;
//                String formattedSeconds = String.format("%.2f", seconds);
//                double secondsFinal = Double.parseDouble(formattedSeconds);
//
//                // Here we wil be plotting the data point onto the graph
//                series.appendData(new DataPoint(secondsFinal, y), true, contents.size());
//            }
//        }
//
//        // This function will be dealing with the axis of the graph
//        plotAxis(0, 10, minVoltage, maxVoltage);
//
//        // Here we will be adding the line to the graph and formatting it as well
//        ECGGraph.addSeries(series);
//        series.setColor(Color.BLUE);
//        series.setThickness(3);
//        series.setDrawDataPoints(true);
//    }


    public void graphDots(List<String> contents) {

        // Make sure to clear the old graph
        clearGraph();

        // This is the minTime and the maxTime
        String minTime = "";
        String maxTime = "";

        // Here we will get the current time
        Date currentTime = new Date();
        SimpleDateFormat currentSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedCurrentDate = currentSDF.format(currentTime);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

        int sample_rate = 125;   // defined in ecg chip
        int total_seconds = 4;  // we plan to draw 10 seconds of data
        int max_total_points = sample_rate * total_seconds;

        int start_i = 0;
        int end_i = contents.size();
        if (contents.size() > max_total_points){
            start_i = contents.size() - max_total_points;
        }
        if (contents.size() > start_i + max_total_points){
            end_i = start_i + max_total_points;
        }
        System.out.println("start i " + start_i);
        System.out.println("end i " + end_i);

        String[] information0 = contents.get(0).split(", ");
        float voltage0 = Integer.parseInt(information0[1]);
        float minVoltage = voltage0;
        float maxVoltage = voltage0;

        for (int i = start_i; i < end_i; i++) {
            String[] information = contents.get(i).split(", ");
            float y = Integer.parseInt(information[1]);
            if (minVoltage > y) {
                minVoltage = y;
            }
            if (y > maxVoltage) {
                maxVoltage = y;
            }
            double x = ((i - start_i) * total_seconds * 1.0)/(max_total_points*1.0);

            series.appendData(new DataPoint(x, y), true, contents.size());
        }
        System.out.println("minVoltage  " + minVoltage);
        System.out.println("maxVoltage  " + maxVoltage);
        // This function will be dealing with the axis of the graph
        plotAxis(0, total_seconds, minVoltage, maxVoltage);

        // Here we will be adding the line to the graph and formatting it as well
        ECGGraph.addSeries(series);
        series.setColor(Color.BLUE);
        series.setThickness(2);
//        series.setDrawDataPoints(true);
        series.setDrawDataPoints(false);
    }


    /**
     * This will allow us to plot the axis of the graph
     * @param minTime: this is the min of the x axis
     * @param maxTime: max of the x axis
     * @param minVoltage: min of the y axis
     * @param maxVoltage: max of the y axis
     */
    public void plotAxis(long minTime, long maxTime, double minVoltage, double maxVoltage) {
        ECGGraph.getViewport().setXAxisBoundsManual(true);
        ECGGraph.getViewport().setMinX(minTime);
        ECGGraph.getViewport().setMaxX(maxTime);
        ECGGraph.getViewport().setYAxisBoundsManual(true);
        ECGGraph.getViewport().setMinY(minVoltage-10000);
        ECGGraph.getViewport().setMaxY(maxVoltage*1.1);

    }

    /**
     * This method will allow us to clear the graph
     */
    public void clearGraph() {
        ECGGraph.removeAllSeries();
    }



    /***********************************************************************************************
     This is the code for ECG classification
     ***********************************************************************************************/

    /**
     * This will allow us to get the heart rate by calculating it with the python file given
     * @return
     */
    public String getECGClassification() {
        // **************Here you are going to initalize a python instance*********************
        // This is where we will be starting python platform where we will be
        // be able to create a python object in which you will be able to use
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(getApplicationContext()));
        }

        // This is where you will initalize the python instance
        Python py = Python.getInstance();

        // Here you will want to get the python file that you will want to call
        PyObject pyobj = py.getModule("heartCondition_categorization");

        // Now we will be calling the main function that is in the heart_rate file
        PyObject obj = pyobj.callAttr("main");

        // For debugging purposes
        System.out.println(obj.toString());

        return obj.toString();
    }



    /***********************************************************************************************
     This is the portion where we will be collecting the heart rate data
     given some ECG data that we have collected
     ***********************************************************************************************/

    /**
     * This will allow us to get the heart rate by calculating it with the python file given
     * @return
     */
    public double getHeartRateDouble() {
        // **************Here you are going to initalize a python instance*********************
        // This is where we will be starting python platform where we will be
        // be able to create a python object in which you will be able to use
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(getApplicationContext()));
        }

        // This is where you will initalize the python instance
        Python py = Python.getInstance();

        // Here you will want to get the python file that you will want to call
        PyObject pyobj = py.getModule("heart_rate");

        // Now we will be calling the main function that is in the heart_rate file
        PyObject obj = pyobj.callAttr("main");

        // For debugging purposes
        System.out.println(obj.toString());

        return Double.parseDouble(obj.toString());
    }



    /***********************************************************************************************
     Old version of Heart Rate code
     ***********************************************************************************************/

    /**
     * This method will get the heart rate
     * @return heart rate of the individual
     */
    public double getHeartRate() {

        double[] ecgSignal = new double[100];

        // Differentiation
        double[] diffECG = new double[ecgSignal.length - 1];
        for (int i = 0; i < ecgSignal.length - 1; i++) {
            diffECG[i] = ecgSignal[i + 1] - ecgSignal[i];
        }

        // Squaring
        double[] squaredECG = new double[diffECG.length];
        for (int i = 0; i < diffECG.length; i++) {
            squaredECG[i] = diffECG[i] * diffECG[i];
        }

        // Moving Window Integration
        int windowWidth = (int) (180);  // Window width of 80 ms (adjust as needed)
        double[] integralECG = new double[squaredECG.length];
        for (int i = 0; i < squaredECG.length; i++) {
            double sum = 0;
            int windowStart = Math.max(0, i - windowWidth + 1);
            for (int j = windowStart; j <= i; j++) {
                sum += squaredECG[j];
            }
            integralECG[i] = sum / windowWidth;
        }

        // now given the ECG's what we can do is we can  find the
        // peak locations in the ECG graph that is given
        for (int i = 5; i < integralECG.length; i++) {
            double averagePrev = (integralECG[i - 1] + integralECG[i - 2] + integralECG[i - 3] + integralECG[i - 4] + integralECG[i - 5]) / 5;
        }

        return 0.0;
    }


    /**
     * Peak detection algorithm
     * @param ecgData: these are the voltages we have for the ECG data
     * @param samplingFrequency: this is the frequency in which we are sampling
     * @return the peaks in which we found
     */
    public int[] detectPeaks(double[] ecgData, double samplingFrequency) {
        int[] rPeakIndices = new int[ecgData.length];
        int rPeakCount = 0;
        int windowRadius = 1000 / 2;

        for (int i = windowRadius; i < ecgData.length - windowRadius; i++) {
            boolean isPeak = true;
            for (int j = i - windowRadius; j <= i + windowRadius; j++) {
                if (ecgData[j] > ecgData[i]) {
                    isPeak = false;
                    break;
                }
            }
            if (isPeak && ecgData[i] >= 0.5) {
                rPeakIndices[rPeakCount++] = i;
            }
        }

        return Arrays.copyOf(rPeakIndices, rPeakCount);
    }


    /**
     * Here we will be getting the intervals between the R-waves based on the peak data
     * @param peakIndexes: the place where we are getting peaks
     * @param sampleFrequency: the rate at which wwe are sampling
     * @return: the intervals between the peaks
     */
    public double[] calculateIntervals(int[] peakIndexes, double sampleFrequency) {
        double[] intervals = new double[peakIndexes.length - 1];
        for (int i = 0; i < peakIndexes.length; i++) {
            double interval = (peakIndexes[i + 1] - peakIndexes[i]) / sampleFrequency;
            intervals[i] = interval * 1000;
        }

        return intervals;
    }

    /**
     * Given the peak intervals we can calculate the heart rate
     * @param peakIntervals: these are the intervals between the peaks
     * @return the heart rate
     */
    public double calculateHeartRate(double[] peakIntervals) {
        double sum = 0.0;
        for (double interval : peakIntervals) {
            sum += interval;
        }

        double averageInterval = sum / peakIntervals.length;

        return 60.0 / averageInterval;
    }

    /***********************************************************************************************
     These methods are for the recycler view
     ***********************************************************************************************/
    public void setAdapter() {
        recyclerAdapter recyclerAdapter = new recyclerAdapter(necklaceList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerAdapter);
    }

    public void setNecklaces() {
        necklaceList.add(new Necklace("Test Heart Rate", true, "..."));
    }



    /*****************************************************************************************************************************
     *                                 The below is the code for permissions                                                      *
     *****************************************************************************************************************************/
    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    Toast.makeText(getApplicationContext(), "Permissions are granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permissions are granted", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }
}