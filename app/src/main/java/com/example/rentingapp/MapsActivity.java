package com.example.rentingapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.example.rentingapp.adapters.MapsListingsAdapter;
import com.example.rentingapp.models.Listing;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;
import com.parse.ParseGeoPoint;

import org.parceler.Parcels;

import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener, OnMapReadyCallback {
    public static final String TAG = "MapsActivity";

    private GoogleMap map;

    List<Listing> listings;
    MapsListingsAdapter adapter;

    RecyclerView rvMapListings;
    SnapHelper snapHelper;
    LinearLayoutManager layoutManager;
    Marker prevHighlightedMarker;

    private HashMap<String, Marker> listingMarkerHM = new HashMap<>();
    private HashMap<Marker, Listing> MarkerListingHM = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        listings = Parcels.unwrap(getIntent().getParcelableExtra("listings"));

        rvMapListings = findViewById(R.id.rvMapListings);
        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvMapListings);

        adapter = new MapsListingsAdapter(this, listings);
        rvMapListings.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvMapListings.setLayoutManager(layoutManager);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.setOnMarkerClickListener(this);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(new LatLng(36.692733, -119.535017));
        map.moveCamera(cameraUpdate);
        getListings();

        rvMapListings.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Reset icon for previously highlighted marker
                    setMarkerIcon(prevHighlightedMarker, false);
                    prevHighlightedMarker.setZIndex(0.0f);

                    // Set marker icon for currently highlighted RecyclerView card
                    View snapView = snapHelper.findSnapView(layoutManager);
                    int position = rvMapListings.getChildAdapterPosition(snapView);
                    Listing listing = listings.get(position);

                    Marker marker = listingMarkerHM.get(listing.getObjectId());
                    setMarkerIcon(marker, true);
                    marker.setZIndex(0.1f);

                    boolean markerInView = map.getProjection()
                            .getVisibleRegion()
                            .latLngBounds
                            .contains(marker.getPosition());

                    if (!markerInView) {
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15);
//                    map.animateCamera(cameraUpdate, 1000, null);
                        map.animateCamera(cameraUpdate);
                    }

                    prevHighlightedMarker = marker;
                }
            }
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        setMarkerIcon(prevHighlightedMarker, false);
        prevHighlightedMarker.setZIndex(0.0f);

        setMarkerIcon(marker, true);
        prevHighlightedMarker = marker;
        marker.setZIndex(0.1f);

        Listing listing = MarkerListingHM.get(marker);
        int position = listings.indexOf(listing);
        rvMapListings.smoothScrollToPosition(position);
        return true;
    }

    private void getListings() {
        IconGenerator iconGenerator = new IconGenerator(MapsActivity.this);
        iconGenerator.setBackground(ContextCompat.getDrawable(MapsActivity.this, R.drawable.text_box));

        for (Listing listing : listings) {
            String price = String.format("$%s", listing.getPrice());
            Bitmap bitmap = iconGenerator.makeIcon(price);
            // Uses BitmapDescriptorFactory to create the marker
            BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);

            ParseGeoPoint geoPoint = listing.getCoordinates();
            LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(price)
                    .icon(icon));

            listingMarkerHM.put(listing.getObjectId(), marker);
            MarkerListingHM.put(marker, listing);
        }

        // Listing 1
        Listing listing1 = listings.get(0);
        Marker marker1 = listingMarkerHM.get(listing1.getObjectId());
        setMarkerIcon(marker1, true);
        marker1.setZIndex(0.1f);
        prevHighlightedMarker = marker1;

        ParseGeoPoint GeoPoint1 = listing1.getCoordinates();
        LatLng LatLng1 = new LatLng(GeoPoint1.getLatitude(), GeoPoint1.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng1, 15);
        map.moveCamera(cameraUpdate);
    }

    private void setMarkerIcon(Marker marker, boolean selected) {
        IconGenerator iconGenerator = new IconGenerator(MapsActivity.this);
        if (selected) {
            iconGenerator.setStyle(3); // sets text to white
            iconGenerator.setBackground(ContextCompat.getDrawable(this, R.drawable.text_box_color_accent));
            Bitmap bitmap = iconGenerator.makeIcon(marker.getTitle());
            // Uses BitmapDescriptorFactory to create the marker
            BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);
            marker.setIcon(icon);
        } else {
            iconGenerator.setBackground(ContextCompat.getDrawable(this, R.drawable.text_box));
            Bitmap bitmap = iconGenerator.makeIcon(marker.getTitle());
            // Uses BitmapDescriptorFactory to create the marker
            BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);
            marker.setIcon(icon);
        }
    }
}