package com.android.screensharesender.model

class SenderManager private constructor() {
    companion object {
        val instance: SenderManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SenderManager()
        }
    }

    fun getData(): String {
        return SenderHelper.getData()
    }
}