package com.example.language_learning_helper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.language_learning_helper.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var servicePreferencesHelper: ServiceSharedPreferencesHelper
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    }
    
    private var currentFragmentTag: String = "MainFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()  // Hide the Action Bar manually
        servicePreferencesHelper = ServiceSharedPreferencesHelper(this)
        servicePreferencesHelper.initializeDefaults(false, true, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mainFragment = MainFragment()
        val statsFragment = StatsFragment()
        val settingsFragment = SettingsFragment()
        // Check if it's the first time the app is opened
        val isFirstTime = sharedPreferences.getBoolean("isFirstTime", true)

        if (isFirstTime) {
            // Launch GreetingActivity if it's the first time
            val intent = Intent(this, GreetingActivity::class.java)
            startActivity(intent)

            // Update SharedPreferences to mark that the app has been opened
            sharedPreferences.edit().putBoolean("isFirstTime", false).apply()
        }

        // Add all fragments initially and show only the first one
        
        supportFragmentManager.beginTransaction().apply {
            // Conditionally hide fragments based on the currentFragmentTag
            if (currentFragmentTag == "MainFragment") {
                add(R.id.frame_container, mainFragment, "MainFragment")
            } else {
                add(R.id.frame_container, mainFragment, "MainFragment").hide(mainFragment)
            }
        
            if (currentFragmentTag == "StatsFragment") {
                add(R.id.frame_container, statsFragment, "StatsFragment")
            } else {
                add(R.id.frame_container, statsFragment, "StatsFragment").hide(statsFragment)
            }
        
            if (currentFragmentTag == "SettingsFragment") {
                add(R.id.frame_container, settingsFragment, "SettingsFragment")
            } else {
                add(R.id.frame_container, settingsFragment, "SettingsFragment").hide(settingsFragment)
            }
        
            commit()
        }
        
        // Set bottom navigation listener
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> showFragment(mainFragment, "MainFragment")
                R.id.navigation_stats -> showFragment(statsFragment, "StatsFragment")
                R.id.navigation_settings -> showFragment(settingsFragment, "SettingsFragment")
            }
            true
        }        
    }

    private fun showFragment(fragment: Fragment, fragmentTag: String) {
        if (currentFragmentTag != fragmentTag) {
            supportFragmentManager.beginTransaction().apply {
                // Hide the currently displayed fragment
                supportFragmentManager.fragments.forEach { hide(it) }
                // Show the selected fragment
                show(fragment)
                commit()
            }
            currentFragmentTag = fragmentTag  // Update the current fragment tag
        }
    }
    
}
