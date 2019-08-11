package sliep.jes.serializer.impl

import org.json.JSONException
import sliep.jes.serializer.NonJesObjectException

interface JesSerializerImpl<JV, OV> {
    @Throws(NonJesObjectException::class)
    fun toJson(value: OV, vararg args: String): JV

    @Throws(JSONException::class)
    fun fromJson(value: JV, type: Class<*>, vararg args: String): OV
}