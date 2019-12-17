@file:Suppress("unused", "UNUSED_PARAMETER", "JoinDeclarationAndAssignment", "SpellCheckingInspection")

package sliep.jes.serializer

import org.junit.Assert
import org.junit.Test
import sliep.jes.serializer.dip.*
import sliep.jes.serializer.dip.MyModule.Companion.inject
import sliep.jes.serializer.dip.MyModule.Variants

class DependencyInjectionTest {

    class Claaaaa : Requester1 {
        val dependencyA: DependencyA = inject()
        val dependencyB: DependencyB = inject()
        val dependencyC: DependencyC = inject()
        val dependencyCChild: DependencyCChild = inject()

        val bubu = cc()
        val bubuz = ccz()

        class cc : Requester2 {
            val dependencyA: DependencyA = inject()
        }

        class ccz : Requester2Child {
            val dependencyA: DependencyA = inject()
        }
    }

    @Test
    fun testDependencyInjectioin() {
        Claaaaa().apply {
            Assert.assertEquals("DependencyA(Provided by MyModuleImpl)", dependencyA.toString())
            Assert.assertEquals("DependencyB(Provided by MyModuleImpl)", dependencyB.toString())
            Assert.assertEquals("DependencyA(Requested by Requester2)", bubu.dependencyA.toString())
            Assert.assertEquals("DependencyC(Provided by MyModuleImpl)", dependencyC.toString())
            Assert.assertEquals("DependencyCChild(Provided by MyModuleImpl)", dependencyCChild.toString())
            Assert.assertEquals("DependencyA(Super secret)", bubuz.dependencyA.toString())
        }
        MyModule.variant = Variants.Testing
        Claaaaa().apply {
            Assert.assertEquals("DependencyA(Provided by MyModuleTestImpl)", dependencyA.toString())
            Assert.assertEquals("DependencyB(Provided by MyModuleTestImpl)", dependencyB.toString())
        }
    }
}