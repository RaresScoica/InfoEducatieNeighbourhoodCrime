package ro.infoeducatie.neighbourhoodcrime;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LawenforcerSignupActivity extends AppCompatActivity {

    private EditText mEmail, mPassword, mName, mId;

    private Button mLogin, mRegistration, mEmailBtn;

    private FirebaseAuth mAuth;

    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    private RadioGroup mRadioGroup;

    private String mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lawenforcer_signup);

        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    Intent intent = new Intent(LawenforcerSignupActivity.this, LawenforcerMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);
        mName = (EditText) findViewById(R.id.name);
        mId = (EditText) findViewById(R.id.id);

        mLogin = (Button) findViewById(R.id.login);
        mRegistration = (Button) findViewById(R.id.registration);
        mEmailBtn = (Button) findViewById(R.id.email_btn);

        mEmail.addTextChangedListener(loginTextWatcher);
        mPassword.addTextChangedListener(loginTextWatcher);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(LawenforcerSignupActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(LawenforcerSignupActivity.this, "sign up error", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            String user_id = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Lawenforcers").child(user_id);

                            final String name = mName.getText().toString();
                            final String id = mId.getText().toString();
                            int selectId = mRadioGroup.getCheckedRadioButtonId();

                            final RadioButton radioButton = (RadioButton) findViewById(selectId);

                            if(radioButton.getText() == null) {
                                return;
                            }
                            mService = radioButton.getText().toString();

                            Map newPost = new HashMap();
                            newPost.put("name", name);
                            newPost.put("id", id);
                            newPost.put("service", mService);
                            newPost.put("accountType", "lawenforcer");

                            current_user_db.setValue(newPost);
                        }
                    }
                });
            }
        });

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LawenforcerSignupActivity.this, LawenforcerLoginActivity.class);
                startActivity(intent);
                return;
            }
        });

        mEmailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LawenforcerSignupActivity.this, EmailUsActivity.class);
                startActivity(intent);
                return;
            }
        });
    }

    private TextWatcher loginTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String usernameInput = mEmail.getText().toString().trim();
            String passwordInput = mPassword.getText().toString().trim();

            mRegistration.setEnabled(!usernameInput.isEmpty() && !passwordInput.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}