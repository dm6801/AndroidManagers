package com.dm6801.managersexample

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import com.dm6801.androidmanagers.Managers

class MainActivity : AppCompatActivity() {

    lateinit var managersObserver: Managers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initManagersObserver()
    }

    @SuppressLint("SetTextI18n")
    private fun initManagersObserver() {
        managersObserver = Managers(this, isDialog = true)
        managersObserver.setAirplaneDialog(message = "airplane", button = "airplane button")
        managersObserver.setNetworkDialog(
            "network\n\n\n",
            null,
            R.layout.dialog_network_custom,
            R.id.dialog_network_custom_button to
                    { Toast.makeText(this, "TOAST", Toast.LENGTH_SHORT).show() }
        ) {
            findViewById<TextView?>(R.id.dialog_network_custom_button)?.text = "network button"
        }

        managersObserver.liveAirplane.observe(this, Observer { isEnabled ->
            Log.i(javaClass.simpleName, "airplane isEnabled=$isEnabled")
        })
        managersObserver.liveIsNetwork.observe(this, Observer { isEnabled ->
            Log.i(javaClass.simpleName, "network isEnabled=$isEnabled")
        })
        managersObserver.liveIsLocation.observe(this, Observer { isEnabled ->
            Log.i(javaClass.simpleName, "location isEnabled=$isEnabled")
        })
        managersObserver.liveIsLocationPermission.observe(this, Observer { isGranted ->
            Log.i(javaClass.simpleName, "location permission isGranted=$isGranted")
        })
        managersObserver.liveIsBluetooth.observe(this, Observer { isEnabled ->
            Log.i(javaClass.simpleName, "bluetooth isEnabled=$isEnabled")
        })
    }

}