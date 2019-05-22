package com.xda.nobar.util

import android.os.Binder

abstract class IImersiveHelperManager : Binder() {
    abstract fun enterNavImmersive()
    abstract fun exitNavImmersive()
    abstract fun isNavImmersive(): Boolean
    abstract fun isStatusImmersive(): Boolean
    abstract fun isFullImmersive(): Boolean
    abstract fun isFullPolicyControl(): Boolean
    abstract fun isNavPolicyControl(): Boolean
    abstract fun isStatusPolicyControl(): Boolean
    abstract fun tempForcePolicyControlForRecents()
    abstract fun putBackOldImmersive()
}