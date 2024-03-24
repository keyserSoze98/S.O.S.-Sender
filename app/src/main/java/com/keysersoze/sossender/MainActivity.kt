package com.keysersoze.sossender

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.keysersoze.sossender.databinding.ActivityMainBinding
import kotlin.system.exitProcess

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private var defaultContact: String? = null

    companion object {
        private const val PERMISSION_REQUEST_SMS = 123
        private const val PERMISSION_REQUEST_LOCATION = 456
        private const val PERMISSION_CONTACT_PICK_REQUEST = 789
        private const val ACTION_SEND_SOS = "com.keysersoze.sossender.ACTION_SEND_SOS"
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS
        )
        if (!arePermissionsGranted(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_SMS)
        }

        toggle = ActionBarDrawerToggle(this, viewBinding.drawerLayout, R.string.open, R.string.close)
        viewBinding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setNavigationFunctions()

        defaultContact = DataPreferences(this@MainActivity).getDefaultContact()
        Log.d("##defaultContact", defaultContact.toString())

        viewBinding.edittextContact.text = defaultContact

        viewBinding.buttonSos.setOnClickListener(this)

        viewBinding.edittextContact.setOnClickListener {
            pickContact()
        }

        if (intent.action == ACTION_SEND_SOS) {
            sendSOS()
            finishAffinity()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun setNavigationFunctions() {
        viewBinding.navView.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.createShortcut -> createShortcut()

                R.id.about -> startActivity(Intent(this, About::class.java))

                R.id.exit -> exitApp()
            }
            true
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_sos -> sendSOS()
        }
    }

    private fun sendSOS() {
        val contact = viewBinding.edittextContact.text.toString()
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
                            viewBinding.edittextContact.text = contactInfo
                        }
                    }
                }
            }
        }
    }

    private fun createShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager

            val intent = Intent(this, MainActivity::class.java)
            intent.action = ACTION_SEND_SOS

            val shortcutId = "shortcut_sos_${System.currentTimeMillis()}"
            val shortcut = ShortcutInfo.Builder(this, shortcutId)
                .setShortLabel(getString(R.string.shortcut_label))
                .setIcon(Icon.createWithResource(this, R.drawable.sos_tap))
                .setIntent(intent)
                .build()

            shortcutManager.requestPinShortcut(shortcut, null)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_SEND_SOS) {
            sendSOS()
            finishAffinity()
        }
    }

    private fun exitApp() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit")
            .setMessage("Exit S.O.S. Sender?")
            .setCancelable(false)
            .setPositiveButton("Yes") {_, _ ->
                exitProcess(1)
            }
            .setNegativeButton("No") {dialog, _ ->
                dialog.dismiss()
            }

        val customDialog = builder.create()
        customDialog.show()
        customDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
        customDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
    }
}