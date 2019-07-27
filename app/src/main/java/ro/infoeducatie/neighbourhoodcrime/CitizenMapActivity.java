package ro.infoeducatie.neighbourhoodcrime;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CitizenMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout, mRequest, mSettings, mEmail, mChat;

    private LatLng requestLocation;

    private Boolean requestBol = false;

    private SupportMapFragment mapFragment;

    private Marker requestMarker;

    private RadioGroup mRadioGroup;

    private String requestService;

    public EditText mDescription;

    private FirebaseAuth mAuth;

    ImageView imgExpandable;
    BottomSheetFragmentCitizen mBottomSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_map);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        GoogleMapOptions options = new GoogleMapOptions().compassEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CitizenMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            mapFragment.getMapAsync(this);
        }

        mSettings = (Button) findViewById(R.id.settings);
        mEmail = (Button) findViewById(R.id.email);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.police);

        mLogout = (Button) findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CitizenMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mAuth = FirebaseAuth.getInstance();

        mDescription = (EditText) findViewById(R.id.description);


        mRequest = (Button) findViewById(R.id.request);
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestBol) {
                    requestBol = false;
                    String user_id = mAuth.getCurrentUser().getUid();
                    DatabaseReference issueRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Citizens").child(user_id).child("issue");
                    issueRef.removeValue();

                    if(lawenforcerFoundID != null) {
                        DatabaseReference lawenforcerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Lawenforcers").child(lawenforcerFoundID).child("citizenRequest");
                        lawenforcerRef.removeValue();
                        lawenforcerFoundID = null;
                    }
                    lawenforcerFound = false;
                    radius=1;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("citizenRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);

                    if(requestMarker != null) {
                        requestMarker.remove();
                    }
                    mRequest.setText("Apeleaza autoritatile");

                } else {
                    showDialogDescriptionIssue();

                    int selectId = mRadioGroup.getCheckedRadioButtonId();

                    final RadioButton radioButton = (RadioButton) findViewById(selectId);

                    if(radioButton.getText() == null) {
                        return;
                    }

                    requestService = radioButton.getText().toString();

                    requestBol = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("citizenRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    requestLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    requestMarker = mMap.addMarker(new MarkerOptions().position(requestLocation).title("Problema raportata aici"));

                    mRequest.setText("Cautam echipaje...");

                    /*getClosestLawenforcer();*/
                }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CitizenMapActivity.this, CitizenSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });
        mEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CitizenMapActivity.this, EmailActivity.class);
                startActivity(intent);
                return;
            }
        });

        mChat = findViewById(R.id.chat);
        mChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CitizenMapActivity.this, MainPageActivity.class);
                startActivity(intent);
                return;
            }
        });

        //View
        imgExpandable = (ImageView) findViewById(R.id.imgExpandable);
        mBottomSheet = BottomSheetFragmentCitizen.newInstance("Citizen bottom sheet");
        imgExpandable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());
            }
        });
    }

    /*public void onButtonShowPopupWindowClick(View view) {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_window, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }*/

    private void showDialogDescriptionIssue() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(CitizenMapActivity.this);
        alertDialog.setTitle("Descriere");
        alertDialog.setMessage("Descrie problema:");

        LayoutInflater inflater = LayoutInflater.from(CitizenMapActivity.this);
        View description_issue_layout = inflater.inflate(R.layout.layout_description_issue, null);

        final EditText mDescription = (EditText) description_issue_layout.findViewById(R.id.description);
        alertDialog.setView(description_issue_layout);

        alertDialog.setPositiveButton("ADAUGA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                String user_id = mAuth.getCurrentUser().getUid();
                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Citizens").child(user_id).child("issue");

                final String description = mDescription.getText().toString();

                Map newPost = new HashMap();
                newPost.put("description", description);

                current_user_db.setValue(newPost);
                getClosestLawenforcer();
                dialog.dismiss();
            }
        });

        alertDialog.setNegativeButton("INAPOI", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestBol = false;
                String user_id = mAuth.getCurrentUser().getUid();
                DatabaseReference issueRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Citizens").child(user_id).child("issue").child("description");
                issueRef.removeValue();

                if(lawenforcerFoundID != null) {
                    DatabaseReference lawenforcerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Lawenforcers").child(lawenforcerFoundID).child("citizenRequest");
                    lawenforcerRef.removeValue();
                    lawenforcerFoundID = null;
                }
                lawenforcerFound = false;
                radius=1;

                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("citizenRequest");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.removeLocation(userId);

                if(requestMarker != null) {
                    requestMarker.remove();
                }
                mRequest.setText("Call Authorities");
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private int radius = 1;
    private Boolean lawenforcerFound = false;
    private String lawenforcerFoundID;
    GeoQuery geoQuery;
    private void getClosestLawenforcer() {
        final DatabaseReference lawenforcerLocation = FirebaseDatabase.getInstance().getReference().child("lawenforcersAvailable");

        GeoFire geoFire = new GeoFire(lawenforcerLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(requestLocation.latitude, requestLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!lawenforcerFound && requestBol) {
                    DatabaseReference mCitizenDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Lawenforcers").child(key);
                    mCitizenDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                Map<String, Object> lawenforcerMap = (Map<String, Object>) dataSnapshot.getValue();
                                if(lawenforcerFound) {
                                    return;
                                }
                                if(lawenforcerMap.get("service").equals(requestService)) {
                                    lawenforcerFound = true;
                                    lawenforcerFoundID = dataSnapshot.getKey();

                                    DatabaseReference lawenforcerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Lawenforcers").child(lawenforcerFoundID);
                                    String citizenId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("citizenRequest", citizenId);
                                    lawenforcerRef.updateChildren(map);

                                    getLawenforcerLocation();
                                    mRequest.setText("Looking for Authority location...");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!lawenforcerFound) {
                    radius++;
                    getClosestLawenforcer();
                }
            }
            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private Marker mLawenforcerMarker;
    private DatabaseReference lawenforcerLocationRef;
    private ValueEventListener lawenforcerLocationRefListener;
    private void getLawenforcerLocation() {
        lawenforcerLocationRef = FirebaseDatabase.getInstance().getReference().child("lawenforcersWorking").child(lawenforcerFoundID).child("l");
        lawenforcerLocationRefListener = lawenforcerLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Law enforcer found");
                    if(map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng lawenforcerLatLng = new LatLng(locationLat, locationLng);
                    if(mLawenforcerMarker != null) {
                        mLawenforcerMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(requestLocation.latitude);
                    loc1.setLongitude(requestLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(lawenforcerLatLng.latitude);
                    loc2.setLongitude(lawenforcerLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if(distance < 100) {
                        mRequest.setText("Echipajul a ajuns la locatie");
                    } else {
                        mRequest.setText("Echipaj gasit " + String.valueOf(distance) + " m");
                    }

                    //mLawenforcerMarker = mMap.addMarker(new MarkerOptions().position(lawenforcerLatLng).title("picked authority"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle));
            if (!success) {
                Log.e("MapActivity", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapActivity", "Can't find style. Error: ", e);
        }

        mMap = googleMap;

        mMap.setPadding(0,20,0,350);
        mMap.getUiSettings().isCompassEnabled();
        mMap.getUiSettings().setMapToolbarEnabled(true);

        LatLng bucharest = new LatLng(44.431802, 26.102680);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bucharest, 11));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CitizenMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized  void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        /*LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 13);
        mMap.animateCamera(cameraUpdate);*/
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    final int LOCATION_REQUEST_CODE = 1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                } else {
                    Toast.makeText(getApplicationContext(), "Va rugam acceptati permisiunea", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }
}
