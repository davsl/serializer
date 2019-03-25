Jes Serializer [ ![BinTray](https://img.shields.io/badge/Bintray-v3.1.0-7cb342.svg) ](https://bintray.com/sliep/jes/serializer/_latestVersion) [ ![Javadoc](https://img.shields.io/badge/API%20Documentation-GutHub-212121.svg) ](https://davsl.github.io/serializer/docs/serializer/) [![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-d84315.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
====

**Jes Serializer** is a [kotlin](https://kotlinlang.org/) native framework which gives you 
simple utility functions for
- Reflection extensions
- [Json](https://www.json.org/) Serialization & Deserialization

Kotlin code is compiled using the **same bytecode** of java
this means that if you are a Java developer you can use
this library too: it does not make **any difference** 
except in syntax! <br>
Anyway i suggest you to forget java and start using kotlin
for everything! `:)`

## Simplicity
The strength of my library is the simplicity of implementation
```groovy
repositories {
    maven {
        url "https://dl.bintray.com/sliep/jes"
    }
}
dependencies {
    implementation 'sliep.jes:serializer:xyz'
}
```
And that's all `:)`

## Reliability
Jes Serializer has **zero** (0.000) errors in it's code!
Some recent discoveries have shown that this library contains
0% of bugs! I'ts incredible! `:D` <br>
Tested more times on a lot of **json** files 
and never crashed! <br>
Every parsing error is easy to locate and fix. <br>

## Maintenance
Even though is in **beta**, is a very **stable** library, and 
is going to grow fast: will be always updated to 
introduce new amazing features

## Optimization
The most important part of this code to me is the **optimization**!
If you browse my code i dare you to find a single redundant 
line of code or function that could be written using **less 
instructions**
Studied to be light, powerful and performing, 
**Jes Serializer** the most optimized serialization library 
in the world

## Code implementation
###### Serializer
The Serializer is very easy to use: just call one of the 
functions from the singleton
```kotlin
fun main(vararg args: String) {
    val myObj = MyObj()
    val jsonObject = myObj.toJson() //make the magic :)
    val fromJson: MyObj = jsonObject.fromJson() //another magic :)
    if (fromJson == myObj)
        System.out.println("Jes Serializer is cool!")
    else
        System.err.println("Jes Serializer is shit!")
}

@Suppress("EqualsOrHashCode")
class MyObj : JesObject {
    val var1: Int = 3
    val var2: String = "Hello"
    val var3: MyObj2 = MyObj2()
    override fun equals(other: Any?) = other is MyObj && var1 == other.var1 && var2 == other.var2 && var3 == other.var3
}

@Suppress("EqualsOrHashCode")
class MyObj2 : JesObject {
    val var1: Int = 5
    val var2: String = "World"
    override fun equals(other: Any?) = other is MyObj2 && var1 == other.var1 && var2 == other.var2
}
```
Don't matter if a variable is `private` or `final`, or the 
constructor is `private`:
**Jes Serializer** will always work! (except if flag 
`--illegal-access=deny` is set)

###### Customize object serialization
The default serializer implementation takes every field of the class and wrap the value into a json...
But there is another way to serialize or deserialize an object by using `JesObjectImpl`<br>
JesObjectImpl is an instance of JesObject which provides a function to let the developer manually convert the class into a json value.<br>
For instance let's suppose we have a class model UsefulClassManager and we need the output json to have a different structure but we don't want to change the class
Just implement JesObjectImpl and add a JesConstructor and it's done!
```kotlin
class MyClass(val classManager: UsefulClassManager) : JesObject

class UsefulClassManager(val usefulClazz: Class<*>) : JesObjectImpl<String> {
    constructor(jes: JesConstructor<String>) : this(Class.forName(jes.data)) //create the instance from JesSerializer 

    override fun toJson() = usefulClazz.name //convert the field into a more simple value
}
```
In this case the model instead of being serialized into a JsonObject will be serialized into a String
You can use this mechanism with every type you want, but remember that a root object can be only a JSONObject or JSONArray 
###### Reflection
Reflection extension is very powerful, be careful `:)`
```kotlin
fun main(vararg args: String) {
    val hello = "*****Hello*****"
    System.out.println(hello.invokeMethod<String>("substring", 5, 10)) //Hello
    val myObj = MyObj()
    if (myObj.var1 == myObj.field("var1"))
        System.out.println("Jes Serializer is cool!")
    else
        System.err.println("Jes Serializer is shit!")
}
```
Another example:
```kotlin
val trueField = java.lang.Boolean::class.fieldR("TRUE")
trueField.isFinal = false
trueField[null] = false
System.err.println("value: ${java.lang.Boolean.TRUE}") //will print false
```
###### Helpful utility
Now you can make lateInit final variables that will be initialized only once 
```kotlin
private val myVal get() = lateInit(::myVal) { loadValue() }
//...
fun myFun(){
System.err.println(myVal) //here loadValue() will be called
System.err.println(myVal) //the value is loaded only once
}
```
Logging utility
```kotlin
class MyClass : Loggable {
    fun doTask() {
        val hello = "world"
        log { "Hello is $hello" } //will be executed ONLY if variable LOG is true
        depth++
        for (i in 0..10) {
            System.out.println(i * 1234)
            log { "Computing $i" }
        }
        depth--
        log { "Fineeshhh" }
    }

    var depth = 0

    companion object {
        var LOG = true
    }
}
```
#### Enjoy! `:D`
