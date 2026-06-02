package com.example.transportapp.data.session

import android.content.Context
import com.example.transportapp.domain.model.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("refa_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_LAST_ACTIVE        = "last_active_ms"
        private const val KEY_IS_DRIVER_MODE     = "is_driver_mode"
        private const val KEY_DRIVER_SWITCHED_AT = "driver_switched_at_ms"
        private const val KEY_CACHED_USER        = "cached_user_json"
        private const val KEY_POLICY_PENDING     = "policy_pending"
        private const val SESSION_DURATION_MS    = 2 * 60 * 60 * 1000L   // 2 hours
        const val DRIVER_LOCK_DAYS               = 15L
        private const val DRIVER_LOCK_MS         = DRIVER_LOCK_DAYS * 24 * 60 * 60 * 1000L
    }

    fun refreshActivity() {
        prefs.edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply()
    }

    fun isSessionValid(): Boolean {
        val lastActive = prefs.getLong(KEY_LAST_ACTIVE, 0L)
        if (lastActive == 0L) return false
        return System.currentTimeMillis() - lastActive < SESSION_DURATION_MS
    }

    fun saveDriverMode(isDriver: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DRIVER_MODE, isDriver).apply()
    }

    fun isDriverMode(): Boolean = prefs.getBoolean(KEY_IS_DRIVER_MODE, false)

    /** Records the exact epoch-ms when the driver mode was first activated on this device.
     *  Only called once when the user registers as a driver or explicitly switches to driver mode. */
    fun recordDriverSwitchTimestamp() {
        if (prefs.getLong(KEY_DRIVER_SWITCHED_AT, 0L) == 0L) {
            prefs.edit().putLong(KEY_DRIVER_SWITCHED_AT, System.currentTimeMillis()).apply()
        }
    }

    fun getDriverSwitchedAtMs(): Long = prefs.getLong(KEY_DRIVER_SWITCHED_AT, 0L)

    /** Returns true if the 15-day lock period has NOT yet passed. */
    fun isDriverLockActive(): Boolean {
        val switchedAt = getDriverSwitchedAtMs()
        if (switchedAt == 0L) return false
        return System.currentTimeMillis() - switchedAt < DRIVER_LOCK_MS
    }

    /** Days remaining in the 15-day lock (0 if lock expired or never set). */
    fun driverLockDaysRemaining(): Int {
        val switchedAt = getDriverSwitchedAtMs()
        if (switchedAt == 0L) return 0
        val elapsed = System.currentTimeMillis() - switchedAt
        if (elapsed >= DRIVER_LOCK_MS) return 0
        return ((DRIVER_LOCK_MS - elapsed) / (24 * 60 * 60 * 1000L)).toInt() + 1
    }

    // ── Policy flow ──────────────────────────────────────────────────────────

    fun setPolicyPending(pending: Boolean) {
        prefs.edit().putBoolean(KEY_POLICY_PENDING, pending).apply()
    }

    fun isPolicyPending(): Boolean = prefs.getBoolean(KEY_POLICY_PENDING, false)

    // ── User cache (instant profile load on repeat launches) ─────────────────

    /** Store a JSON snapshot of the User so the profile screen can display data
     *  immediately from cache while Firestore listener fires in the background. */
    fun cacheUser(user: User) {
        try {
            prefs.edit().putString(KEY_CACHED_USER, gson.toJson(user)).apply()
        } catch (_: Exception) { }
    }

    fun getCachedUser(): User? {
        return try {
            val json = prefs.getString(KEY_CACHED_USER, null) ?: return null
            gson.fromJson(json, User::class.java)
        } catch (_: Exception) { null }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
