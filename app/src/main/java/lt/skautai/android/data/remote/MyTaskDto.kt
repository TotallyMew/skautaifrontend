package lt.skautai.android.data.remote

import com.google.gson.annotations.SerializedName

data class MyTaskDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("priority") val priority: Int,
    @SerializedName("urgency") val urgency: String,
    @SerializedName("bucket") val bucket: String,
    @SerializedName("routeTarget") val routeTarget: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("dueAt") val dueAt: String? = null,
    @SerializedName("entityId") val entityId: String? = null
)

data class MyTaskListDto(
    @SerializedName("tasks") val tasks: List<MyTaskDto>,
    @SerializedName("total") val total: Int
)
