package br.com.smarttarget.android.controller.domain

data class ProfileUpdateResult(
    val profiles: List<ControlProfile>,
    val activeProfileId: String
)

object ProfileOperations {

    fun upsertProfile(
        existing: List<ControlProfile>,
        candidate: ControlProfile
    ): ProfileUpdateResult {
        val normalizedName = candidate.name.trim().ifBlank { "Perfil Personalizado" }
        val sanitized = candidate.copy(
            name = normalizedName,
            pointerSensitivity = candidate.pointerSensitivity.coerceIn(0.4f, 3.5f),
            scrollSensitivity = candidate.scrollSensitivity.coerceIn(0.4f, 3.5f),
            airMouseSensitivity = candidate.airMouseSensitivity.coerceIn(0.4f, 3.5f)
        )
        val index = existing.indexOfFirst { it.id == sanitized.id }
        val updated = if (index >= 0) {
            existing.toMutableList().apply { set(index, sanitized) }
        } else {
            existing + sanitized
        }
        return ProfileUpdateResult(updated, sanitized.id)
    }

    fun deleteProfile(
        existing: List<ControlProfile>,
        profileId: String,
        currentActiveId: String
    ): ProfileUpdateResult {
        if (existing.size <= 1) {
            return ProfileUpdateResult(existing, currentActiveId)
        }
        val filtered = existing.filterNot { it.id == profileId }
        val nextActive = if (currentActiveId == profileId || filtered.none { it.id == currentActiveId }) {
            filtered.first().id
        } else {
            currentActiveId
        }
        return ProfileUpdateResult(filtered, nextActive)
    }

    fun duplicateProfile(
        existing: List<ControlProfile>,
        sourceId: String,
        newId: String
    ): ProfileUpdateResult {
        val source = existing.firstOrNull { it.id == sourceId }
            ?: return ProfileUpdateResult(existing, existing.first().id)
        val copy = source.copy(
            id = newId,
            name = "${source.name} (Cópia)"
        )
        return ProfileUpdateResult(existing + copy, copy.id)
    }
}
