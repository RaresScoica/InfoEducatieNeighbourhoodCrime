package ro.infoeducatie.neighbourhoodcrime;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LawenforcerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, RoutingListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout, mSettings, mStatus, mEmail;

    private Switch mWorkingSwitch;

    private String citizenId = "", description;

    private Boolean isLoggingOut = false;

    private SupportMapFragment mapFragment;

    private LinearLayout mCitizenInfo, mFinish, mDescriptionInfo;

    private ImageView mCitizenProfileImage;

    private TextView mCitizenName, mCitizenPhone, mDescriptionBox;

    private LatLng lawenforcerLatLng;

    private FirebaseAuth mAuth;

    private NotificationManagerCompat notificationManager;

    ImageView imgExpandable;
    BottomSheetFragmentLawenforcer mBottomSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lawenforcer_map);
        polylines = new ArrayList<>();

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mAuth = FirebaseAuth.getInstance();

        GoogleMapOptions options = new GoogleMapOptions().compassEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LawenforcerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            mapFragment.getMapAsync(this);
        }

        mCitizenInfo = (LinearLayout) findViewById(R.id.citizenInfo);
        mFinish = (LinearLayout) findViewById(R.id.finish);
        mDescriptionInfo = (LinearLayout) findViewById(R.id.description_info);

        mCitizenProfileImage = (ImageView) findViewById(R.id.citizenProfileImage);

        mWorkingSwitch = (Switch) findViewById(R.id.workingSwitch);
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    connectLawenforcer();
                } else {
                    disconnectLawenforcer();
                }
            }
        });

        mCitizenName = (TextView) findViewById(R.id.citizenName);
        mCitizenPhone = (TextView) findViewById(R.id.citizenPhone);
        mDescriptionBox = (TextView) findViewById(R.id.description_box);

        notificationManager = NotificationManagerCompat.from(this);

        mEmail = (Button) findViewById(R.id.email);
        mEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LawenforcerMapActivity.this, EmailActivity.class);
                startActivity(intent);
                return;
            }
        });

        mSettings = (Button) findViewById(R.id.settings);
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LawenforcerMapActivity.this, LawenforcerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        mLogout = (Button) findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;
                disconnectLawenforcer();

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(LawenforcerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mStatus = (Button) findViewById(R.id.status);
        mStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endRequest();
            }
        });

        getAssignedCitizen();

        //View
        imgExpandable = (ImageView) findViewById(R.id.imgExpandable);
        mBottomSheet = BottomSheetFragmentLawenforcer.newInstance("Lawenforcer bottom sheet");
        imgExpandable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());
            }
        });
    }

    private void getAssignedCitizen() {
        String lawenforcerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCitizenRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Lawenforcers").child(lawenforcerId).child("citizenRequest");
        assignedCitizenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    citizenId = dataSnapshot.getValue().toString();
                    getAssignedCitizenPickupLocation();
                    getAssignedCitizenInfo();
                } else {
                    endRequest();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    Marker requestMarker;
    private DatabaseReference assignedCitizenPickupLocationRef;
    private ValueEventListener assignedCitizenPickupLocationRefListener;
    private void getAssignedCitizenPickupLocation() {
        assignedCitizenPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("citizenRequest").child(citizenId).child("l");
        assignedCitizenPickupLocationRefListener = assignedCitizenPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !citizenId.equals("")) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if(map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng requestLatLng = new LatLng(locationLat, locationLng);
                    requestMarker = mMap.addMarker(new MarkerOptions().position(requestLatLng).title("Locatia incidentului"));
                    getRouteToMarker(requestLatLng);

                    Location loc1 = new Location("");
                    loc1.setLatitude(requestLatLng.latitude);
                    loc1.setLongitude(requestLatLng.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(lawenforcerLatLng.latitude);
                    loc2.setLongitude(lawenforcerLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if(distance < 100) {
                        mFinish.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getRouteToMarker(LatLng requestLatLng) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), requestLatLng)
                .key("AIzaSyC33XalX5Pb7QbdEJxl0spmzPb4aQB-f8k")
                .build();
        routing.execute();
    }

    private void getAssignedCitizenInfo() {
        mDescriptionInfo.setVisibility(View.VISIBLE);
        mCitizenInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCitizenDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Citizens").child(citizenId);
        mCitizenDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null) {
                        mCitizenName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") != null) {
                        mCitizenPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl") != null) {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).apply(RequestOptions.circleCropTransform()).into(mCitizenProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        DatabaseReference issueRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Citizens").child(citizenId).child("issue");
        issueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map2 = (Map<String, Object>) dataSnapshot.getValue();
                    if (map2.get("description") != null) {
                        mDescriptionBox.setText(map2.get("description").toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void endRequest() {
        mStatus.setText("Finalizeaza");
        erasePolyLines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference lawenforcerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Lawenforcers").child(userId).child("citizenRequest");
        lawenforcerRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("citizenRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(citizenId);
        citizenId="";

        if(requestMarker != null) {
            requestMarker.remove();
        }
        if(assignedCitizenPickupLocationRefListener != null) {
            assignedCitizenPickupLocationRef.removeEventListener(assignedCitizenPickupLocationRefListener);
        }
        mCitizenInfo.setVisibility(View.GONE);
        mDescriptionInfo.setVisibility(View.GONE);
        mFinish.setVisibility(View.GONE);
        mCitizenName.setText("");
        mCitizenPhone.setText("");
        mCitizenProfileImage.setImageResource(R.mipmap.ic_logo);
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

        mMap.setPadding(0,20,0,450);
        mMap.getUiSettings().isCompassEnabled();
        mMap.getUiSettings().setMapToolbarEnabled(true);

        LatLng bucharest = new LatLng(44.431802, 26.102680);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bucharest, 11));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
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
        /*LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 13);
        mMap.animateCamera(cameraUpdate);*/

        if(getApplicationContext()!=null){
            mLastLocation = location;

            lawenforcerLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("lawenforcersAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("lawenforcersWorking");
            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);

            switch(citizenId) {
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

                default:
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void connectLawenforcer() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LawenforcerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void disconnectLawenforcer() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("lawenforcersAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
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

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        for (int i = 0; i <route.size(); i++) {

            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Ruta "+ (i+1) +": distanta - " + route.get(i).getDistanceValue() + "m, durata - "+ route.get(i).getDurationValue() + "s",Toast.LENGTH_LONG).show();
            requestMarker.setTitle("Ruta "+ (i+1) +": distanta - " + route.get(i).getDistanceValue() + "m, durata - "+ route.get(i).getDurationValue() + "s");
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void erasePolyLines() {
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }
}
