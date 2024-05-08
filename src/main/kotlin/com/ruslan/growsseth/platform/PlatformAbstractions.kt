package com.ruslan.growsseth.platform

import com.filloax.fxlib.platform.ServiceUtil

interface PlatformAbstractions {

    companion object {
        fun get(): PlatformAbstractions = ServiceUtil.findService(PlatformAbstractions::class.java)
    }
}