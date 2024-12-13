package com.newbee.dynamic_watermark


import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.AppTask
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.lang.ref.WeakReference

class WatermarkAppLifeCycle : ActivityLifecycleCallbacks {

    companion object {
        var instance: WatermarkAppLifeCycle = WatermarkAppLifeCycle()
    }

    private var activityOnForegroundCount = 0

    private val TAG = "LifecycleEvent"

    private var weakMainActivity: WeakReference<Activity>? = null

    private var currentResumedActivity: WeakReference<Activity>? = null

    override fun onActivityCreated(activity: Activity, state: Bundle?) {
        android.util.Log.i(TAG, "onActivityCreated: $activity")

        // MainActivity as the MAIN ACTION Activity. It may started from Launcher icon or notification or other sources.
        if (activity::class == mainActivityClazz) {
            weakMainActivity = WeakReference(activity)
        }

        // apply watermark
        WaterMarkerManager.applyToActivity(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        android.util.Log.i(TAG, "onActivityStarted: $activity")
    }

    override fun onActivityResumed(activity: Activity) {
        android.util.Log.i(TAG, "onActivityResumed: $activity")
        currentResumedActivity = WeakReference(activity)

        // Count it
        val previousCount = activityOnForegroundCount
        activityOnForegroundCount++
        // App is now enter foreground
        if (previousCount <= 0 && activityOnForegroundCount > 0) {
            android.util.Log.i(TAG, "APP ENTER FOREGROUND NOW")
            this.invokeOnAppEnterForegroundListeners()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        android.util.Log.i(TAG, "onActivityPaused: $activity")
    }

    override fun onActivityStopped(activity: Activity) {
        android.util.Log.i(TAG, "onActivityStopped: $activity")

        // Count it
        activityOnForegroundCount--
        // App is now enter background
        if (activityOnForegroundCount <= 0) {
            android.util.Log.i(TAG, "APP ENTER BACKGROUND NOW")
            this.invokeOnAppEnterBackgroundListeners()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        android.util.Log.i(TAG, "onActivitySaveInstanceState: $activity")
    }

    override fun onActivityDestroyed(activity: Activity) {
        android.util.Log.i(TAG, "onActivityDestroyed: $activity")
    }

    val isAppInBackground: Boolean
        /**
         * Tell if app in foreground/background
         */
        get() = !isAppInForeground

    val isAppInForeground: Boolean
        get() = activityOnForegroundCount > 0

    /**
     * Listeners
     */
    private val mOnAppEnterForegroundListeners = ArrayList<Runnable>()

    fun addOnAppEnterForegroundListener(listener: Runnable) {
        mOnAppEnterForegroundListeners.add(listener)
    }

    fun removeOnAppEnterForegroundListener(listener: Runnable) {
        mOnAppEnterForegroundListeners.remove(listener)
    }

    fun invokeOnAppEnterForegroundListeners() {
        try {
            for (listener in mOnAppEnterForegroundListeners) {
                listener.run()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e(TAG, "invokeOnAppEnterForegroundListeners: " + e.message)
        }
    }

    private val mOnAppEnterBackgroundListeners = ArrayList<Runnable>()

    fun addOnAppEnterBackgroundListener(listener: Runnable) {
        mOnAppEnterBackgroundListeners.add(listener)
    }

    fun removeOnAppEnterBackgroundListener(listener: Runnable) {
        mOnAppEnterBackgroundListeners.remove(listener)
    }

    fun invokeOnAppEnterBackgroundListeners() {
        try {
            for (listener in mOnAppEnterBackgroundListeners) {
                listener.run()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "invokeOnAppEnterBackgroundListeners: " + e.message)
        }
    }

    /**
     * Bring the top activity to front when user click the app icon on home screen launcher or from ali notification etc.
     */
    var application: Application? = null
        set(value) {
            field = value
            field?.registerActivityLifecycleCallbacks(this)
        }

    var mainActivityClazz: Class<Any>? = null

    fun bringApplicationToFront() {
        mainActivityClazz?.let { bringMainActivityToFront() }
    }

    private fun bringMainActivityToFront() {
        val activity = mainActivity

        // start MainActivity as click in Launcher
        if (activity == null) {
            try {
                val application: Application = this.application ?: return
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.setClass(application, mainActivityClazz!!)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                application.startActivity(intent)
            } catch (e: Throwable) {
                android.util.Log.e(TAG, e.toString())
            }
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }

        // Get the task with MainActivity
        try {
            var mainTask: AppTask? = null
            val am = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            val appTasks = am.appTasks
            if (appTasks.isEmpty()) return
            for (i in appTasks.indices) {
                val task = appTasks[i]
                val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    task.taskInfo.baseActivity ?: continue
                } else {
                    TODO("VERSION.SDK_INT < M")
                }
                if (name.className == mainActivityClazz!!.name) {
                    mainTask = task
                    break
                }
            }
            mainTask?.moveToFront()
            return
        } catch (e: Throwable) {
            e.printStackTrace()
            android.util.Log.e(TAG, e.toString())
        }
        android.util.Log.e(TAG, "bringMainActivityToFront failed: cannot find MainActivity instance and its task")
    }


    val mainActivity: Activity?
        get() {
            if (weakMainActivity != null) {
                val act = weakMainActivity!!.get()
                if (act != null && !act.isFinishing && !act.isDestroyed) {
                    return act
                }
            }
            return null
        }

    val currentActivity: Activity?
        get() {
            if (currentResumedActivity != null) {
                val act = currentResumedActivity!!.get()
                if (act != null && !act.isFinishing && !act.isDestroyed) {
                    return act
                }
            }
            return null
        }


}
