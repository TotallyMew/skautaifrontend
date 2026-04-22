package lt.skautai.android.data.local.mapper

import com.google.gson.Gson
import java.lang.reflect.Type

internal val localGson = Gson()

internal inline fun <reified T> toJsonOrNull(value: T?): String? =
    value?.let { localGson.toJson(it) }

internal fun <T> fromJsonOrNull(json: String?, type: Type): T? {
    if (json.isNullOrBlank() || json == "null") return null
    return runCatching { localGson.fromJson<T>(json, type) }.getOrNull()
}

internal fun <T> fromJsonListOrEmpty(json: String?, type: Type): List<T> {
    if (json.isNullOrBlank() || json == "null") return emptyList()
    return runCatching { localGson.fromJson<List<T>>(json, type) }.getOrNull().orEmpty()
}
