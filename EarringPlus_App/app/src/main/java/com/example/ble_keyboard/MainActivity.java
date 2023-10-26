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
    public GraphView PPGGraph;
    public GraphView AcclGraph;

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

    public BluetoothGattCharacteristic ppgCharacteristic;

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

        // ****************Displaying nessecary graph components*****************************
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // This button will be the button that you can press to draw the PPG graph
        Button graph_PPG = findViewById(R.id.ppg_button);
        // This will set the PPGGraph where you can put dots onto
        PPGGraph = findViewById(R.id.graph);

        // This is where you will set the titles of the graph
        PPGGraph.setTitle("PPG Graph");
        PPGGraph.setTitleTextSize(70);

        // This is where you will set the legend of the graph
        PPGGraph.getLegendRenderer().setVisible(false);

        // This is where you will be setting the zooming and scrolling feature of the graph
        PPGGraph.getViewport().setScalable(true);
        PPGGraph.getViewport().setScalableY(true);


        // This button will be the button that you can press to draw the graph
        Button graph_Accl = findViewById(R.id.accl_button);
        AcclGraph = findViewById(R.id.graph2);
        AcclGraph.setTitle("Accelerometer Graph");
        AcclGraph.setTitleTextSize(70);
        AcclGraph.getLegendRenderer().setVisible(false);
        AcclGraph.getViewport().setScalable(true);
        AcclGraph.getViewport().setScalableY(true);


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

        // This is where you will be initalizing the listener for the graph PPG button
        graph_PPG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("I clicked on the graph button!");
                try {
                    canGraphPPG();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        graph_Accl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("I clicked on the graph accl button!");
                try {
                    canGraphAccl();
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
        PPGGraph.removeAllSeries();

        // This is where we will be setting the Graph PPG button text
        Button graphButton = findViewById(R.id.ppg_button);
        graphButton.setText("Graph PPG");

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
        if (ppgCharacteristic != null && activeBleDevice != null) {
            BleManager.getInstance().stopNotify(
                    activeBleDevice,
                    ppgCharacteristic.getService().getUuid().toString(),
                    ppgCharacteristic.getUuid().toString()
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
                        if (bgc.getUuid().toString().equals("e9ea0002-e19b-482d-9293-c7907585fc48")) {
                            ppgCharacteristic = bgc;
                        }
                    }
                }

                // If the characteristic is not found, we will stop the scanning
                if (ppgCharacteristic == null) {
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
                        int data_length = 238;   // 34 * 7 bytes
                        int ppg_data_length = 34;
                        int[] ppg_data_array = new int[ppg_data_length];
                        int accl_data_length = 34;
                        float[] accl_x_data_array = new float[accl_data_length];
                        float[] accl_y_data_array = new float[accl_data_length];
                        float[] accl_z_data_array = new float[accl_data_length];
                        int ppg_data = 0;
                        int accl_x = 0;
                        int accl_y = 0;
                        int accl_z = 0;
                        float x=0;
                        float y=0;
                        float z=0;

                        int i=0;
                        for(i=0; i<data_length; i=i+7){
                            // convert accl bytes to float
                            accl_x = ((data[i+1]&0xFF) << 8) | (data[i]&0xFF);
                            accl_y = ((data[i+3]&0xFF) << 8) | (data[i+2]&0xFF);
                            accl_z = ((data[i+5]&0xFF) << 8) | (data[i+4]&0xFF);
                            if ((accl_x & 0x8000) != 0) {
                                accl_x = -((int)(0xFFFF - accl_x + 1));
                            }
                            if ((accl_y & 0x8000) != 0) {
                                accl_y = -((int)(0xFFFF - accl_y + 1));
                            }
                            if ((accl_z & 0x8000) != 0) {
                                accl_z = -((int)(0xFFFF - accl_z + 1));
                            }
                            accl_x = accl_x >> 2;
                            accl_y = accl_y >> 2;
                            accl_z = accl_z >> 2;
                            x = accl_x * 0.244f * 9.8f/1000.0f; // convert to mg unit then convert to m/s^2 unit. 0.244 is from datasheet
                            y = accl_y * 0.244f * 9.8f/1000.0f;
                            z = accl_z * 0.244f * 9.8f/1000.0f;
                            accl_x_data_array[(i)/7] = x;
                            accl_y_data_array[(i)/7] = y;
                            accl_z_data_array[(i)/7] = z;
                            ppg_data = data[i+6];
                            ppg_data_array[i/7] = ppg_data;
                        }

                        // Here we will be setting the value of the necklace to the right value
//                        Necklace dataRecievedNecklace = necklaceList.get(0);
//                        dataRecievedNecklace.heartRate = "" + ppg_data;

                        // update this on the recyclerView
                        setAdapter();

                        // Here we will toast the value that we got from the necklace
                        Toast.makeText(MainActivity.this, Integer.toString(ppg_data), Toast.LENGTH_SHORT).show();

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


                        // Here we will be inserting the data we got into a file
                        for(i=0; i<ppg_data_length; i++){
                            String stringToWrite = formattedDateTime + ", " + Integer.toString(ppg_data_array[i]) + "\n";
                            try {
                                File f = new File("/storage/emulated/0/EarringPlus/", "PPG_data.txt");
                                FileOutputStream fos = new FileOutputStream(f, true);
                                fos.write((stringToWrite).getBytes());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        for(i=0; i<accl_data_length; i++){
                            String stringToWrite = formattedDateTime + ", " + Float.toString(accl_x_data_array[i]) + "\n";
                            try {
                                File f = new File("/storage/emulated/0/EarringPlus/", "Accelerometer_x.txt");
                                FileOutputStream fos = new FileOutputStream(f, true);
                                fos.write((stringToWrite).getBytes());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        for(i=0; i<accl_data_length; i++){
                            String stringToWrite = formattedDateTime + ", " + Float.toString(accl_y_data_array[i]) + "\n";
                            try {
                                File f = new File("/storage/emulated/0/EarringPlus/", "Accelerometer_y.txt");
                                FileOutputStream fos = new FileOutputStream(f, true);
                                fos.write((stringToWrite).getBytes());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        for(i=0; i<accl_data_length; i++){
                            String stringToWrite = formattedDateTime + ", " + Float.toString(accl_z_data_array[i]) + "\n";
                            try {
                                File f = new File("/storage/emulated/0/EarringPlus/", "Accelerometer_z.txt");
                                FileOutputStream fos = new FileOutputStream(f, true);
                                fos.write((stringToWrite).getBytes());
                            } catch (Exception e) {
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
                        ppgCharacteristic.getService().getUuid().toString(),
                        ppgCharacteristic.getUuid().toString(),
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
        if (ppgCharacteristic != null && activeBleDevice != null) {
            BleManager.getInstance().stopNotify(
                    activeBleDevice,
                    ppgCharacteristic.getService().getUuid().toString(),
                    ppgCharacteristic.getUuid().toString()
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
     *                   This is the main code for graphing the PPG data                               *
     ***************************************************************************************************/

    public void canGraphPPG() throws IOException {
        graphPPG();
//        if (activeBleDevice != null) {
//            graphECG();
//        } else {
//            Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_LONG).show();
//        }
    }

    /**
     * This method will graph the PPG graph
     * @throws IOException: This is if we get any issues with reading the file
     */
    public void graphPPG() throws IOException {

        // Here we will say reGraph PPG to let the user know that they can regraph
        Button graphButton = findViewById(R.id.ppg_button);
        graphButton.setText("ReGraph PPG");

        // This is where we will be getting the PPG data from
        String fileName = "/storage/emulated/0/EarringPlus/PPG_data.txt";

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
        graph_PPG_Dots(contents);

        // Close resources when complete
        fileReader.close();
        bufferedReader.close();
    }


    public void graph_PPG_Dots(List<String> contents) {

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

        int sample_rate = 50;   // defined in ppg chip
        int total_seconds = 8;  // we plan to draw 10 seconds of data
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
        PPGGraph.addSeries(series);
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
        PPGGraph.getViewport().setXAxisBoundsManual(true);
        PPGGraph.getViewport().setMinX(minTime);
        PPGGraph.getViewport().setMaxX(maxTime);
        PPGGraph.getViewport().setYAxisBoundsManual(true);
        PPGGraph.getViewport().setMinY(minVoltage-10000);
        PPGGraph.getViewport().setMaxY(maxVoltage*1.1);

    }

    /**
     * This method will allow us to clear the graph
     */
    public void clearGraph() {
        PPGGraph.removeAllSeries();
    }



    /***************************************************************************************************
     *                   This is the main code for graphing the data                               *
     ***************************************************************************************************/

    public void canGraphAccl() throws IOException {
        graphAccl();
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
    public void graphAccl() throws IOException {

        Button graphButton = findViewById(R.id.accl_button);
        graphButton.setText("ReGraph Accelerometer");

        String fileName = "/storage/emulated/0/EarringPlus/Accelerometer_y.txt";   // just plot x axis for now

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
        graph_Accl_Dots(contents);

        // Close resources when complete
        fileReader.close();
        bufferedReader.close();
    }


    public void graph_Accl_Dots(List<String> contents) {

        // Make sure to clear the old graph
        clearAcclGraph();

        // This is the minTime and the maxTime
        String minTime = "";
        String maxTime = "";

        // Here we will get the current time
        Date currentTime = new Date();
        SimpleDateFormat currentSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedCurrentDate = currentSDF.format(currentTime);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

        int sample_rate = 50;   // defined in Accl chip
        int total_seconds = 10;  // we plan to draw 5 seconds of data
        int max_total_points = sample_rate * total_seconds;

        int start_i = 0;
        int end_i = contents.size();
        if (contents.size() > max_total_points){
            start_i = contents.size() - max_total_points;
        }
        if (contents.size() > start_i + max_total_points){
            end_i = start_i + max_total_points;
        }


        String[] information0 = contents.get(0).split(", ");
        float accl0 = Integer.parseInt(information0[1]);
        float min_y = accl0;
        float max_y = accl0;

        for (int i = start_i; i < end_i; i++) {
            String[] information = contents.get(i).split(", ");
            float y = Float.parseFloat(information[1]);
            if (min_y > y) {
                min_y = y;
            }
            if (y > max_y) {
                max_y = y;
            }
            double x = ((i - start_i) * total_seconds * 1.0)/(max_total_points*1.0);

            series.appendData(new DataPoint(x, y), true, contents.size());
        }
        // This function will be dealing with the axis of the graph
        plotAcclAxis(0, total_seconds, min_y, max_y);

        // Here we will be adding the line to the graph and formatting it as well
        AcclGraph.addSeries(series);
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
    public void plotAcclAxis(long minTime, long maxTime, double min_y, double max_y) {
        AcclGraph.getViewport().setXAxisBoundsManual(true);
        AcclGraph.getViewport().setMinX(minTime);
        AcclGraph.getViewport().setMaxX(maxTime);
        AcclGraph.getViewport().setYAxisBoundsManual(true);
        AcclGraph.getViewport().setMinY(min_y);
        AcclGraph.getViewport().setMaxY(max_y*1.1);
    }
    /**
     * This method will allow us to clear the graph
     */
    public void clearAcclGraph() {
        AcclGraph.removeAllSeries();
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