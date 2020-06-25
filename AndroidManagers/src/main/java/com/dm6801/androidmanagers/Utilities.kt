package com.dm6801.androidmanagers

import android.app.Dialog
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

internal val isMainThread: Boolean get() = Thread.currentThread() == Looper.getMainLooper().thread

internal fun <T> LiveData<T>.set(value: T) {
    try {
        (this as? MutableLiveData)?.run {
            if (isMainThread) this.value = value
            else postValue(value)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal fun <T, R> T.catch(silent: Boolean = false, action: T.() -> R?): R? {
    return try {
        action()
    } catch (e: Exception) {
        if (!silent) e.printStackTrace()
        null
    }
}

internal fun Dialog.showFullScreen(
    gravity: Int = Gravity.CENTER,
    onShow: (Dialog.() -> Unit)? = null
) {
    setOnShowListener {
        this@showFullScreen.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        this@showFullScreen.window?.setGravity(gravity)
        onShow?.invoke(this)
    }
    show()
}