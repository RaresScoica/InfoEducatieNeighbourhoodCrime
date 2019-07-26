package ro.infoeducatie.neighbourhoodcrime;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rengwuxian.materialedittext.MaterialEditText;

public class CitizenLoginActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;

    private Button mLogin, mRegistration, mEmailBtn;

    private FirebaseAuth mAuth;

    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    private TextView mForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_login);

        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    Intent intent = new Intent(CitizenLoginActivity.this, CitizenMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);

        mLogin = (Button) findViewById(R.id.login);
        mRegistration = (Button) findViewById(R.id.registration);
        mEmailBtn = (Button) findViewById(R.id.email_btn);

        mEmail.addTextChangedListener(loginTextWatcher);
        mPassword.addTextChangedListener(loginTextWatcher);

        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CitizenLoginActivity.this, CitizenSignupActivity.class);
                startActivity(intent);
                return;
            }
        });

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();

                if(password.length() < 6) {
                    Toast.makeText(CitizenLoginActivity.this, "Parola trebuie sa contina minimum 6 caractere", Toast.LENGTH_LONG).show();
                }

                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(CitizenLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(CitizenLoginActivity.this, "Eroare la conectare", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        mEmailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CitizenLoginActivity.this, EmailUsActivity.class);
                startActivity(intent);
                return;
            }
        });

        mForgotPassword = (TextView) findViewById(R.id.forgot_password);
        mForgotPassword.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                showDialogForgotPassword();
                return false;
            }
        });
    }

    private void showDialogForgotPassword() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(CitizenLoginActivity.this);
        alertDialog.setTitle("Resetare");
        alertDialog.setMessage("Introdu adresa de email");

        LayoutInflater inflater = LayoutInflater.from(CitizenLoginActivity.this);
        View forgot_password_layout = inflater.inflate(R.layout.layout_forgot_password, null);

        final MaterialEditText edtEmail = (MaterialEditText) forgot_password_layout.findViewById(R.id.edtEmail);
        alertDialog.setView(forgot_password_layout);

        alertDialog.setPositiveButton("RESET", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
               mAuth.sendPasswordResetEmail(edtEmail.getText().toString().trim())
                       .addOnCompleteListener(new OnCompleteListener<Void>() {
                           @Override
                           public void onComplete(@NonNull Task<Void> task) {
                               dialog.dismiss();
                               Toast.makeText(CitizenLoginActivity.this, "Emailul pentru resetarea parolei a fost trimis", Toast.LENGTH_LONG).show();
                           }
                       }).addOnFailureListener(new OnFailureListener() {
                   @Override
                   public void onFailure(@NonNull Exception e) {
                       Toast.makeText(CitizenLoginActivity.this, ""+e.getMessage(), Toast.LENGTH_LONG).show();
                   }
               });
            }
        });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private TextWatcher loginTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String usernameInput = mEmail.getText().toString().trim();
            String passwordInput = mPassword.getText().toString().trim();

            mLogin.setEnabled(!usernameInput.isEmpty() && !passwordInput.isEmpty());
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
