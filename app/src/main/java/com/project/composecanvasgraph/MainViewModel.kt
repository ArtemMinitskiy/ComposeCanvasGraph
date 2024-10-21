package com.example.composepractice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            startEmit()
        }
    }

    private val _testPointsFlow = MutableStateFlow(0)
    var testPointFlow: StateFlow<Int> = _testPointsFlow
    private suspend fun startEmit() {
        viewModelScope.launch(Dispatchers.IO) {
            repeat(60) {
                delay(200)
                _testPointsFlow.emit(Random.nextInt(0, 100))
            }
        }
    }
}