package dev.slne.surf.api.gen.data

import kotlinx.serialization.Serializable

@Serializable
data class AdvancementRegistry(val categories: List<AdvancementCategory>)