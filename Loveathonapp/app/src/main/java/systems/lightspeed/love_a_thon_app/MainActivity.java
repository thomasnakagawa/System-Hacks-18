package systems.lightspeed.love_a_thon_app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;



import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import android.os.Handler;


public class MainActivity extends AppCompatActivity implements SensorEventListener{

    TextView mText;
    GPS_Service gps;
    Context mContext;
    double partnerLongitude;
    double partnerLatitude;
    double mylongitude;
    double mylatitude;
    String username, partnerName;
    SensorManager mySensorManager;
    double mypitch, percent;

    //Firebase Work
    DatabaseReference mDatabaseLocationDetails;
    DatabaseReference partnerLocationDetails;

    //Arduino
    Handler handler = new Handler();
    int delay = 5000; //milliseconds
    int counter = 0;
    public final String ACTION_USB_PERMISSION = "systems.lightspeed.love_a_thon_app.USB_PERMISSION";

    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    private void listenForPartner() {
        partnerLocationDetails = FirebaseDatabase.getInstance().getReference().child("Direction_Details").child(partnerName);
        partnerLocationDetails.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("azimuth")) {
                    percent = distPercentage(mypitch, dataSnapshot.child("azimuth").getValue(Double.class));
                    if(percent >= 80.00)
                        System.out.println("facing~~~~~~~~~~~~");
                }else {
                    System.err.println("couldnt get partner location, keys were missing");
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            partnerName = extras.getString("partner");
            username = extras.getString("user");
            mDatabaseLocationDetails = FirebaseDatabase.getInstance().getReference().child("Direction_Details").child(username);

        }
        mText = (TextView) findViewById(R.id.location_tv);
        mySensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor mySensors = mySensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mySensorManager.registerListener(this, mySensors, SensorManager.SENSOR_DELAY_NORMAL);
        listenForPartner();


        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

    }

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    };
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.

                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop();
            }
        }

        ;
    };



    public void onClickStart() {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x1A86)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
        handler.postDelayed(new Runnable(){
            public void run(){
                System.out.println("percent : " + percent + " Counter: "+ counter);
                if (percent > 0 && percent < 80){
                    if(counter >= 3){
                        counter = 3;
                    }else {
                        counter++;
                    }

                }else{
                    if (counter <= 0){
                        counter = 0;
                    }else {
                        counter--;
                    }

                }

                if(counter == 0){
                    onClickSend("1");
                }else if(counter == 1){
                    onClickSend("2");
                }else if (counter == 2){
                    onClickSend("3");
                }

                handler.postDelayed(this, delay);
            }
        }, delay);


    }

    public void onClickSend(String data) {
        String string = data;
        serialPort.write(string.getBytes());

    }


    public void onClickStop() {
        serialPort.close();
    }

    double distance(double srcLat, double srcLng, double desLat, double desLng) {
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(desLat - srcLat);
        double dLng = Math.toRadians(desLng - srcLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(srcLat))
                * Math.cos(Math.toRadians(desLat)) * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;

        double meterConversion = 1609;

        return (dist * meterConversion);
    }

    private double distPercentage(double a1, double a2){
        double percentage;
        percentage = (Math.abs(a1 - a2)/180) * 100;
        return percentage;
    }

    private void storeInDatabase(double azimuth) {
        mDatabaseLocationDetails.child("azimuth").setValue(azimuth);
    }

    private boolean runtime_permission() {
        if(Build.VERSION.SDK_INT>=23 && ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED&& ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},123);
            return true;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==123){
            runtime_permission();

        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //device heading in degrees
        double x = sensorEvent.values[0];
        double y = sensorEvent.values[1];
        double z = sensorEvent.values[2];

        //convert radians into degrees
        double pitch = x;
        storeInDatabase(pitch);
        mypitch = pitch;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}