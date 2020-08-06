package com.nurflower.screenrecorder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel:ViewModel() {

    private val _startRecording = MutableLiveData<Boolean>()
    val startRecording:LiveData<Boolean> get() = _startRecording

    private val _stopRecording = MutableLiveData<Boolean>()
    val stopRecording:LiveData<Boolean> get() = _stopRecording

    fun changeStartButtonStatus(){
        _startRecording.value = false
    }

    fun changeStopButtonStatus(){
        _stopRecording.value = false
    }

    fun onStopClick(){
        _stopRecording.value = true
    }

    fun onStartClick(){
        _startRecording.value = true
    }

}