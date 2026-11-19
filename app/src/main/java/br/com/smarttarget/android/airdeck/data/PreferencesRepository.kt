package br.com.smarttarget.android.airdeck.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.smarttarget.android.airdeck.domain.ControlProfile
import br.com.smarttarget.android.airdeck.domain.ControlProfileCodec
import br.com.smarttarget.android.airdeck.domain.ProfilePresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "air_deck_prefs"
)

data class AppPreferencesState(
    val powerSaverMode: Boolean = false,
    val profiles: List<ControlProfile> = ProfilePresets.defaultProfiles(),
    val activeProfileId: String = ProfilePresets.defaultProfiles().first().id
) {
    val activeProfile: ControlProfile
        get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()
}

interface SettingsStore {
    val stateFlow: Flow<AppPreferencesState>
    suspend fun setPowerSaverMode(enabled: Boolean)
    suspend fun setActiveProfileId(profileId: String)
    suspend fun saveProfiles(profiles: List<ControlProfile>, activeProfileId: String)
}

class PreferencesRepository(private val context: Context) : SettingsStore {

    private object Keys {
        val POWER_SAVER_MODE = booleanPreferencesKey("power_saver_mode")
        val PROFILES_BLOB = stringPreferencesKey("control_profiles_blob")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }

    override val stateFlow: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        val decoded = prefs[Keys.PROFILES_BLOB]?.let { ControlProfileCodec.decodeList(it) }
        val profiles = if (decoded.isNullOrEmpty()) ProfilePresets.defaultProfiles() else decoded
        val storedActive = prefs[Keys.ACTIVE_PROFILE_ID]
        val activeId = if (storedActive != null && profiles.any { it.id == storedActive }) {
            storedActive
        } else {
            profiles.first().id
        }
        AppPreferencesState(
            powerSaverMode = prefs[Keys.POWER_SAVER_MODE] ?: false,
            profiles = profiles,
            activeProfileId = activeId
        )
    }

    override suspend fun setPowerSaverMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.POWER_SAVER_MODE] = enabled }
    }

    override suspend fun setActiveProfileId(profileId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_PROFILE_ID] = profileId }
    }

    override suspend fun saveProfiles(profiles: List<ControlProfile>, activeProfileId: String) {
        context.dataStore.edit {
            it[Keys.PROFILES_BLOB] = ControlProfileCodec.encodeList(profiles)
            it[Keys.ACTIVE_PROFILE_ID] = activeProfileId
        }
    }
}
