package systems.lightspeed.love_a_thon_app;

import android.content.Context;
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
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity{

    TextView mText;
    GPS_Service gps;
    Context mContext;
    double partnerLongitude;
    double partnerLatitude;
    double mylongitude;
    double mylatitude;
    String username, partnerName;

    //Firebase Work
    DatabaseReference mDatabaseLocationDetails;
    DatabaseReference partnerLocationDetails;

    private void listenForPartner() {
        partnerLocationDetails = FirebaseDatabase.getInstance().getReference().child("Location_Details").child(partnerName);
        partnerLocationDetails.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("longitude") && dataSnapshot.hasChild("latitude")) {
                    partnerLongitude = dataSnapshot.child("longitude").getValue(Double.class);
                    partnerLatitude = dataSnapshot.child("latitude").getValue(Double.class);
                    System.out.println("distance: "+ distPercentage(mylatitude, mylongitude, partnerLatitude, partnerLongitude));
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
            mDatabaseLocationDetails = FirebaseDatabase.getInstance().getReference().child("Location_Details").child(username);

        }
        mText = (TextView) findViewById(R.id.location_tv);
    }
    @Override
    protected void onStart(){
        super.onStart();
        updateDisplay();
    }


    private void updateDisplay() {
        Timer timer = new Timer();
        gps = new GPS_Service(mContext);
        startService(new Intent(mContext,GPS_Service.class));

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(gps.canGetLocation()) {
                    mylatitude = gps.getLatitude();
                    mylongitude = gps.getLongitude();
                    storeInDatabase(mylatitude, mylongitude);
                    System.out.println("HEREHRE");
                }
                handler.postDelayed(this, 1000);
            }
        }, 1000);

        runtime_permission();

        listenForPartner();
    }

    private double distPercentage(double latitude,  double longitude, double partnerlat, double partnerlong){
        double percentage;
        Location loc = new Location("");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);

        Location partnerloc = new Location("");
        partnerloc.setLatitude(partnerlat);
        partnerloc.setLongitude(partnerlong);

        percentage = loc.distanceTo(partnerloc);

        percentage/=2;

        // prevent divide by zero
        if (percentage == 0.0) {
            return 0.0;
        }

        percentage=1/percentage;
        percentage*=100;
        if(percentage >= 100) percentage = 100;
        percentage = Math.abs(percentage);
        return percentage;
    }

    private void storeInDatabase(double latitude, double longitude) {
        mDatabaseLocationDetails.child("longitude").setValue(longitude);
        mDatabaseLocationDetails.child("latitude").setValue(latitude);
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
}