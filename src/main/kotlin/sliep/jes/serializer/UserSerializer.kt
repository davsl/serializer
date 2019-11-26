package sliep.jes.serializer

import org.json.JSONException

interface UserSerializer<JV, OV> {
    @Throws(NonJesObjectException::class)
    fun toJson(value: OV): JV

    @Throws(JSONException::class)
    fun fromJson(value: JV, type: Class<*>): OV
}