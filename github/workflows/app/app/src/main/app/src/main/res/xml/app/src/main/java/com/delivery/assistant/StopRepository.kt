package com.delivery.assistant

import androidx.compose.runtime.mutableStateMapOf

object StopRepository {
    // نگه‌داری نگاشت: شناسه سفارش -> شماره مقصد (۱ تا ۳۵)
    val destinationMap = mutableStateMapOf<String, Int>()

    fun clear() {
        destinationMap.clear()
    }

    fun addStop(orderId: String, stopIndex: Int) {
        destinationMap[orderId] = stopIndex
    }
}
