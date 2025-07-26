import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krosskinetic.notisentry.data.AppNotifications

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMessageInfoList(messages: List<AppNotifications.MessageInfo>?): String {
        return gson.toJson(messages)
    }

    @TypeConverter
    fun toMessageInfoList(json: String?): List<AppNotifications.MessageInfo> {
        if (json == null || json.isEmpty()) {
            return emptyList()
        }
        val listType = object : TypeToken<List<AppNotifications.MessageInfo>>() {}.type
        return gson.fromJson(json, listType)
    }
}