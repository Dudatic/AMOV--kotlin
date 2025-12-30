package pt.isec.a2022136610.safetysec.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

enum class UserRole {
    MONITOR,
    PROTECTED,
    BOTH
}

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.PROTECTED,

    val monitorIds: List<String> = emptyList(),
    val protectedIds: List<String> = emptyList(),

    val lastLocation: GeoPoint? = null,
    val cancelPin: String = "0000"
)

enum class RuleType {
    FALL_DETECTION,
    ACCIDENT,
    GEOFENCING,
    MAX_SPEED,
    INACTIVITY,
    PANIC_BUTTON
}

data class SafetyRule(
    val id: String = "",
    val monitorId: String = "",
    val protectedId: String = "",
    val type: RuleType = RuleType.PANIC_BUTTON,
    val isActive: Boolean = true,
    val name: String = "",

    // --- REGRAS ESPECÍFICAS ---
    val maxSpeedKmh: Double? = null,
    val inactivityTimeMinutes: Int? = null,
    val geofenceCenter: GeoPoint? = null,
    val geofenceRadiusMeters: Double? = null,

    // --- JANELAS TEMPORAIS ---
    // Formato esperado: "HH:mm" (ex: "09:00", "23:30")
    val startTime: String? = null,
    val endTime: String? = null,
    // Dias da semana: 1 = Domingo, 2 = Segunda, ..., 7 = Sábado (Java Calendar)
    val activeDays: List<Int>? = null
)

data class SafetyAlert(
    val id: String = "",
    val protectedId: String = "",
    val ruleType: RuleType = RuleType.PANIC_BUTTON,
    val timestamp: Timestamp = Timestamp.now(),

    val location: GeoPoint? = null,
    val videoUrl: String? = null,

    val status: String = "PENDING",
    val cancelReason: String? = null
)