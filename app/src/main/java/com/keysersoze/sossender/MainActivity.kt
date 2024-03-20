package com.keysersoze.sossender

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var sosButton: Button
    private lateinit var contactText: TextView
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
        contactText = findViewById(R.id.edittext_contact)

        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS
        )
        if (!arePermissionsGranted(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_SMS)
        }

        defaultContact = DataPreferences(this@MainActivity).getDefaultContact()
        Log.d("##defaultContact", defaultContact.toString())

        contactText.text = defaultContact

        sosButton.setOnClickListener(this)

        contactText.setOnClickListener {
            pickContact()
        }
    }

    private fun arePermissionsGranted(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_sos -> sendSOS()
        }
    }

    private fun sendSOS() {
        val contact = contactText.text.toString()
        /*try {
            validatePhoneNumber(contact)
            Log.d(TAG, "Phone number is valid.")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error: ${e.message}")
            Toast.makeText(this, "Please set a 10-digit contact number", Toast.LENGTH_LONG).show()
            return
        }*/
        if (contact.contains("Please select a default")) {
            Toast.makeText(this, "Please set a default contact number", Toast.LENGTH_LONG).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.SEND_SMS),
                PERMISSION_REQUEST_SMS)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_LOCATION)
            } else {
                sendSOSWithLocation(contact)
            }
        }
    }

    private fun sendSOSWithLocation(contact: String) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
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

    private fun pickContact() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSION_CONTACT_PICK_REQUEST)
        } else {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(intent, PERMISSION_CONTACT_PICK_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_SMS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_REQUEST_LOCATION)
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_LONG).show()
                }
            }
            PERMISSION_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //sendSOSWithLocation(contactText.text.toString())
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
                }
            }
            PERMISSION_CONTACT_PICK_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickContact()
                } else {
                    Toast.makeText(this, "Contact permission denied", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_CONTACT_PICK_REQUEST && resultCode == Activity.RESULT_OK) {
            val contactUri = data?.data
            val cursor = contentResolver.query(contactUri!!, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val contactName =
                        it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    val contactId =
                        it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use { phone ->
                        if (phone.moveToFirst()) {
                            val phoneNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            val contactInfo = "$contactName - $phoneNumber"
                            DataPreferences(this@MainActivity).setDefaultContact(contactInfo)
                            Log.d("##contactInfo", contactInfo)
                            contactText.text = contactInfo
                        }
                    }
                }
            }
        }
    }
}