package com.keysersoze.sossender;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button sosButton;
    private EditText contactEditText;
    private String defaultContact;

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

    private boolean isValidPhoneNumber(String phoneNumber) {
        // Remove all non-numeric characters from the phone number
        phoneNumber = phoneNumber.replaceAll("[^\\d]", "");

        // Check if the resulting phone number is exactly 10 digits
        return (phoneNumber.length() == 10);
    }


    private void sendSOS() {
        String contact = contactEditText.getText().toString();
        if (TextUtils.isEmpty(contact)) {
            Toast.makeText(this, "Please set a default contact number", Toast.LENGTH_LONG).show();
            return;
        }

        // Save the default contact to SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(AppConstants.DEFAULT_CONTACT_KEY, contact);
        editor.apply();

        // Get the user's current location
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please grant location permission", Toast.LENGTH_LONG).show();
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_LONG).show();
            return;
        }

        // Send an SMS with the user's current location to the selected contact
        String message = "SOS: My current location is http://maps.google.com/maps?q=" +
                location.getLatitude() + "," + location.getLongitude();
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(contact, null, message, null, null);

        Toast.makeText(this, "SOS message sent to " + contact, Toast.LENGTH_LONG).show();
    }
}
