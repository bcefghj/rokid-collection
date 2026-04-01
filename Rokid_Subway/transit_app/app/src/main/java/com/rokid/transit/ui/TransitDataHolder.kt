package com.rokid.transit.ui

import com.rokid.transit.data.TransitPlan

object TransitDataHolder {
    var plans: List<TransitPlan> = emptyList()
    var destName: String = ""
    var selectedPlanIndex: Int = 0
}
