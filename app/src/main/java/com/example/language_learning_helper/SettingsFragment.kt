package com.example.language_learning_helper

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.content.Intent
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.widget.CompoundButton
import android.widget.TextView
import android.graphics.Color
import android.content.res.ColorStateList


class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var servicePermissionManager: ServicePermissionManager
    private lateinit var servicePreferencesHelper: ServiceSharedPreferencesHelper

    private lateinit var toggle1: Switch
    private lateinit var toggle2: Switch
    private lateinit var toggle3: Switch
    private lateinit var text21: TextView
    private lateinit var text22: TextView
    private lateinit var text31: TextView
    private lateinit var text32: TextView

    private lateinit var thumbColorStateList: ColorStateList
    private lateinit var trackColorStateList: ColorStateList
    private lateinit var thumbColorStateListUnenabled: ColorStateList
    private lateinit var trackColorStateListUnenabled: ColorStateList

    
    // BroadcastReceiver to listen for changes
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.example.language_learning_helper.TOGGLE_FLOATING_ICON_VISIBILITY" -> {
                    println("Received broadcast: TOGGLE_FLOATING_ICON_VISIBILITY")
                    val fromFragment = intent.getBooleanExtra("from_fragment", false)
                    // Prevent infinite loop by temporarily removing the listener
                    if(!fromFragment){
                        toggle2.setOnCheckedChangeListener(null)
                        toggle2.isChecked = !toggle2.isChecked
                        toggle2.setOnCheckedChangeListener(toggle2Listener)
                    }
                }

                "com.example.language_learning_helper.STOP_SERVICE" -> {
                    println("Received broadcast: STOP_SERVICE")

                    // Prevent infinite loop by temporarily removing the listener
                    toggle1.setOnCheckedChangeListener(null)
                    toggle1.isChecked = false
                    toggle1.setOnCheckedChangeListener(toggle1Listener)

                    updateRowsState(text21, text22, text31, text32, toggle2, toggle3, false)
                }
            }
        }
    }

    private lateinit var toggle1Listener: CompoundButton.OnCheckedChangeListener
    private lateinit var toggle2Listener: CompoundButton.OnCheckedChangeListener
    private lateinit var toggle3Listener: CompoundButton.OnCheckedChangeListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Register the BroadcastReceiver
        val filter = IntentFilter().apply {
            addAction("com.example.language_learning_helper.TOGGLE_FLOATING_ICON_VISIBILITY")
            addAction("com.example.language_learning_helper.STOP_SERVICE")
        }
        requireActivity().registerReceiver(broadcastReceiver, filter)
        

        // Initialize SharedPreferences helper
        servicePreferencesHelper = ServiceSharedPreferencesHelper(requireContext())
        servicePermissionManager = ServicePermissionManager(requireActivity(), servicePreferencesHelper)

        val myImage = view.findViewById<ImageView>(R.id.myImage)
        toggle1 = view.findViewById(R.id.toggle1)
        toggle2 = view.findViewById(R.id.toggle2)
        toggle3 = view.findViewById(R.id.toggle3)

        text21 = view.findViewById(R.id.text_2_1)
        text22 = view.findViewById(R.id.text_2_2)
        text31 = view.findViewById(R.id.text_3_1)
        text32 = view.findViewById(R.id.text_3_2)

        val onThumbColor = ContextCompat.getColor(requireContext(), R.color.color_primary_variant)
        val offThumbColor = ContextCompat.getColor(requireContext(), R.color.switch_grey)
        val onTrackColor = ContextCompat.getColor(requireContext(), R.color.purple_200)
        val offTrackColor = ContextCompat.getColor(requireContext(), R.color.switch_light_grey)
        


        // Define the color state lists for thumb and track
        thumbColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked), // Checked (ON) state
                intArrayOf(-android.R.attr.state_checked) // Unchecked (OFF) state
            ),
            intArrayOf(
                onThumbColor, // Color when ON
                offThumbColor // Color when OFF
            )
        )

        trackColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked), // Checked (ON) state
                intArrayOf(-android.R.attr.state_checked) // Unchecked (OFF) state
            ),
            intArrayOf(
                onTrackColor, // Color when ON
                offTrackColor // Color when OFF
            )
        )

        // Define the color state lists for thumb and track
        thumbColorStateListUnenabled = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked), // Checked (ON) state
                intArrayOf(-android.R.attr.state_checked) // Unchecked (OFF) state
            ),
            intArrayOf(
                offThumbColor, // Color when ON
                offThumbColor // Color when OFF
            )
        )

        trackColorStateListUnenabled = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked), // Checked (ON) state
                intArrayOf(-android.R.attr.state_checked) // Unchecked (OFF) state
            ),
            intArrayOf(
                offTrackColor, // Color when ON
                offTrackColor // Color when OFF
            )
        )

        // Apply the color state lists to the thumb and track of the switch
        toggle1.thumbTintList = thumbColorStateList
        toggle1.trackTintList = trackColorStateList
        


        
        // Set initial switch values based on stored preferences
        val isServiceOn = servicePreferencesHelper.isServiceOn()
        println("$isServiceOn")
        toggle1.isChecked = isServiceOn
        toggle2.isChecked = servicePreferencesHelper.showFloatingIcon()
        toggle3.isChecked = servicePreferencesHelper.isScanningModeOn()

        // Update UI for initial state
        updateRowsState(text21, text22, text31, text32, toggle2, toggle3, isServiceOn)

        // Initialize listeners
        toggle1Listener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            println("toggled in 1")
            // Prevent infinite loop
            toggle1.setOnCheckedChangeListener(null)
            toggle1.isChecked = !isChecked
            if (!isChecked) {
                println("Stopping the service")
                val broadcastIntent = Intent("com.example.language_learning_helper.STOP_SERVICE")
                requireContext().sendBroadcast(broadcastIntent)
                // Reattach the listener
                toggle1.setOnCheckedChangeListener(toggle1Listener)
                
            } else {
                println("Requesting service permission")
                lifecycleScope.launch {
                    val isPermissible = servicePermissionManager.setupPermissions()
                    // Update toggle state based on permission result
                    toggle1.isChecked = isPermissible
                    println("Permission granted: $isPermissible")

                    updateRowsState(text21, text22, text31, text32, toggle2, toggle3, isPermissible)
                    // Reattach the listener
                    toggle1.setOnCheckedChangeListener(toggle1Listener)
                }
            }
        }

        toggle2Listener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            // Prevent infinite loop
            println("Toggling floating icon visibility")
            val broadcastIntent = Intent("com.example.language_learning_helper.TOGGLE_FLOATING_ICON_VISIBILITY")
            broadcastIntent.putExtra("from_fragment", true)  // Add the boolean extra
            requireContext().sendBroadcast(broadcastIntent)

        }

        toggle3Listener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            println("Toggling scanning mode")
            val broadcastIntent = Intent("com.example.language_learning_helper.TOGGLE_SCANNING_MODE")
            requireContext().sendBroadcast(broadcastIntent)
        }

        // Attach listeners
        toggle1.setOnCheckedChangeListener(toggle1Listener)
        toggle2.setOnCheckedChangeListener(toggle2Listener)
        toggle3.setOnCheckedChangeListener(toggle3Listener)
    
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            // Set initial switch values based on stored preferences
            val isServiceOn = servicePreferencesHelper.isServiceOn()
            toggle1.isChecked = isServiceOn
            toggle2.isChecked = servicePreferencesHelper.showFloatingIcon()
            toggle3.isChecked = servicePreferencesHelper.isScanningModeOn()

            // Update UI for initial state
            updateRowsState(text21, text22, text31, text32, toggle2, toggle3, isServiceOn)
        }
    }

    // Helper function to enable/disable rows based on toggle state
    private fun updateRowsState(
    text21: TextView, text22: TextView, text31: TextView, text32: TextView,
    toggle2: Switch, toggle3: Switch, isServiceOn: Boolean) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.color_on_primary)
        val disabledColor = Color.GRAY // Gray color when service is OFF

        val textColor = if (isServiceOn) activeColor else disabledColor

        // Apply the color to each TextView
        text21.setTextColor(textColor)
        text22.setTextColor(textColor)
        text31.setTextColor(textColor)
        text32.setTextColor(textColor)

        if(isServiceOn){
            toggle2.thumbTintList = thumbColorStateList
            toggle2.trackTintList = trackColorStateList
            toggle3.thumbTintList = thumbColorStateList
            toggle3.trackTintList = trackColorStateList
        }else{
            toggle2.thumbTintList = thumbColorStateListUnenabled
            toggle2.trackTintList = trackColorStateListUnenabled
            toggle3.thumbTintList = thumbColorStateListUnenabled
            toggle3.trackTintList = trackColorStateListUnenabled
        }

        // Enable/disable the switches
        toggle2.isEnabled = isServiceOn  
        toggle3.isEnabled = isServiceOn  
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }
}

