package sliep.jes.serializer

import java.io.Serializable

/**
 * Implement [JesObject] interface from your model class to let [JesSerializer] know that the type can be serialized/deserialized
 * @author sliep
 * @see JesSerializer
 */
interface JesObject : Serializable