package com.example.data

import android.content.Context
import com.example.xmp.XmpParser
import com.example.xmp.XmpPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class PresetRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.presetDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedBuiltInsIfEmpty()
        }
    }

    private suspend fun seedBuiltInsIfEmpty() {
        val count = dao.getPresetById(BuiltInPresets.all.first().id)
        if (count == null) {
            val entities = BuiltInPresets.all.map { PresetEntity.fromDomain(it) }
            dao.insertAll(entities)
        }
    }

    fun getAllPresets(): Flow<List<XmpPreset>> {
        return dao.getAllPresets().map { list ->
            // Put Default Original first, followed by others
            val mapped = list.map { it.toDomain() }
            listOf(XmpPreset.DEFAULT) + mapped
        }
    }

    suspend fun importXmp(xmlContent: String, fileName: String, category: String = "Custom XMP"): XmpPreset {
        val parsed = XmpParser.parse(xmlContent, fileName.removeSuffix(".xmp"))
        val preset = parsed.copy(
            id = UUID.randomUUID().toString(),
            category = category,
            isBuiltIn = false,
            rawXml = xmlContent
        )
        dao.insert(PresetEntity.fromDomain(preset))
        return preset
    }

    suspend fun savePreset(preset: XmpPreset) {
        val xml = if (preset.rawXml.isBlank()) XmpParser.toXml(preset) else preset.rawXml
        dao.insert(PresetEntity.fromDomain(preset.copy(rawXml = xml)))
    }

    suspend fun updatePreset(preset: XmpPreset) {
        dao.update(PresetEntity.fromDomain(preset))
    }

    suspend fun deletePreset(id: String) {
        dao.deleteById(id)
    }

    suspend fun duplicatePreset(preset: XmpPreset): XmpPreset {
        val newPreset = preset.copy(
            id = UUID.randomUUID().toString(),
            name = "${preset.name} Copy",
            isBuiltIn = false,
            category = "Custom XMP"
        )
        dao.insert(PresetEntity.fromDomain(newPreset))
        return newPreset
    }
}
