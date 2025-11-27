package com.app.jdvideoplayer.viewModels

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.jdvideoplayer.Utils.PreferenceManager
import com.app.jdvideoplayer.model.DemoClass
import com.app.jdvideoplayer.repository.MainRepository
import kotlinx.coroutines.launch

class MainViewModel (var activity: Activity) : ViewModel() {

    private val preferenceManager: PreferenceManager = PreferenceManager(activity)
    private val mainRepository: MainRepository = MainRepository(activity)

    fun getDataApiCall(hashMap: HashMap<String, Any>): MutableLiveData<DemoClass> {

        var api : MutableLiveData<DemoClass> = MutableLiveData()

        viewModelScope.launch {
            api = mainRepository.getData(hashMap)
        }

        return api
    }
}