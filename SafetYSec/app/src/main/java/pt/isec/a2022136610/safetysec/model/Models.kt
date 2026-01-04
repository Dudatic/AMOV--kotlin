package pt.isec.a2022136610.safetysec.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

enum class UserRole {
    MONITOR, PROTECTED, BOTH
}

enum class RuleType {
    GEOFENCING, FALL_DETECTION, ACCIDENT, MAX_SPEED, INACTIVITY, PANIC_BUTTON
}

// NEW: Status for Rule Approval
enum class RuleStatus {
    PENDING, ACTIVE, REJECTED
}

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.PROTECTED,
    val cancelPin: String = "0000",
    val protectedIds: List<String> = emptyList(),
    val monitorIds: List<String> = emptyList(),
    val lastLocation: GeoPoint? = null
)

data class SafetyAlert(
    val id: String = "",
    val protectedId: String = "",
    val ruleType: RuleType = RuleType.PANIC_BUTTON,
    val timestamp: Timestamp = Timestamp.now(),
    val location: GeoPoint? = null,
    val status: String = "ACTIVE",
    val cancelReason: String? = null,
    val videoUrl: String? = null
)

data class SafetyRule(
    val id: String = "",
    val monitorId: String = "",
    val protectedId: String = "",
    val type: RuleType = RuleType.GEOFENCING,
    // isActive checks if the rule is technically "on",
    // status checks if the user agreed to it.
    val isActive: Boolean = true,
    val status: RuleStatus = RuleStatus.PENDING, // Default to PENDING
    val name: String = "",
    val geofenceCenter: GeoPoint? = null,
    val geofenceRadiusMeters: Double? = null,
    val maxSpeedKmh: Double? = null,
    val inactivityTimeMinutes: Int? = null,
    val activeDays: List<Int>? = null,
    val startTime: String? = null,
    val endTime: String? = null
)