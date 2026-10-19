package com.delivery.assistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SnappAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return

        // 1. کلیک خودکار بر روی سفارش ۳۵ مقصد
        scanAndAutoAccept(rootNode)

        // 2. خواندن لیست اقلام و استخراج ترتیب مقاصد
        scrapeItemsList(rootNode)
    }

    private fun scanAndAutoAccept(node: AccessibilityNodeInfo) {
        val textList = mutableListOf<String>()
        collectAllText(node, textList)
        val fullContent = textList.joinToString(" ")

        // اگر سفارش ۳۵ مقصدی بود بلافاصله دکمه را کلیک کن
        if (fullContent.contains("۳۵ مقصد") || fullContent.contains("35 مقصد")) {
            val acceptButtons = node.findAccessibilityNodeInfosByText("قبول درخواست شاپ")
            for (btn in acceptButtons) {
                if (btn.isClickable) {
                    btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }
    }

    private fun scrapeItemsList(node: AccessibilityNodeInfo) {
        val rawTexts = mutableListOf<String>()
        collectAllText(node, rawTexts)

        var currentStop = 1
        for (text in rawTexts) {
            val normalized = normalizePersianDigits(text)
            // استخراج ارقام طولانی (شناسه سفارش شاپ)
            val regex = Regex("\\b\\d{6,12}\\b")
            val match = regex.find(normalized)
            if (match != null) {
                val orderId = match.value
                if (!StopRepository.destinationMap.containsKey(orderId)) {
                    StopRepository.addStop(orderId, currentStop)
                    currentStop++
                }
            }
        }
    }

    private fun collectAllText(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        node.text?.let { list.add(it.toString()) }
        for (i in 0 until node.childCount) {
            collectAllText(node.getChild(i), list)
        }
    }

    private fun normalizePersianDigits(str: String): String {
        return str.replace('۰', '0').replace('۱', '1')
            .replace('۲', '2').replace('۳', '3')
            .replace('۴', '4').replace('۵', '5')
            .replace('۶', '6').replace('۷', '7')
            .replace('۸', '8').replace('۹', '9')
    }

    override fun onInterrupt() {}
}
