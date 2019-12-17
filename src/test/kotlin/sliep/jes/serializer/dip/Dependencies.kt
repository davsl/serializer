package sliep.jes.serializer.dip

abstract class MyDependency {
    abstract val additionalData: String
    override fun toString(): String = "${this::class.java.simpleName}($additionalData)"
}

class DependencyA(override val additionalData: String) : MyDependency()

class DependencyB(override val additionalData: String) : MyDependency()

open class DependencyC(override val additionalData: String) : MyDependency()

class DependencyCChild(additionalData: String) : DependencyC(additionalData)
