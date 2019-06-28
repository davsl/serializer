package sliep.jes.serializer

class NonJesObjectException(errorClass: Class<*>) :
    Exception("Can't deserialize non ${JesObject::class.java.simpleName} classes. found instance of type $errorClass")