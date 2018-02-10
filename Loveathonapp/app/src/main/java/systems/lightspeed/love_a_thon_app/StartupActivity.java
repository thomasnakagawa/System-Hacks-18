package systems.lightspeed.love_a_thon_app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.FirebaseDatabase;

public class StartupActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        Button button = (Button) findViewById(R.id.start_button);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String userName = ((EditText)findViewById(R.id.userName_textEdit)).getText().toString();
        String partnerName = ((EditText)findViewById(R.id.partnerName_textEdit)).getText().toString();
        System.out.println(userName + partnerName);
    }
}
