package com.example.empire.data

import android.content.Context
import org.json.JSONObject
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight save system using SharedPreferences + JSON.
 * Persists core progression, resources, map, player position, and unlock flags.
 */
class SaveManager(private val context: Context) {
	data class SaveState(
		val mapIndex: Int,
		val mapId: String,
		val playerX: Float,
		val playerY: Float,
		val gold: Int,
		val meat: Int,
		val tier: Int,
		val xp: Int,
		val totalXp: Int,
		val highestClearedMapIndex: Int,
		val warriorsBought: Int,
		val lancerUnlocked: Boolean,
		val archerUnlocked: Boolean,
		val monkUnlocked: Boolean,
		val savedAt: Long? = null,
		val army: List<SaveUnit> = emptyList()
	)

	data class SaveUnit(
		val type: String,
		val x: Float,
		val y: Float,
		val hp: Int
	)

	data class SaveSummary(
		val exists: Boolean,
		val slotIndex: Int,
		val mapId: String? = null,
		val level: Int? = null, // tier + 1
		val gold: Int? = null,
		val meat: Int? = null,
		val savedAt: Long? = null
	)

	private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

	fun exists(): Boolean = prefs.contains(KEY_STATE)

	fun clear() { prefs.edit().remove(KEY_STATE).apply() }

	fun save(state: SaveState) {
		val json = JSONObject().apply {
			put("mapIndex", state.mapIndex)
			put("mapId", state.mapId)
			put("playerX", state.playerX)
			put("playerY", state.playerY)
			put("gold", state.gold)
			put("meat", state.meat)
			put("tier", state.tier)
			put("xp", state.xp)
			put("totalXp", state.totalXp)
			put("highestClearedMapIndex", state.highestClearedMapIndex)
			put("warriorsBought", state.warriorsBought)
			put("lancerUnlocked", state.lancerUnlocked)
			put("archerUnlocked", state.archerUnlocked)
			put("monkUnlocked", state.monkUnlocked)
			put("savedAt", System.currentTimeMillis())
			// army
			val arr = org.json.JSONArray()
			state.army.forEach { u ->
				val ju = JSONObject()
				ju.put("type", u.type)
				ju.put("x", u.x)
				ju.put("y", u.y)
				ju.put("hp", u.hp)
				arr.put(ju)
			}
			put("army", arr)
		}
		prefs.edit().putString(KEY_STATE, json.toString()).apply()
	}

	fun load(): SaveState? {
		val s = prefs.getString(KEY_STATE, null) ?: return null
		return try {
			val j = JSONObject(s)
			val armyArr = j.optJSONArray("army")
			val armyList = mutableListOf<SaveUnit>()
			if (armyArr != null) {
				for (i in 0 until armyArr.length()) {
					val ju = armyArr.optJSONObject(i) ?: continue
					armyList += SaveUnit(
						type = ju.optString("type", "WARRIOR"),
						x = ju.optDouble("x", 0.0).toFloat(),
						y = ju.optDouble("y", 0.0).toFloat(),
						hp = ju.optInt("hp", 1)
					)
				}
			}
			SaveState(
				mapIndex = j.optInt("mapIndex", 0),
				mapId = j.optString("mapId", "main"),
				playerX = j.optDouble("playerX", 0.0).toFloat(),
				playerY = j.optDouble("playerY", 0.0).toFloat(),
				gold = j.optInt("gold", 0),
				meat = j.optInt("meat", 0),
				tier = j.optInt("tier", 0),
				xp = j.optInt("xp", 0),
				totalXp = j.optInt("totalXp", 0),
				highestClearedMapIndex = j.optInt("highestClearedMapIndex", 0),
				warriorsBought = j.optInt("warriorsBought", 0),
				lancerUnlocked = j.optBoolean("lancerUnlocked", false),
				archerUnlocked = j.optBoolean("archerUnlocked", false),
				monkUnlocked = j.optBoolean("monkUnlocked", false),
				savedAt = j.optLong("savedAt", 0L).let { if (it == 0L) null else it },
				army = armyList
			)
		} catch (_: Exception) { null }
	}

	// --- Slot-based operations ---
	fun saveToSlot(slotIndex: Int): Boolean {
		val s = prefs.getString(KEY_STATE, null) ?: return false
		return try {
			val j = JSONObject(s)
			j.put("savedAt", System.currentTimeMillis())
			prefs.edit().putString(KEY_SLOT_PREFIX + slotIndex, j.toString()).apply()
			true
		} catch (_: JSONException) {
			false
		}
	}

	fun loadFromSlot(slotIndex: Int): SaveState? {
		val s = prefs.getString(KEY_SLOT_PREFIX + slotIndex, null) ?: return null
		prefs.edit().putString(KEY_STATE, s).apply()
		return try {
			val j = JSONObject(s)
			val armyArr = j.optJSONArray("army")
			val armyList = mutableListOf<SaveUnit>()
			if (armyArr != null) {
				for (i in 0 until armyArr.length()) {
					val ju = armyArr.optJSONObject(i) ?: continue
					armyList += SaveUnit(
						type = ju.optString("type", "WARRIOR"),
						x = ju.optDouble("x", 0.0).toFloat(),
						y = ju.optDouble("y", 0.0).toFloat(),
						hp = ju.optInt("hp", 1)
					)
				}
			}
			SaveState(
				mapIndex = j.optInt("mapIndex", 0),
				mapId = j.optString("mapId", "main"),
				playerX = j.optDouble("playerX", 0.0).toFloat(),
				playerY = j.optDouble("playerY", 0.0).toFloat(),
				gold = j.optInt("gold", 0),
				meat = j.optInt("meat", 0),
				tier = j.optInt("tier", 0),
				xp = j.optInt("xp", 0),
				totalXp = j.optInt("totalXp", 0),
				highestClearedMapIndex = j.optInt("highestClearedMapIndex", 0),
				warriorsBought = j.optInt("warriorsBought", 0),
				lancerUnlocked = j.optBoolean("lancerUnlocked", false),
				archerUnlocked = j.optBoolean("archerUnlocked", false),
				monkUnlocked = j.optBoolean("monkUnlocked", false),
				savedAt = j.optLong("savedAt", 0L).let { if (it == 0L) null else it },
				army = armyList
			)
		} catch (_: Exception) { null }
	}

	fun clearSlot(slotIndex: Int) {
		prefs.edit().remove(KEY_SLOT_PREFIX + slotIndex).apply()
	}

	fun getSlotSummary(slotIndex: Int): SaveSummary {
		val s = prefs.getString(KEY_SLOT_PREFIX + slotIndex, null) ?: return SaveSummary(false, slotIndex)
		return try {
			val j = JSONObject(s)
			SaveSummary(
				exists = true,
				slotIndex = slotIndex,
				mapId = j.optString("mapId", "main"),
				level = j.optInt("tier", 0) + 1,
				gold = j.optInt("gold", 0),
				meat = j.optInt("meat", 0),
				savedAt = j.optLong("savedAt", 0L).let { if (it == 0L) null else it }
			)
		} catch (_: Exception) { SaveSummary(false, slotIndex) }
	}

	fun listSlotSummaries(slotCount: Int): List<SaveSummary> =
		(0 until slotCount).map { getSlotSummary(it) }

	companion object {
		private const val PREF_NAME = "empire_save"
		private const val KEY_STATE = "state"
		private const val KEY_SLOT_PREFIX = "state_slot_"
	}
}