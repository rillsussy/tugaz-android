package com.tugaz.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object TaskRepository {
    private const val Preferences = "tugaz_tasks"
    private const val Key = "tasks"

    fun load(context: Context): List<Task> {
        val raw = context.getSharedPreferences(Preferences, Context.MODE_PRIVATE).getString(Key, "[]") ?: "[]"
        return JSONArray(raw).let { list ->
            buildList {
                for (index in 0 until list.length()) {
                    val item = list.getJSONObject(index)
                    add(Task(item.getLong("id"), item.getString("title"), item.getLong("deadline")))
                }
            }.sortedBy(Task::deadlineMillis)
        }
    }

    fun save(context: Context, tasks: List<Task>) {
        val items = JSONArray()
        tasks.forEach { task -> items.put(JSONObject().put("id", task.id).put("title", task.title).put("deadline", task.deadlineMillis)) }
        context.getSharedPreferences(Preferences, Context.MODE_PRIVATE).edit().putString(Key, items.toString()).apply()
    }
}
