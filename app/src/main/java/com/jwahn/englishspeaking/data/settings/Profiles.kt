package com.jwahn.englishspeaking.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Profile(val id: String, val name: String, val emoji: String)

/**
 * 한 폰에서 쓰는 가족 프로필 목록.
 * 프로필마다 학습 기록(DB)과 설정이 따로 저장되고, AI 연결(키)은 함께 쓴다.
 */
class Profiles(context: Context) {
    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    private val _list = MutableStateFlow(load())
    val list: StateFlow<List<Profile>> = _list

    var currentId: String
        get() = prefs.getString("current", "default") ?: "default"
        set(v) { prefs.edit().putString("current", v).commit() }

    private fun load(): List<Profile> {
        val raw = prefs.getString("list", null)
        val parsed = raw?.lines()?.mapNotNull { l ->
            val p = l.split("\t"); if (p.size == 3) Profile(p[0], p[1], p[2]) else null
        }.orEmpty()
        return if (parsed.none { it.id == "default" }) listOf(Profile("default", "나", "🙂")) + parsed else parsed
    }

    private fun save(list: List<Profile>) {
        prefs.edit().putString("list", list.joinToString("\n") { "${it.id}\t${it.name.replace("\t", " ")}\t${it.emoji}" }).commit()
        _list.value = list
    }

    fun current(): Profile = _list.value.firstOrNull { it.id == currentId } ?: _list.value.first()

    fun add(name: String, emoji: String): Profile {
        val p = Profile("p" + System.currentTimeMillis().toString(36), name.trim().ifBlank { "가족" }, emoji.ifBlank { "🙂" })
        save(_list.value + p)
        return p
    }

    fun rename(id: String, name: String, emoji: String) =
        save(_list.value.map { if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }, emoji = emoji.ifBlank { it.emoji }) else it })

    /** 기본 프로필은 지울 수 없다. 지운 프로필의 학습 기록 파일도 삭제 */
    fun remove(context: Context, id: String) {
        if (id == "default") return
        save(_list.value.filterNot { it.id == id })
        if (currentId == id) currentId = "default"
        context.deleteDatabase(com.jwahn.englishspeaking.data.db.AppDatabase.fileName(id))
        context.deleteSharedPreferences("profile_$id")
    }
}
