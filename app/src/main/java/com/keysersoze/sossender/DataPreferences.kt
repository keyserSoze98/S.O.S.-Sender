package com.keysersoze.sossender

import android.content.Context
import android.content.SharedPreferences

class DataPreferences(context: Context) {

    private val pref: SharedPreferences
    private val editor: SharedPreferences.Editor
    private val PRIVATE_MODE = 0

    init {
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }

    fun setDefaultContact(name: String) {
        editor.putString(DEFAULT_CONTACT, name)
        editor.apply()
    }

    fun getDefaultContact(): String {
        return pref.getString(DEFAULT_CONTACT, "Please select a default contact")!!
    }

    companion object {
        private const val PREF_NAME = "PREF_NAME"
        private const val DEFAULT_CONTACT = "DEFAULT_CONTACT"
    }
}