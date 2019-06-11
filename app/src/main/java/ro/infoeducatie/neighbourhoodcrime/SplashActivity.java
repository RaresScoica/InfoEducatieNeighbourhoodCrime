package ro.infoeducatie.neighbourhoodcrime;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    private DatabaseReference mRefDatatbase;

    private FirebaseAuth mAuth;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        startService(new Intent(SplashActivity.this, onAppKilled.class));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        mAuth = FirebaseAuth.getInstance();
        if (user != null) {
            userId = mAuth.getCurrentUser().getUid();

            mRefDatatbase = FirebaseDatabase.getInstance().getReference();
            mRefDatatbase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child("Users").child("Citizens").hasChild(userId)) {
                        Intent intent = new Intent(SplashActivity.this, CitizenMapActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = new Intent(SplashActivity.this, LawenforcerMapActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            Thread welcomeThread = new Thread() {

                @Override
                public void run() {
                    try {
                        super.run();
                        sleep(1500);
                        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    } catch (Exception e) {

                    } finally {

                        Intent i = new Intent(SplashActivity.this,
                                MainActivity.class);
                        startActivity(i);
                        finish();
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                }
            };
            welcomeThread.start();
        }
    }
}
