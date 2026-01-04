package pt.isec.a2022136610.safetysec.viewmodel

import android.app.Application
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.isec.a2022136610.safetysec.R
import pt.isec.a2022136610.safetysec.model.RuleStatus
import pt.isec.a2022136610.safetysec.model.RuleType
import pt.isec.a2022136610.safetysec.model.SafetyAlert
import pt.isec.a2022136610.safetysec.model.SafetyRule
import pt.isec.a2022136610.safetysec.model.UserProfile
import pt.isec.a2022136610.safetysec.model.UserRole
import java.util.Calendar
import kotlin.random.Random

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
    private fun getString(resId: Int, vararg args: Any): String = getApplication<Application>().getString(resId, *args)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser

    private val _generatedCode = MutableStateFlow<String?>(null)
    val generatedCode: StateFlow<String?> = _generatedCode

    // List of people I am monitoring (Protégés)
    private val _associatedUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val associatedUsers: StateFlow<List<UserProfile>> = _associatedUsers

    // List of people monitoring ME (Monitors)
    private val _associatedMonitors = MutableStateFlow<List<UserProfile>>(emptyList())
    val associatedMonitors: StateFlow<List<UserProfile>> = _associatedMonitors

    private val _targetUser = MutableStateFlow<UserProfile?>(null)
    val targetUser: StateFlow<UserProfile?> = _targetUser

    private val _alertHistory = MutableStateFlow<List<SafetyAlert>>(emptyList())
    val alertHistory: StateFlow<List<SafetyAlert>> = _alertHistory

    private val _monitorAlerts = MutableStateFlow<List<SafetyAlert>>(emptyList())
    val monitorAlerts: StateFlow<List<SafetyAlert>> = _monitorAlerts

    private val _selectedAlert = MutableStateFlow<SafetyAlert?>(null)
    val selectedAlert: StateFlow<SafetyAlert?> = _selectedAlert

    private val _selectedUserRules = MutableStateFlow<List<SafetyRule>>(emptyList())
    val selectedUserRules: StateFlow<List<SafetyRule>> = _selectedUserRules

    // Internal active rules for sensor logic
    private var myRules: List<SafetyRule> = emptyList()

    // Exposed active rules for UI (Active Rules Screen)
    private val _myActiveRules = MutableStateFlow<List<SafetyRule>>(emptyList())
    val myActiveRules: StateFlow<List<SafetyRule>> = _myActiveRules

    // Pending rules for approval (Protected Dashboard)
    private val _pendingRules = MutableStateFlow<List<SafetyRule>>(emptyList())
    val pendingRules: StateFlow<List<SafetyRule>> = _pendingRules

    private val _showCountdown = MutableStateFlow(false)
    val showCountdown: StateFlow<Boolean> = _showCountdown

    private var pendingAlertReason: String? = null
    private var isAlertActive = false
    private var currentAlertId: String? = null

    private var lastMovementTime: Long = System.currentTimeMillis()
    private var lastKnownLocation: Location? = null

    private var monitorAlertsListener: ListenerRegistration? = null
    private var rulesListener: ListenerRegistration? = null
    private var pendingRulesListener: ListenerRegistration? = null

    init {
        // Auto-load if user is already logged in
        if (auth.currentUser != null) {
            loadCurrentUser()
        }
    }

    // --- LOGIN ---
    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                _authState.value = AuthState.Success
                                loadCurrentUser()
                            } else {
                                auth.signOut()
                                _authState.value = AuthState.Error(getString(R.string.err_account_not_found))
                            }
                        }
                        .addOnFailureListener {
                            auth.signOut()
                            _authState.value = AuthState.Error(getString(R.string.err_db_connection))
                        }
                }
            }
            .addOnFailureListener { _authState.value = AuthState.Error(getString(R.string.login_error)) }
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
                    .addOnFailureListener { _authState.value = AuthState.Error(getString(R.string.save_profile_error)) }
            }
            .addOnFailureListener { _authState.value = AuthState.Error(getString(R.string.reg_error)) }
    }

    // --- LOAD DATA ---
    fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(UserProfile::class.java)
                    _currentUser.value = user

                    if (user != null) {
                        // Load Proteges
                        if (user.protectedIds.isNotEmpty()) {
                            fetchAssociatedUsers(user.protectedIds)
                            fetchMonitorAlerts(user.protectedIds)
                        } else { _associatedUsers.value = emptyList() }

                        // Load Monitors
                        if (user.monitorIds.isNotEmpty()) {
                            fetchAssociatedMonitors(user.monitorIds)
                        } else { _associatedMonitors.value = emptyList() }

                        // Listeners for Protected side
                        if (user.role == UserRole.PROTECTED || user.role == UserRole.BOTH) {
                            startListeningToRules(user.id)
                            startListeningToPendingRules(user.id)
                        }
                    }
                }
            }
    }

    // --- DISASSOCIATE ---
    fun disassociateUsers(monitorId: String, protectedId: String) {
        val batch = db.batch()
        batch.update(db.collection("users").document(monitorId), "protectedIds", FieldValue.arrayRemove(protectedId))
        batch.update(db.collection("users").document(protectedId), "monitorIds", FieldValue.arrayRemove(monitorId))
        batch.commit().addOnSuccessListener {
            loadCurrentUser()
            Toast.makeText(getApplication(), "Association Removed", Toast.LENGTH_SHORT).show()
        }
    }

    // --- UPDATE PROFILE ---
    fun updateProfile(newName: String, newPass: String, currentPass: String) {
        val user = auth.currentUser ?: return
        val userEmail = user.email ?: return

        fun performUpdates() {
            if (newName.isNotBlank() && newName != _currentUser.value?.name) {
                db.collection("users").document(user.uid).update("name", newName)
                    .addOnSuccessListener { _currentUser.value = _currentUser.value?.copy(name = newName) }
            }
            if (newPass.isNotBlank()) user.updatePassword(newPass)
            Toast.makeText(getApplication(), getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT).show()
        }

        if (newPass.isNotBlank()) {
            if (currentPass.isBlank()) { _authState.value = AuthState.Error(getString(R.string.err_current_pass_required)); return }
            val credential = EmailAuthProvider.getCredential(userEmail, currentPass)
            user.reauthenticate(credential)
                .addOnSuccessListener { performUpdates() }
                .addOnFailureListener { _authState.value = AuthState.Error(getString(R.string.err_verification)) }
        } else {
            performUpdates()
        }
    }

    fun updateCancelPin(newPin: String) {
        val userId = auth.currentUser?.uid ?: return
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) { _authState.value = AuthState.Error(getString(R.string.err_pin_digits)); return }
        db.collection("users").document(userId).update("cancelPin", newPin)
            .addOnSuccessListener {
                _currentUser.value = _currentUser.value?.copy(cancelPin = newPin)
                Toast.makeText(getApplication(), "PIN Updated", Toast.LENGTH_SHORT).show()
            }
    }

    // --- FETCH LISTS ---
    private fun fetchAssociatedUsers(ids: List<String>) {
        if (ids.isEmpty()) return
        val safeIds = ids.take(10)
        db.collection("users").whereIn(FieldPath.documentId(), safeIds).get()
            .addOnSuccessListener { _associatedUsers.value = it.toObjects(UserProfile::class.java) }
            .addOnFailureListener { Log.e("AUTH", "Error fetching proteges: ${it.message}") }
    }

    private fun fetchAssociatedMonitors(ids: List<String>) {
        if (ids.isEmpty()) return
        val safeIds = ids.take(10)
        db.collection("users").whereIn(FieldPath.documentId(), safeIds).get()
            .addOnSuccessListener { _associatedMonitors.value = it.toObjects(UserProfile::class.java) }
            .addOnFailureListener { Log.e("AUTH", "Error fetching monitors: ${it.message}") }
    }

    fun loadTargetUser(targetId: String) {
        db.collection("users").document(targetId).get().addOnSuccessListener { if (it.exists()) _targetUser.value = it.toObject(UserProfile::class.java) }
    }

    // --- RULES MANAGEMENT (MONITOR) ---
    fun loadRulesForUser(protectedId: String) {
        db.collection("rules").whereEqualTo("protectedId", protectedId).get()
            .addOnSuccessListener { result -> _selectedUserRules.value = result.toObjects(SafetyRule::class.java) }
    }

    fun saveRule(rule: SafetyRule) {
        val ruleId = if (rule.id.isEmpty()) db.collection("rules").document().id else rule.id

        // Ensure Monitor ID is set
        val currentUserId = auth.currentUser?.uid ?: ""
        val finalMonitorId = if (rule.monitorId.isNotEmpty()) rule.monitorId else currentUserId

        val ruleData = rule.copy(
            id = ruleId,
            monitorId = finalMonitorId,
            status = RuleStatus.PENDING,
            isActive = true
        )

        db.collection("rules").document(ruleId).set(ruleData)
            .addOnSuccessListener {
                loadRulesForUser(rule.protectedId)
            }
    }

    // --- RULES APPROVAL (PROTECTED) ---
    fun approveRule(ruleId: String) {
        val updates = mapOf(
            "status" to RuleStatus.ACTIVE.name,
            "isActive" to true
        )
        db.collection("rules").document(ruleId).update(updates)
            .addOnSuccessListener { Toast.makeText(getApplication(), "Rule Approved", Toast.LENGTH_SHORT).show() }
    }

    fun rejectRule(ruleId: String) {
        db.collection("rules").document(ruleId).delete()
            .addOnSuccessListener { Toast.makeText(getApplication(), "Rule Rejected", Toast.LENGTH_SHORT).show() }
    }

    fun revokeRule(ruleId: String) {
        db.collection("rules").document(ruleId).delete()
            .addOnSuccessListener { Toast.makeText(getApplication(), "Rule Revoked", Toast.LENGTH_SHORT).show() }
    }

    private fun startListeningToRules(userId: String) {
        rulesListener?.remove()
        // Listen only for ACTIVE, APPROVED rules
        rulesListener = db.collection("rules")
            .whereEqualTo("protectedId", userId)
            .whereEqualTo("status", RuleStatus.ACTIVE.name)
            .addSnapshotListener { snapshots, e ->
                if (snapshots != null) {
                    val rules = snapshots.toObjects(SafetyRule::class.java)
                    myRules = rules
                    _myActiveRules.value = rules
                }
            }
    }

    private fun startListeningToPendingRules(userId: String) {
        pendingRulesListener?.remove()
        pendingRulesListener = db.collection("rules")
            .whereEqualTo("protectedId", userId)
            .whereEqualTo("status", RuleStatus.PENDING.name)
            .addSnapshotListener { snapshots, e ->
                if (snapshots != null) _pendingRules.value = snapshots.toObjects(SafetyRule::class.java)
            }
    }

    // --- ASSOCIATION ---
    fun generateAssociationCode() {
        val code = Random.nextInt(100000, 999999).toString()
        val userId = auth.currentUser?.uid ?: return
        db.collection("codes").document(code).set(hashMapOf("protectedId" to userId, "timestamp" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { _generatedCode.value = code }
    }

    fun connectWithProtege(code: String) {
        val monitorId = auth.currentUser?.uid ?: return
        db.collection("codes").document(code).get().addOnSuccessListener {
            val protectedId = it.getString("protectedId")
            if (protectedId != null && protectedId != monitorId) performAssociation(monitorId, protectedId, code)
            else _authState.value = AuthState.Error(if (protectedId == monitorId) getString(R.string.err_monitor_self) else getString(R.string.err_invalid_code))
        }.addOnFailureListener { _authState.value = AuthState.Error(getString(R.string.err_verification)) }
    }

    private fun performAssociation(monitorId: String, protectedId: String, codeUsed: String) {
        val batch = db.batch()
        batch.update(db.collection("users").document(protectedId), "monitorIds", FieldValue.arrayUnion(monitorId))
        batch.update(db.collection("users").document(monitorId), "protectedIds", FieldValue.arrayUnion(protectedId))
        batch.delete(db.collection("codes").document(codeUsed))
        batch.commit().addOnSuccessListener { _authState.value = AuthState.Success; loadCurrentUser() }
    }

    // --- ALERTS & HISTORY ---
    fun fetchAlertHistory() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("alerts").whereEqualTo("protectedId", userId).limit(50).get()
            .addOnSuccessListener { result -> _alertHistory.value = result.toObjects(SafetyAlert::class.java).filter { it.status != "CANCELED" }.sortedByDescending { it.timestamp } }
    }

    fun fetchMonitorAlerts(protectedIds: List<String>) {
        if (protectedIds.isEmpty()) return
        monitorAlertsListener?.remove()
        monitorAlertsListener = db.collection("alerts").whereIn("protectedId", protectedIds.take(10)).addSnapshotListener { snapshots, e ->
            if (snapshots != null) _monitorAlerts.value = snapshots.toObjects(SafetyAlert::class.java).filter { it.status != "CANCELED" }.sortedByDescending { it.timestamp }
        }
    }

    fun dismissAlert(alertId: String) { db.collection("alerts").document(alertId).update("status", "RESOLVED") }
    fun getAlert(alertId: String) { db.collection("alerts").document(alertId).addSnapshotListener { doc, _ -> if (doc != null) _selectedAlert.value = doc.toObject(SafetyAlert::class.java) } }

    // --- SENSOR LOGIC ---
    private fun isRuleActiveNow(rule: SafetyRule): Boolean {
        if (rule.status != RuleStatus.ACTIVE) return false
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        if (rule.activeDays != null && rule.activeDays.isNotEmpty() && !rule.activeDays.contains(currentDay)) return false
        if (rule.startTime != null && rule.endTime != null) {
            try {
                val (startH, startM) = rule.startTime.split(":").map { it.toInt() }
                val (endH, endM) = rule.endTime.split(":").map { it.toInt() }
                val nowMins = currentHour * 60 + currentMinute
                val startMins = startH * 60 + startM
                val endMins = endH * 60 + endM
                if (startMins <= endMins) { if (nowMins < startMins || nowMins > endMins) return false }
                else { if (nowMins < startMins && nowMins > endMins) return false }
            } catch (e: Exception) { Log.e("RULES", "Time parse error") }
        }
        return true
    }

    fun handleFallDetected() {
        if (isAlertActive) return
        val activeRule = myRules.firstOrNull { it.type == RuleType.FALL_DETECTION && isRuleActiveNow(it) }
        triggerCountdown(if (activeRule != null) getString(R.string.reason_fall) + " (${activeRule.name})" else getString(R.string.reason_fall) + " (${getString(R.string.reason_test)})")
    }

    fun handleAccidentDetected() {
        if (isAlertActive) return
        val activeRule = myRules.firstOrNull { it.type == RuleType.ACCIDENT && isRuleActiveNow(it) }
        triggerCountdown(if (activeRule != null) getString(R.string.reason_accident) + " (${activeRule.name})" else getString(R.string.reason_accident) + " (${getString(R.string.reason_test)})")
    }

    fun updateUserLocation(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).update("lastLocation", GeoPoint(location.latitude, location.longitude))
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
                Location.distanceBetween(location.latitude, location.longitude, rule.geofenceCenter.latitude, rule.geofenceCenter.longitude, results)
                if (results[0] > rule.geofenceRadiusMeters) { triggerCountdown(getString(R.string.reason_zone, rule.name)); return }
            }
        }
    }

    private fun checkSpeedRules(location: Location) {
        val currentSpeedKmh = location.speed * 3.6
        for (rule in myRules) {
            if (rule.type == RuleType.MAX_SPEED && rule.maxSpeedKmh != null) {
                if (!isRuleActiveNow(rule)) continue
                if (currentSpeedKmh > rule.maxSpeedKmh) { triggerCountdown(getString(R.string.reason_speed, String.format("%.1f", currentSpeedKmh))); return }
            }
        }
    }

    private fun checkInactivityRules(currentLocation: Location) {
        val now = System.currentTimeMillis()
        if (lastKnownLocation != null && currentLocation.distanceTo(lastKnownLocation!!) > 100) lastMovementTime = now
        lastKnownLocation = currentLocation
        for (rule in myRules) {
            if (rule.type == RuleType.INACTIVITY && rule.inactivityTimeMinutes != null) {
                if (!isRuleActiveNow(rule)) continue
                val inactiveDurationMinutes = (now - lastMovementTime) / 60000
                if (inactiveDurationMinutes >= rule.inactivityTimeMinutes) { triggerCountdown(getString(R.string.reason_inactivity, inactiveDurationMinutes)); lastMovementTime = now; return }
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
            val user = _currentUser.value ?: return
            val alertLog = SafetyAlert(id = db.collection("alerts").document().id, protectedId = user.id, ruleType = determineRuleType(pendingAlertReason ?: ""), timestamp = Timestamp.now(), location = user.lastLocation, status = "CANCELED", cancelReason = "Canceled by user: $pendingAlertReason")
            db.collection("alerts").document(alertLog.id).set(alertLog)
            _showCountdown.value = false
            isAlertActive = false
            pendingAlertReason = null
        }
    }

    private fun determineRuleType(reason: String): RuleType {
        return when {
            reason.contains("ZONE", true) -> RuleType.GEOFENCING
            reason.contains("FALL", true) -> RuleType.FALL_DETECTION
            reason.contains("ACCIDENT", true) -> RuleType.ACCIDENT
            reason.contains("SPEED", true) -> RuleType.MAX_SPEED
            reason.contains("INACTIVITY", true) -> RuleType.INACTIVITY
            else -> RuleType.PANIC_BUTTON
        }
    }

    fun executeFinalAlert(videoUrl: String? = null) {
        _showCountdown.value = false
        val user = _currentUser.value ?: return
        val reason = pendingAlertReason ?: getString(R.string.reason_emergency)
        if (currentAlertId != null && videoUrl != null) { db.collection("alerts").document(currentAlertId!!).update("videoUrl", videoUrl); return }
        val newAlertId = db.collection("alerts").document().id
        currentAlertId = newAlertId
        val alert = SafetyAlert(id = newAlertId, protectedId = user.id, ruleType = determineRuleType(reason), timestamp = Timestamp.now(), location = user.lastLocation, status = "ACTIVE", cancelReason = reason, videoUrl = videoUrl)
        db.collection("alerts").document(alert.id).set(alert)
    }

    fun startPanicButtonSequence() { triggerCountdown(getString(R.string.reason_panic)) }

    fun createGeofenceRule(protectedId: String, center: GeoPoint, radius: Double) {
        val monitorId = auth.currentUser?.uid ?: return
        val existingRule = _selectedUserRules.value.find { it.type == RuleType.GEOFENCING }
        val ruleId = existingRule?.id ?: db.collection("rules").document().id

        // Inherit time from other rules if Geofence is new/updated
        var startT = existingRule?.startTime
        var endT = existingRule?.endTime
        if (startT == null) {
            val other = _selectedUserRules.value.firstOrNull { it.startTime != null }
            startT = other?.startTime
            endT = other?.endTime
        }

        val rule = SafetyRule(
            id = ruleId,
            monitorId = monitorId,
            protectedId = protectedId,
            type = RuleType.GEOFENCING,
            isActive = true,
            name = "Safe Zone",
            geofenceCenter = center,
            geofenceRadiusMeters = radius,
            startTime = startT,
            endTime = endT,
            status = RuleStatus.PENDING
        )
        saveRule(rule)
    }

    fun signOut() {
        monitorAlertsListener?.remove()
        rulesListener?.remove()
        pendingRulesListener?.remove()
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
        pendingRulesListener?.remove()
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}