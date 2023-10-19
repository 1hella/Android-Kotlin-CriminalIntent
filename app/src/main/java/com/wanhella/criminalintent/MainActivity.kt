package com.wanhella.criminalintent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wanhella.criminalintent.databinding.ActivityMainBinding

private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}