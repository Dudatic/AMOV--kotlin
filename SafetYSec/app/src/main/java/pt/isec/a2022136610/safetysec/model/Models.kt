package pt.isec.a2022136610.safetysec.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

enum class UserRole {
    MONITOR, PROTECTED, BOTH
}

enum class RuleType {
    GEOFENCING, FALL_DETECTION, ACCIDENT, MAX_SPEED, INACTIVITY, PANIC_BUTTON
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
    val status: String = "ACTIVE", // ACTIVE, RESOLVED, CANCELED
    val cancelReason: String? = null,
    val videoUrl: String? = null
)

data class SafetyRule(
    val id: String = "",
    val monitorId: String = "",
    val protectedId: String = "",
    val type: RuleType = RuleType.GEOFENCING,
    val isActive: Boolean = true,
    val name: String = "",
    // Rule specific params
    val geofenceCenter: GeoPoint? = null,
    val geofenceRadiusMeters: Double? = null,
    val maxSpeedKmh: Double? = null,
    val inactivityTimeMinutes: Int? = null,
    val activeDays: List<Int>? = null, // Calendar.DAY_OF_WEEK
    val startTime: String? = null, // "HH:mm"
    val endTime: String? = null
)