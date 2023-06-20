package com.keysersoze.sossender;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button sosButton;
    private EditText contactEditText;
    private String defaultContact;

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_SMS = 123;
    private static final int PERMISSION_REQUEST_LOCATION = 456;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sosButton = findViewById(R.id.button_sos);
        contactEditText = findViewById(R.id.edittext_contact);

        // Get the default contact from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        defaultContact = prefs.getString(AppConstants.DEFAULT_CONTACT_KEY, "");

        // If a default contact is set, display it in the EditText field
        if (!TextUtils.isEmpty(defaultContact)) {
            contactEditText.setText(defaultContact);
        }

        sosButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_sos:
                sendSOS();
                break;
        }
    }

    private void sendSOS() {
        String contact = contactEditText.getText().toString();
        try {
            validatePhoneNumber(contact);
            Log.d(TAG, "Phone number is valid.");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error: " + e.getMessage());
            Toast.makeText(this, "Please set a 10-digit contact number", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(contact)) {
            Toast.makeText(this, "Please set a default contact number", Toast.LENGTH_LONG).show();
            return;
        }

        // Save the default contact to SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(AppConstants.DEFAULT_CONTACT_KEY, contact);
        editor.apply();

        // Request SMS permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    PERMISSION_REQUEST_SMS);
        } else {
            // Request location permission if not granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_LOCATION);
            } else {
                // Permissions are granted, proceed with sending SOS
                sendSOSWithLocation(contact);
            }
        }
    }

    private void sendSOSWithLocation(String contact) {
        // Get the user's current location
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    // Send an SMS with the user's current location to the selected contact
                    String message = "SOS: My current location is http://maps.google.com/maps?q=" +
                            location.getLatitude() + "," + location.getLongitude();
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(contact, null, message, null, null);

                    Toast.makeText(this, "SOS message sent to " + contact, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void validatePhoneNumber(String phoneNumber) {
        String regex = "^\\d{10}$";
        if (!phoneNumber.matches(regex)) {
            throw new IllegalArgumentException("Invalid phone number. Please enter a 10-digit number.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // SMS permission granted, proceed with location permission request
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_REQUEST_LOCATION);
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_LONG).show();
                }
                break;
            case PERMISSION_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted, proceed with sending SOS
                    sendSOSWithLocation(contactEditText.getText().toString());
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}
