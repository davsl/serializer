package sliep.jes.serializer.dip

import sliep.jes.serializer.di.DIModule
import sliep.jes.serializer.di.ModuleVariant

interface MyModule : DIModule {
    val provideC: DependencyC
    val provideCChild: DependencyCChild
    fun provideA(requester: Requester1): DependencyA
    fun provideB(requester: Requester1): DependencyB
    fun provideA(requester: Requester2): DependencyA
    fun provideA(requester: Requester2Child): DependencyA

    enum class Variants {
        Main,
        Testing
    }

    companion object : ModuleVariant<Variants, MyModule>() {
        inline fun <reified T : Any> Any.inject(): T = resolve(this, T::class.java) as T

        override fun initializeVariants(): Variants {
            variants[Variants.Main] = MyModuleImpl
            variants[Variants.Testing] = MyModuleTestImpl
            return Variants.Main
        }
    }
}