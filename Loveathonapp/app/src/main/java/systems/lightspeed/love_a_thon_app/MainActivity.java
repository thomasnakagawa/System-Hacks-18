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