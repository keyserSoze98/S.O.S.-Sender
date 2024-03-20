package com.keysersoze.sossender

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.keysersoze.sossender.AppConstants.DEFAULT_CONTACT_KEY
import com.keysersoze.sossender.AppConstants.PREFS_NAME

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var sosButton: Button
    private lateinit var contactEditText: EditText
    private var defaultContact: String? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_SMS = 123
        private const val PERMISSION_REQUEST_LOCATION = 456
        private const val PERMISSION_CONTACT_PICK_REQUEST = 789
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sosButton = findViewById(R.id.button_sos)
        contactEditText = findViewById(R.id.edittext_contact)

        // Get the default contact from SharedPreferences
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        defaultContact = prefs.getString(DEFAULT_CONTACT_KEY, "")

        // If a default contact is set, display it in the EditText field
        defaultContact?.let {
            if (!TextUtils.isEmpty(it)) {
                contactEditText.setText(it)
            }
        }

        sosButton.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_sos -> sendSOS()
        }
    }

    private fun sendSOS() {
        val contact = contactEditText.text.toString()
        try {
            validatePhoneNumber(contact)
            Log.d(TAG, "Phone number is valid.")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error: ${e.message}")
            Toast.makeText(this, "Please set a 10-digit contact number", Toast.LENGTH_LONG).show()
            return
        }
        if (TextUtils.isEmpty(contact)) {
            Toast.makeText(this, "Please set a default contact number", Toast.LENGTH_LONG).show()
            return
        }

        // Save the default contact to SharedPreferences
        val editor: SharedPreferences.Editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
        editor.putString(DEFAULT_CONTACT_KEY, contact)
        editor.apply()

        // Request SMS permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.SEND_SMS),
                PERMISSION_REQUEST_SMS)
        } else {
            // Request location permission if not granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_LOCATION)
            } else {
                // Permissions are granted, proceed with sending SOS
                sendSOSWithLocation(contact)
            }
        }
    }

    private fun sendSOSWithLocation(contact: String) {
        // Get the user's current location
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                // Send an SMS with the user's current location to the selected contact
                val message = "SOS: My current location is http://maps.google.com/maps?q=" +
                        "${location.latitude},${location.longitude}"
                val smsManager: SmsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(contact, null, message, null, null)
                Toast.makeText(this, "SOS message sent to $contact", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
        }
    }

    private fun validatePhoneNumber(phoneNumber: String) {
        val regex = "^\\d{10}$".toRegex()
        if (!phoneNumber.matches(regex)) {
            throw IllegalArgumentException("Invalid phone number. Please enter a 10-digit number.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_SMS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // SMS permission granted, proceed with location permission request
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_REQUEST_LOCATION)
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_LONG).show()
                }
            }
            PERMISSION_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted, proceed with sending SOS
                    sendSOSWithLocation(contactEditText.text.toString())
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
                }
            }
            PERMISSION_CONTACT_PICK_REQUEST -> {

            }
        }
    }
}
