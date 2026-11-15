package com.fitai.app.ui.platform

import com.fitai.app.auth.PlatformAuthSession

class IosPlatformAuthSession : PlatformAuthSession {
    override suspend fun clear() = Unit
}
