package com.wanhella.criminalintent

import android.app.Application
import com.wanhella.criminalintent.database.CrimeRepository

class CriminalIntentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}