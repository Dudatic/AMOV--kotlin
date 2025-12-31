package pt.isec.a2022136610.safetysec.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.isec.a2022136610.safetysec.model.RuleType
import pt.isec.a2022136610.safetysec.model.SafetyAlert
import pt.isec.a2022136610.safetysec.model.SafetyRule
import pt.isec.a2022136610.safetysec.model.UserProfile
import pt.isec.a2022136610.safetysec.model.UserRole
import java.util.Calendar
import kotlin.random.Random

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser

    private val _generatedCode = MutableStateFlow<String?>(null)
    val generatedCode: StateFlow<String?> = _generatedCode

    private val _associatedUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val associatedUsers: StateFlow<List<UserProfile>> = _associatedUsers

    private val _targetUser = MutableStateFlow<UserProfile?>(null)
    val targetUser: StateFlow<UserProfile?> = _targetUser

    private val _alertHistory = MutableStateFlow<List<SafetyAlert>>(emptyList())
    val alertHistory: StateFlow<List<SafetyAlert>> = _alertHistory

    // New: Monitor Alerts
    private val _monitorAlerts = MutableStateFlow<List<SafetyAlert>>(emptyList())
    val monitorAlerts: StateFlow<List<SafetyAlert>> = _monitorAlerts

    // New: Single Alert for Details
    private val _selectedAlert = MutableStateFlow<SafetyAlert?>(null)
    val selectedAlert: StateFlow<SafetyAlert?> = _selectedAlert

    private val _selectedUserRules = MutableStateFlow<List<SafetyRule>>(emptyList())
    val selectedUserRules: StateFlow<List<SafetyRule>> = _selectedUserRules

    private var myRules: List<SafetyRule> = emptyList()

    private val _showCountdown = MutableStateFlow(false)
    val showCountdown: StateFlow<Boolean> = _showCountdown

    private var pendingAlertReason: String? = null
    private var isAlertActive = false
    private var currentAlertId: String? = null

    private var lastMovementTime: Long = System.currentTimeMillis()
    private var lastKnownLocation: Location? = null

    // Listener registrations to avoid leaks and duplication
    private var monitorAlertsListener: ListenerRegistration? = null
    private var rulesListener: ListenerRegistration? = null

    // --- LOGIN ---
    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _authState.value = AuthState.Success
                loadCurrentUser()
            }
            .addOnFailureListener { e -> _authState.value = AuthState.Error(e.message ?: "Login Error") }
    }

    // --- REGISTER ---
    fun register(email: String, pass: String, name: String, role: UserRole) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener
                val newUser = UserProfile(id = userId, email = email, name = name, role = role, cancelPin = "0000")

                db.collection("users").document(userId).set(newUser)
                    .addOnSuccessListener {
                        _currentUser.value = newUser
                        _authState.value = AuthState.Success
                    }
                    .addOnFailureListener { _authState.value = AuthState.Error("Error saving profile") }
            }
            .addOnFailureListener { e -> _authState.value = AuthState.Error(e.message ?: "Registration Error") }
    }

    // --- LOAD DATA ---
    fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(UserProfile::class.java)
                    _currentUser.value = user

                    if (user != null && user.protectedIds.isNotEmpty()) {
                        fetchAssociatedUsers(user.protectedIds)
                        fetchMonitorAlerts(user.protectedIds) // Fetch alerts for monitor
                    }

                    if (user != null && (user.role == UserRole.PROTECTED || user.role == UserRole.BOTH)) {
                        startListeningToRules(user.id)
                    }
                }
            }
    }

    // --- UPDATE PIN ---
    fun updateCancelPin(newPin: String) {
        val userId = auth.currentUser?.uid ?: return
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            _authState.value = AuthState.Error("PIN must be 4 digits")
            return
        }

        db.collection("users").document(userId).update("cancelPin", newPin)
            .addOnSuccessListener {
                // Update local user object immediately for UI reflection
                _currentUser.value = _currentUser.value?.copy(cancelPin = newPin)
                // Just to trigger a success toast if needed, but keeping state clean is better
                Log.d("AUTH", "PIN Updated Successfully")
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error("Failed to update PIN")
            }
    }

    // --- HISTORY (For Protected) ---
    fun fetchAlertHistory() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("alerts")
            .whereEqualTo("protectedId", userId)
            .limit(50)
            .get()
            .addOnSuccessListener { result ->
                val alerts = result.toObjects(SafetyAlert::class.java)
                _alertHistory.value = alerts
                    .filter { it.status != "CANCELED" }
                    .sortedByDescending { it.timestamp }
            }
            .addOnFailureListener { e -> Log.e("HISTORY", "Error fetching history: ${e.message}") }
    }

    // --- MONITOR ALERTS (For Monitor Dashboard) ---
    fun fetchMonitorAlerts(protectedIds: List<String>) {
        if (protectedIds.isEmpty()) return

        // Remove previous listener if exists
        monitorAlertsListener?.remove()

        val safeIds = protectedIds.take(10)

        // Simplified query: Fetch all by ID, filter 'CANCELED' locally.
        monitorAlertsListener = db.collection("alerts")
            .whereIn("protectedId", safeIds)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("MONITOR", "Error listening to alerts", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val alerts = snapshots.toObjects(SafetyAlert::class.java)
                    // Local filtering and sorting
                    val activeAlerts = alerts
                        .filter { it.status != "CANCELED" }
                        .sortedByDescending { it.timestamp }

                    _monitorAlerts.value = activeAlerts
                }
            }
    }

    fun getAlert(alertId: String) {
        _selectedAlert.value = null
        db.collection("alerts").document(alertId).addSnapshotListener { doc, e ->
            if (doc != null && doc.exists()) {
                _selectedAlert.value = doc.toObject(SafetyAlert::class.java)
            }
        }
    }

    fun resolveAlert(alertId: String) {
        db.collection("alerts").document(alertId).update("status", "RESOLVED")
    }

    // --- RULES MANAGEMENT ---
    fun loadRulesForUser(protectedId: String) {
        db.collection("rules")
            .whereEqualTo("protectedId", protectedId)
            .get()
            .addOnSuccessListener { result ->
                val rules = result.toObjects(SafetyRule::class.java)
                _selectedUserRules.value = rules
            }
    }

    fun saveRule(rule: SafetyRule) {
        val ruleId = if (rule.id.isEmpty()) db.collection("rules").document().id else rule.id
        val ruleData = rule.copy(id = ruleId)

        db.collection("rules").document(ruleId).set(ruleData)
            .addOnSuccessListener {
                Log.d("RULES", "Rule saved: $ruleId")
                loadRulesForUser(rule.protectedId)
            }
            .addOnFailureListener { e ->
                Log.e("RULES", "Error saving rule: ${e.message}")
            }
    }

    private fun startListeningToRules(userId: String) {
        rulesListener?.remove()
        rulesListener = db.collection("rules")
            .whereEqualTo("protectedId", userId)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    myRules = snapshots.toObjects(SafetyRule::class.java)
                    Log.d("RULES", "Active rules updated: ${myRules.size}")
                }
            }
    }

    private fun fetchAssociatedUsers(ids: List<String>) {
        if (ids.isEmpty()) return
        db.collection("users").whereIn(FieldPath.documentId(), ids).get()
            .addOnSuccessListener { documents ->
                _associatedUsers.value = documents.toObjects(UserProfile::class.java)
            }
    }

    fun loadTargetUser(targetId: String) {
        _targetUser.value = null
        db.collection("users").document(targetId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    _targetUser.value = document.toObject(UserProfile::class.java)
                }
            }
    }

    fun generateAssociationCode() {
        val code = Random.nextInt(100000, 999999).toString()
        val userId = auth.currentUser?.uid ?: return
        val codeData = hashMapOf("protectedId" to userId, "timestamp" to FieldValue.serverTimestamp())

        db.collection("codes").document(code).set(codeData)
            .addOnSuccessListener { _generatedCode.value = code }
    }

    fun connectWithProtege(code: String) {
        val monitorId = auth.currentUser?.uid ?: return
        db.collection("codes").document(code).get()
            .addOnSuccessListener { document ->
                val protectedId = document.getString("protectedId")
                if (protectedId != null) {
                    performAssociation(monitorId, protectedId, code)
                } else {
                    _authState.value = AuthState.Error("Invalid Code")
                }
            }
            .addOnFailureListener { _authState.value = AuthState.Error("Verification Error") }
    }

    private fun performAssociation(monitorId: String, protectedId: String, codeUsed: String) {
        val batch = db.batch()
        batch.update(db.collection("users").document(protectedId), "monitorIds", FieldValue.arrayUnion(monitorId))
        batch.update(db.collection("users").document(monitorId), "protectedIds", FieldValue.arrayUnion(protectedId))
        batch.delete(db.collection("codes").document(codeUsed))

        batch.commit().addOnSuccessListener {
            _authState.value = AuthState.Success
            loadCurrentUser()
        }
    }

    // --- SENSOR LOGIC ---
    private fun isRuleActiveNow(rule: SafetyRule): Boolean {
        if (!rule.isActive) return false

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentDay = now.get(Calendar.DAY_OF_WEEK)

        if (rule.activeDays != null && rule.activeDays.isNotEmpty()) {
            if (!rule.activeDays.contains(currentDay)) return false
        }

        if (rule.startTime != null && rule.endTime != null) {
            try {
                val (startH, startM) = rule.startTime.split(":").map { it.toInt() }
                val (endH, endM) = rule.endTime.split(":").map { it.toInt() }
                val nowMins = currentHour * 60 + currentMinute
                val startMins = startH * 60 + startM
                val endMins = endH * 60 + endM

                if (startMins <= endMins) {
                    if (nowMins < startMins || nowMins > endMins) return false
                } else {
                    if (nowMins < startMins && nowMins > endMins) return false
                }
            } catch (e: Exception) {
                Log.e("RULES", "Time parse error")
            }
        }
        return true
    }

    fun handleFallDetected() {
        if (isAlertActive) return
        val activeRule = myRules.firstOrNull { it.type == RuleType.FALL_DETECTION && isRuleActiveNow(it) }
        if (activeRule != null) {
            triggerCountdown("FALL DETECTED (${activeRule.name})")
        } else {
            triggerCountdown("FALL DETECTED (Test/No Rule)")
        }
    }

    fun handleAccidentDetected() {
        if (isAlertActive) return
        val activeRule = myRules.firstOrNull { it.type == RuleType.ACCIDENT && isRuleActiveNow(it) }
        if (activeRule != null) {
            triggerCountdown("ACCIDENT DETECTED (${activeRule.name})")
        } else {
            triggerCountdown("ACCIDENT DETECTED (Test/No Rule)")
        }
    }

    fun updateUserLocation(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        db.collection("users").document(userId).update("lastLocation", geoPoint)
        checkInactivityRules(location)

        if (isAlertActive) return
        checkGeofenceRules(location)
        checkSpeedRules(location)
    }

    private fun checkGeofenceRules(location: Location) {
        for (rule in myRules) {
            if (rule.type == RuleType.GEOFENCING && rule.geofenceCenter != null && rule.geofenceRadiusMeters != null) {
                if (!isRuleActiveNow(rule)) continue
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    rule.geofenceCenter.latitude, rule.geofenceCenter.longitude,
                    results
                )
                if (results[0] > rule.geofenceRadiusMeters) {
                    triggerCountdown(reason = "LEFT SAFE ZONE: ${rule.name}")
                    return
                }
            }
        }
    }

    private fun checkSpeedRules(location: Location) {
        val currentSpeedKmh = location.speed * 3.6
        for (rule in myRules) {
            if (rule.type == RuleType.MAX_SPEED && rule.maxSpeedKmh != null) {
                if (!isRuleActiveNow(rule)) continue
                if (currentSpeedKmh > rule.maxSpeedKmh) {
                    triggerCountdown(reason = "SPEEDING: ${String.format("%.1f", currentSpeedKmh)} km/h")
                    return
                }
            }
        }
    }

    private fun checkInactivityRules(currentLocation: Location) {
        val now = System.currentTimeMillis()
        if (lastKnownLocation != null) {
            if (currentLocation.distanceTo(lastKnownLocation!!) > 100) {
                lastMovementTime = now
            }
        }
        lastKnownLocation = currentLocation

        for (rule in myRules) {
            if (rule.type == RuleType.INACTIVITY && rule.inactivityTimeMinutes != null) {
                if (!isRuleActiveNow(rule)) continue
                val inactiveDurationMinutes = (now - lastMovementTime) / 60000
                if (inactiveDurationMinutes >= rule.inactivityTimeMinutes) {
                    triggerCountdown(reason = "INACTIVITY: ${inactiveDurationMinutes} min")
                    lastMovementTime = now
                    return
                }
            }
        }
    }

    fun triggerCountdown(reason: String) {
        if (isAlertActive) return
        isAlertActive = true
        pendingAlertReason = reason
        _showCountdown.value = true
    }

    fun verifyPinAndCancel(inputPin: String) {
        val correctPin = _currentUser.value?.cancelPin ?: "0000"
        if (inputPin == correctPin) {
            logCanceledAlert()
            _showCountdown.value = false
            isAlertActive = false
            pendingAlertReason = null
        }
    }

    private fun determineRuleType(reason: String): RuleType {
        return when {
            reason.contains("ZONE", ignoreCase = true) -> RuleType.GEOFENCING
            reason.contains("FALL", ignoreCase = true) -> RuleType.FALL_DETECTION
            reason.contains("ACCIDENT", ignoreCase = true) -> RuleType.ACCIDENT
            reason.contains("SPEED", ignoreCase = true) -> RuleType.MAX_SPEED
            reason.contains("INACTIVITY", ignoreCase = true) -> RuleType.INACTIVITY
            else -> RuleType.PANIC_BUTTON
        }
    }

    private fun logCanceledAlert() {
        val user = _currentUser.value ?: return
        val reason = pendingAlertReason ?: "Unknown"
        val type = determineRuleType(reason)

        val alertLog = SafetyAlert(
            id = db.collection("alerts").document().id,
            protectedId = user.id,
            ruleType = type,
            timestamp = Timestamp.now(),
            location = user.lastLocation,
            status = "CANCELED",
            cancelReason = "Canceled by user: $reason"
        )
        db.collection("alerts").document(alertLog.id).set(alertLog)
    }

    fun executeFinalAlert(videoUrl: String? = null) {
        _showCountdown.value = false
        val user = _currentUser.value ?: return
        val location = user.lastLocation
        val reason = pendingAlertReason ?: "Emergency"
        val type = determineRuleType(reason)

        if (currentAlertId != null && videoUrl != null) {
            db.collection("alerts").document(currentAlertId!!).update("videoUrl", videoUrl)
            return
        }

        val newAlertId = db.collection("alerts").document().id
        currentAlertId = newAlertId
        val alert = SafetyAlert(
            id = newAlertId,
            protectedId = user.id,
            ruleType = type,
            timestamp = Timestamp.now(),
            location = location,
            status = "ACTIVE",
            cancelReason = reason,
            videoUrl = videoUrl
        )
        db.collection("alerts").document(alert.id).set(alert)
    }

    fun startPanicButtonSequence() { triggerCountdown("Panic Button Pressed") }

    fun createGeofenceRule(protectedId: String, center: GeoPoint, radius: Double) {
        val monitorId = auth.currentUser?.uid ?: return
        val rule = SafetyRule(
            id = db.collection("rules").document().id,
            monitorId = monitorId,
            protectedId = protectedId,
            type = RuleType.GEOFENCING,
            isActive = true,
            name = "Safe Zone",
            geofenceCenter = center,
            geofenceRadiusMeters = radius
        )
        saveRule(rule)
    }

    fun dismissAlert(alertId: String) {
        db.collection("alerts").document(alertId).update("status", "RESOLVED")
    }

    fun signOut() {
        monitorAlertsListener?.remove()
        rulesListener?.remove()
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Idle
        myRules = emptyList()
        isAlertActive = false
        _showCountdown.value = false
        currentAlertId = null
    }

    override fun onCleared() {
        super.onCleared()
        monitorAlertsListener?.remove()
        rulesListener?.remove()
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}