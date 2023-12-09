package com.darkbyte.groupit

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.darkbyte.groupit.facedetection.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroupItApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.Default) {
            Utils.initiateTFLite(applicationContext, assets)
        }
    }
}