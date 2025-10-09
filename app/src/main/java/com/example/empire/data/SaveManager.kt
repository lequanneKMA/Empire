package com.example.empire.data

import android.content.Context
import org.json.JSONObject

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
		val monkUnlocked: Boolean
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
		}
		prefs.edit().putString(KEY_STATE, json.toString()).apply()
	}

	fun load(): SaveState? {
		val s = prefs.getString(KEY_STATE, null) ?: return null
		return try {
			val j = JSONObject(s)
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
				monkUnlocked = j.optBoolean("monkUnlocked", false)
			)
		} catch (_: Exception) { null }
	}

	companion object {
		private const val PREF_NAME = "empire_save"
		private const val KEY_STATE = "state"
	}
}