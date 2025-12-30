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

    private var myRules: List<SafetyRule> = emptyList()

    // --- ESTADOS DE ALERTA ---
    private val _showCountdown = MutableStateFlow(false)
    val showCountdown: StateFlow<Boolean> = _showCountdown

    private var pendingAlertReason: String? = null
    private var isAlertActive = false
    private var currentAlertId: String? = null

    // --- CONTROLO DE INATIVIDADE ---
    private var lastMovementTime: Long = System.currentTimeMillis()
    private var lastKnownLocation: Location? = null

    // --- LOGIN ---
    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _authState.value = AuthState.Success
                loadCurrentUser()
            }
            .addOnFailureListener { e -> _authState.value = AuthState.Error(e.message ?: "Erro") }
    }

    // --- REGISTO ---
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
                    .addOnFailureListener { _authState.value = AuthState.Error("Erro ao guardar") }
            }
            .addOnFailureListener { e -> _authState.value = AuthState.Error(e.message ?: "Erro") }
    }

    // --- CARREGAR DADOS ---
    fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(UserProfile::class.java)
                    _currentUser.value = user

                    if (user != null && user.protectedIds.isNotEmpty()) {
                        fetchAssociatedUsers(user.protectedIds)
                    }

                    if (user != null && (user.role == UserRole.PROTECTED || user.role == UserRole.BOTH)) {
                        startListeningToRules(user.id)
                    }
                }
            }
    }

    private fun startListeningToRules(userId: String) {
        db.collection("rules")
            .whereEqualTo("protectedId", userId)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    myRules = snapshots.toObjects(SafetyRule::class.java)
                    Log.d("RULES", "Regras carregadas: ${myRules.size}")
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

    // --- ASSOCIAÇÃO ---
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
                    _authState.value = AuthState.Error("Código inválido")
                }
            }
            .addOnFailureListener { _authState.value = AuthState.Error("Erro ao verificar") }
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

    // --- REGRAS DE MONITORIZAÇÃO ---

    private fun isRuleActiveNow(rule: SafetyRule): Boolean {
        if (!rule.isActive) return false

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentDay = now.get(Calendar.DAY_OF_WEEK) // 1=Dom, 7=Sab

        // Verificar Dias da Semana
        if (rule.activeDays != null && rule.activeDays.isNotEmpty()) {
            if (!rule.activeDays.contains(currentDay)) return false
        }

        // Verificar Janela Horária (HH:mm)
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
                Log.e("RULES", "Erro ao fazer parse das horas: ${e.message}")
            }
        }
        return true
    }

    // --- EVENTOS DE SENSORES (QUEDA / ACIDENTE) ---

    fun handleFallDetected() {
        if (isAlertActive) return

        // 1. Tenta encontrar uma regra válida
        val activeRule = myRules.firstOrNull { it.type == RuleType.FALL_DETECTION && isRuleActiveNow(it) }

        // 2. CORREÇÃO: Se existir regra, usa o nome dela. Se NÃO existir, dispara em modo DEBUG.
        if (activeRule != null) {
            triggerCountdown("QUEDA DETETADA (Regra: ${activeRule.name})")
        } else {
            // DEBUG APENAS: Para testar sem ter de criar regras na DB
            Log.w("DEBUG", "Queda detetada pelo sensor, mas sem regra definida. A disparar alerta de teste.")
            triggerCountdown("QUEDA DETETADA (Teste/Sem Regra)")
        }
    }

    fun handleAccidentDetected() {
        if (isAlertActive) return
        val activeRule = myRules.firstOrNull { it.type == RuleType.ACCIDENT && isRuleActiveNow(it) }

        if (activeRule != null) {
            triggerCountdown("ACIDENTE DETETADO (Regra: ${activeRule.name})")
        } else {
            // DEBUG APENAS
            Log.w("DEBUG", "Acidente detetado pelo sensor, mas sem regra definida. A disparar alerta de teste.")
            triggerCountdown("ACIDENTE DETETADO (Teste/Sem Regra)")
        }
    }

    // --- GPS UPDATE E VERIFICAÇÕES DE LOCALIZAÇÃO/VELOCIDADE/INATIVIDADE ---

    fun updateUserLocation(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        // Atualizar DB
        db.collection("users").document(userId).update("lastLocation", geoPoint)
            .addOnFailureListener { }

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
                val distanceInMeters = results[0]

                if (distanceInMeters > rule.geofenceRadiusMeters) {
                    triggerCountdown(reason = "SAIU DA ZONA SEGURA: ${rule.name}")
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
                    triggerCountdown(reason = "EXCESSO DE VELOCIDADE: ${String.format("%.1f", currentSpeedKmh)} km/h")
                    return
                }
            }
        }
    }

    private fun checkInactivityRules(currentLocation: Location) {
        val now = System.currentTimeMillis()

        if (lastKnownLocation != null) {
            val distance = currentLocation.distanceTo(lastKnownLocation!!)
            if (distance > 10) {
                lastMovementTime = now
            }
        }
        lastKnownLocation = currentLocation

        for (rule in myRules) {
            if (rule.type == RuleType.INACTIVITY && rule.inactivityTimeMinutes != null) {
                if (!isRuleActiveNow(rule)) continue

                val inactiveDurationMinutes = (now - lastMovementTime) / 60000

                if (inactiveDurationMinutes >= rule.inactivityTimeMinutes) {
                    triggerCountdown(reason = "INATIVIDADE PROLONGADA: ${inactiveDurationMinutes} min")
                    lastMovementTime = now
                    return
                }
            }
        }
    }

    // --- GESTÃO DE ALERTAS ---

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
        } else {
            Log.d("ALERTA", "PIN errado.")
        }
    }

    private fun determineRuleType(reason: String): RuleType {
        return when {
            reason.contains("ZONA", ignoreCase = true) -> RuleType.GEOFENCING
            reason.contains("QUEDA", ignoreCase = true) -> RuleType.FALL_DETECTION
            reason.contains("ACIDENTE", ignoreCase = true) -> RuleType.ACCIDENT
            reason.contains("VELOCIDADE", ignoreCase = true) -> RuleType.MAX_SPEED
            reason.contains("INATIVIDADE", ignoreCase = true) -> RuleType.INACTIVITY
            else -> RuleType.PANIC_BUTTON
        }
    }

    private fun logCanceledAlert() {
        val user = _currentUser.value ?: return
        val reason = pendingAlertReason ?: "Desconhecido"
        val type = determineRuleType(reason)

        val alertLog = SafetyAlert(
            id = db.collection("alerts").document().id,
            protectedId = user.id,
            ruleType = type,
            timestamp = Timestamp.now(),
            location = user.lastLocation,
            status = "CANCELED",
            cancelReason = "Cancelado pelo utilizador: $reason"
        )

        db.collection("alerts").document(alertLog.id).set(alertLog)
    }

    fun executeFinalAlert(videoUrl: String? = null) {
        _showCountdown.value = false

        val user = _currentUser.value ?: return
        val location = user.lastLocation
        val reason = pendingAlertReason ?: "Emergência"
        val type = determineRuleType(reason)

        if (currentAlertId != null && videoUrl != null) {
            db.collection("alerts").document(currentAlertId!!).update("videoUrl", videoUrl)
                .addOnSuccessListener { Log.d("ALERTA", "Vídeo anexado ao alerta!") }
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
            .addOnSuccessListener {
                Log.d("ALERTA", "Alerta enviado! A aguardar vídeo...")
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error("Falha ao enviar SOS")
            }
    }

    fun startPanicButtonSequence() {
        triggerCountdown("Botão de Pânico Pressionado")
    }

    fun createGeofenceRule(protectedId: String, center: GeoPoint, radius: Double) {
        val monitorId = auth.currentUser?.uid ?: return

        val rule = SafetyRule(
            id = db.collection("rules").document().id,
            monitorId = monitorId,
            protectedId = protectedId,
            type = RuleType.GEOFENCING,
            isActive = true,
            name = "Zona Segura",
            geofenceCenter = center,
            geofenceRadiusMeters = radius
        )

        db.collection("rules").add(rule)
    }

    fun dismissAlert(alertId: String) {
        db.collection("alerts").document(alertId).update("status", "RESOLVED")
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Idle
        myRules = emptyList()
        isAlertActive = false
        _showCountdown.value = false
        currentAlertId = null
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}