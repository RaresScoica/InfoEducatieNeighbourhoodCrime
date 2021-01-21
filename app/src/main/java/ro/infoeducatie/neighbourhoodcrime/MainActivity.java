package ro.infoeducatie.neighbourhoodcrime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button mCitizen, mLawenforcer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCitizen = findViewById(R.id.citizen);
        mLawenforcer = findViewById(R.id.lawenforcer);

        startService(new Intent(MainActivity.this, onAppKilled.class));

        mCitizen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CitizenLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mLawenforcer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LawenforcerLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }
}
