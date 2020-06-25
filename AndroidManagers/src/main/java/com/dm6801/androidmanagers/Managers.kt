package com.dm6801.androidmanagers

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.location.LocationManagerCompat
import androidx.core.net.ConnectivityManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.lang.ref.WeakReference

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
class Managers(
    activity: Activity?,
    private val isDialog: Boolean = false
) : ActivityLifecycleCallbacks {

    companion object {
        private const val LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION
        private const val REQUEST_LOCATION_PERMISSION_CODE = 14341

        fun isLocationPermissionGranted(context: Context): Boolean {
            return PermissionChecker.checkSelfPermission(context, LOCATION_PERMISSION) ==
                    PermissionChecker.PERMISSION_GRANTED
        }

        private fun View.enable() {
            isEnabled = true; alpha = 1f
        }

        private fun View.disable() {
            isEnabled = false; alpha = 0.5f
        }
    }

    private enum class Manager(val intentFilterAction: String, val dialogLayout: Int) {
        Airplane(Intent.ACTION_AIRPLANE_MODE_CHANGED, R.layout.dialog_airplane),
        Network(ConnectivityManager.CONNECTIVITY_ACTION, R.layout.dialog_network),
        Location(LocationManager.MODE_CHANGED_ACTION, R.layout.dialog_location),
        Bluetooth(BluetoothAdapter.ACTION_STATE_CHANGED, R.layout.dialog_bluetooth);
    }

    private val _activity: WeakReference<Activity?> = WeakReference(activity)
    private val activity: Activity? get() = _activity.get()

    val networkManager: ConnectivityManager? get() = activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val locationManager: LocationManager? get() = activity?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val bluetoothManager: BluetoothManager? get() = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    val isAirplane: Boolean
        get() = activity?.contentResolver?.let {
            android.provider.Settings.Global.getInt(
                it, android.provider.Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
        } == true

    val isNetworkConnected: Boolean
        get() = networkManager?.activeNetworkInfo?.isConnected == true

    val isLocationEnabled: Boolean
        get() = locationManager?.let(LocationManagerCompat::isLocationEnabled) == true

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    val liveAirplane: LiveData<Boolean> = MutableLiveData(isAirplane)
    val liveIsNetwork: LiveData<Boolean> = MutableLiveData(isNetworkConnected)
    val liveIsLocation: LiveData<Boolean> = MutableLiveData(isLocationEnabled)
    val liveIsBluetooth: LiveData<Boolean> = MutableLiveData(isBluetoothEnabled)

    private val activityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            super.onActivityResumed(activity)
            if (activity == this@Managers.activity) {
                val isLocationPermissionGranted = isLocationPermissionGranted
                if (isLocationPermissionGranted != liveIsLocationPermission.value)
                    liveIsLocationPermission.set(isLocationPermissionGranted)
                if (isDialog) refreshDialogs()
            }
        }
    }

    val isLocationPermissionGranted: Boolean get() = activity?.let(Companion::isLocationPermissionGranted) == true
    val liveIsLocationPermission: LiveData<Boolean> = MutableLiveData(isLocationPermissionGranted)

    private val intentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        addAction(LocationManager.MODE_CHANGED_ACTION)
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Manager.Airplane.intentFilterAction -> onAirplaneBroadcast(intent)
                Manager.Network.intentFilterAction -> onNetworkBroadcast(intent)
                Manager.Location.intentFilterAction -> onLocationBroadcast(intent)
                Manager.Bluetooth.intentFilterAction -> onBluetoothBroadcast(intent)
            }
            if (isDialog) refreshDialogs()
        }
    }

    init {
        registerBroadcastReceiver()
        observeActivityLifecycle()
    }

    private fun registerBroadcastReceiver() {
        catch(silent = true) { activity?.unregisterReceiver(broadcastReceiver) }
        catch { activity?.registerReceiver(broadcastReceiver, intentFilter) }
    }

    private fun observeActivityLifecycle() {
        catch(silent = true) {
            activity?.application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        }
        catch { activity?.application?.registerActivityLifecycleCallbacks(activityLifecycleCallbacks) }
    }

    private fun onAirplaneBroadcast(intent: Intent) {
        val isEnabled = intent.getBooleanExtra("state", false)
        if (isEnabled != liveAirplane.value)
            liveAirplane.set(isEnabled)
    }

    private fun onNetworkBroadcast(intent: Intent) {
        val isConnected = networkManager
            ?.let { ConnectivityManagerCompat.getNetworkInfoFromBroadcast(it, intent) }
            ?.isConnected
            ?: isNetworkConnected
        if (isConnected != liveIsNetwork.value) {
            liveIsNetwork.set(isConnected)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onLocationBroadcast(intent: Intent) {
        val isEnabled = isLocationEnabled
        if (isEnabled != liveIsLocation.value) {
            liveIsLocation.set(isEnabled)
        }
    }

    private fun onBluetoothBroadcast(intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        val isEnabled = state == BluetoothAdapter.STATE_ON
        if (isEnabled != liveIsBluetooth.value) {
            liveIsBluetooth.set(isEnabled)
        }
    }

    private fun openAirplaneSettings() {
        activity?.startActivity(Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS))
    }

    private fun openNetworkSettings() {
        activity?.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
    }

    private fun openLocationSettings() {
        activity?.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            activity ?: return,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION_CODE
        )
    }

    private fun requestBluetooth() {
        activity?.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private val dialogs: MutableMap<Manager, AlertDialog?> = mutableMapOf()
    private val dialogSettings: MutableMap<Manager, DialogSettings> = mutableMapOf()

    private fun setDialogSettings(
        manager: Manager,
        message: CharSequence? = null,
        button: CharSequence? = null,
        layout: Int? = null,
        vararg actions: Pair<Int, () -> Unit>,
        onShow: (Dialog.() -> Unit)? = null
    ) {
        dialogSettings[manager] = createDialogSettings(
            manager,
            layout,
            message,
            button,
            actions.toMap(),
            onShow
        )
    }

    private fun requireDialogSettings(manager: Manager): DialogSettings {
        return dialogSettings[manager]
            ?: createDialogSettings(manager, null, null, null, null, null)
                .also { dialogSettings[manager] = it }
    }

    private fun showDialog(manager: Manager, onShow: (Dialog.() -> Unit)? = null) {
        showDialog(requireDialogSettings(manager), onShow)
    }

    private fun showDialog(dialogSettings: DialogSettings, onShow: (Dialog.() -> Unit)? = null) {
        var dialog = dialogs[dialogSettings.manager]
        if (dialog?.isShowing == true) {
            onShow?.invoke(dialog)
            return
        }
        val activity = activity ?: return
        dialog = createDialog(dialogSettings, activity)
        dialogs[dialogSettings.manager] = dialog
        dialog.showFullScreen {
            dialogSettings.labels?.forEach { (id, text) ->
                (findViewById<View?>(id) as? TextView)?.text = text
            }
            dialogSettings.message?.let { (id, text) ->
                (findViewById<View?>(id) as? TextView)?.text = text
            }
            dialogSettings.actions?.forEach { (id, action) ->
                findViewById<View?>(id)?.setOnClickListener { action() }
            }
            onShow?.invoke(this)
            dialogSettings.onShow?.invoke(this)
        }
    }

    private fun cancelDialog(manager: Manager) {
        dialogs[manager]?.cancel()
        dialogs.remove(manager)
    }

    private fun refreshDialogs() {
        if (isAirplane) showAirplaneDialog()
        else {
            hideAirplaneDialog()
            if (!isNetworkConnected) showNetworkDialog()
            else hideNetworkDialog()
            if (!isLocationEnabled || !isLocationPermissionGranted) showLocationDialog()
            else hideLocationDialog()
            if (!isBluetoothEnabled) showBluetoothDialog()
            else hideBluetoothDialog()
        }
    }

    fun setAirplaneDialog(
        message: CharSequence? = null,
        button: CharSequence? = null,
        layout: Int? = null,
        vararg actions: Pair<Int, () -> Unit>,
        onShow: (Dialog.() -> Unit)? = null
    ) {
        setDialogSettings(
            Manager.Airplane,
            message,
            button,
            layout,
            *actions,
            onShow = onShow
        )
    }

    private fun showAirplaneDialog() {
        showDialog(Manager.Airplane)
    }

    private fun hideAirplaneDialog() {
        cancelDialog(Manager.Airplane)
    }

    fun setNetworkDialog(
        message: CharSequence? = null,
        button: CharSequence? = null,
        layout: Int? = null,
        vararg actions: Pair<Int, () -> Unit>,
        onShow: (Dialog.() -> Unit)? = null
    ) {
        setDialogSettings(
            Manager.Network,
            message,
            button,
            layout,
            *actions,
            onShow = onShow
        )
    }

    private fun showNetworkDialog() {
        showDialog(Manager.Network)
    }

    private fun hideNetworkDialog() {
        cancelDialog(Manager.Network)
    }

    fun setLocationDialog(
        message: CharSequence? = null,
        button: CharSequence? = null,
        layout: Int? = null,
        vararg actions: Pair<Int, () -> Unit>,
        onShow: (Dialog.() -> Unit)? = null
    ) {
        setDialogSettings(
            Manager.Location,
            message,
            button,
            layout,
            *actions,
            onShow = onShow
        )
    }

    private fun showLocationDialog() {
        val enableButtonId = R.id.dialog_location_button
        val permissionButtonId = R.id.dialog_location_permissions
        val isEnabled = isLocationEnabled
        val isPermission = isLocationPermissionGranted

        showDialog(Manager.Location) {
            findViewById<Button?>(enableButtonId)?.apply {
                if (isEnabled) disable()
                else enable()
            }
            findViewById<Button?>(permissionButtonId)?.apply {
                if (isPermission) disable()
                else enable()
            }
        }
    }

    private fun hideLocationDialog() {
        cancelDialog(Manager.Location)
    }

    fun setBluetoothDialog(
        message: CharSequence? = null,
        button: CharSequence? = null,
        layout: Int? = null,
        vararg actions: Pair<Int, () -> Unit>,
        onShow: (Dialog.() -> Unit)? = null
    ) {
        setDialogSettings(
            Manager.Bluetooth,
            message,
            button,
            layout,
            *actions,
            onShow = onShow
        )
    }

    private fun showBluetoothDialog() {
        showDialog(Manager.Bluetooth)
    }

    private fun hideBluetoothDialog() {
        cancelDialog(Manager.Bluetooth)
    }

    private class DialogSettings(
        val manager: Manager,
        _layout: Int?,
        val message: Pair<Int, CharSequence>?,
        val labels: Map<Int, CharSequence>?,
        val actions: Map<Int, () -> Unit>?,
        val onShow: (Dialog.() -> Unit)?
    ) {
        val layout = _layout ?: manager.dialogLayout
    }

    private fun createDialogSettings(
        manager: Manager,
        layout: Int?,
        message: CharSequence?,
        button: CharSequence?,
        actions: Map<Int, () -> Unit>?,
        onShow: (Dialog.() -> Unit)?
    ): DialogSettings {
        val _layout: Int
        val textId: Int
        val buttonId: Int
        val _actions: MutableMap<Int, () -> Unit>

        when (manager) {
            Manager.Airplane -> {
                _layout = R.layout.dialog_airplane
                textId = R.id.dialog_airplane_text
                buttonId = R.id.dialog_airplane_button
                _actions = mutableMapOf(buttonId to ::openAirplaneSettings)
            }
            Manager.Network -> {
                _layout = R.layout.dialog_network
                textId = R.id.dialog_network_text
                buttonId = R.id.dialog_network_button
                _actions = mutableMapOf(buttonId to ::openNetworkSettings)
            }
            Manager.Location -> {
                _layout = R.layout.dialog_location
                textId = R.id.dialog_location_text
                buttonId = R.id.dialog_location_button
                _actions = mutableMapOf(
                    buttonId to ::openLocationSettings,
                    R.id.dialog_location_permissions to ::requestLocationPermissions
                )
            }
            Manager.Bluetooth -> {
                _layout = R.layout.dialog_bluetooth
                textId = R.id.dialog_bluetooth_text
                buttonId = R.id.dialog_bluetooth_button
                _actions = mutableMapOf(buttonId to ::requestBluetooth)
            }
        }

        actions?.let { _actions.putAll(it) }

        return DialogSettings(
            manager,
            layout ?: _layout,
            message?.let { textId to it },
            button?.let { buttonId to it }?.let(::mapOf),
            _actions,
            onShow
        )
    }

    private fun createDialog(dialogSettings: DialogSettings, context: Context): AlertDialog {
        return AlertDialog.Builder(context, R.style.Dialog_Fullscreen)
            .setView(dialogSettings.layout)
            .setCancelable(false)
            .create()
    }

}