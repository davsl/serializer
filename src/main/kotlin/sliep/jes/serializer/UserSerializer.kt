package sliep.jes.serializer

import org.json.JSONException
import java.lang.reflect.Type

interface UserSerializer<JV, OV> {
    @Throws(NonJesObjectException::class)
    fun toJson(value: OV): JV

    @Throws(JSONException::class)
    fun fromJson(value: JV, type: Type): OV
}