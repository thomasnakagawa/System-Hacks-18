package systems.lightspeed.love_a_thon_app;

import android.location.Location;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    TextView mText;
    GPS_Service gps;

    double partnerLongitude;
    double partnerLatitude;
    double mylongitude;
    double mylatitude;

    //Firebase Work
    DatabaseReference mDatabaseLocationDetails;
    DatabaseReference partnerLocationDetails;

    private void listenForPartner(String partnerName) {
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

        mText = (TextView) findViewById(R.id.location_tv);
        mDatabaseLocationDetails = FirebaseDatabase.getInstance().getReference().child("Location_Details").child("User 1");

        runtime_permission();

        listenForPartner("MyPartner");
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