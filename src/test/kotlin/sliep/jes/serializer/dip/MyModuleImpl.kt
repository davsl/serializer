package sliep.jes.serializer.dip

import sliep.jes.serializer.di.Module

internal object MyModuleImpl : Module(), MyModule {
    override val provideC: DependencyC
        get() = DependencyC(
            "Provided by ${this::class.java.simpleName}"
        )
    override val provideCChild: DependencyCChild
        get() = DependencyCChild(
            "Provided by ${this::class.java.simpleName}"
        )

    override fun provideA(requester: Requester1): DependencyA =
        DependencyA("Provided by ${this::class.java.simpleName}")

    override fun provideB(requester: Requester1): DependencyB =
        DependencyB("Provided by ${this::class.java.simpleName}")

    override fun provideA(requester: Requester2): DependencyA =
        DependencyA("Requested by Requester2")

    override fun provideA(requester: Requester2Child): DependencyA =
        DependencyA("Super secret")
}
