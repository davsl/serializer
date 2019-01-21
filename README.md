# Wellcome to Jes Serializer
**Jes Serializer** is a [kotlin](https://kotlinlang.org/) native framework which gives you 
simple utility functions for
- Reflection [Inline functions](https://kotlinlang.org/docs/reference/inline-functions.html)
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
dependencies {
    implementation 'sliep.jes:serializer:1.0'
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

## How to use
###### Serializer
The Serializer is very easy to use: just call one of the 
functions from the singleton
```kotlin
fun main(vararg args: String) {
    val myObj = MyObj()
    val jsonObject = JesSerializer.toJson(myObj) //make the magic :)
    val fromJson = JesSerializer.fromJson(jsonObject, MyObj::class.java) //another magic :)
    if (fromJson == myObj)
        System.out.println("Jes Serializer is cool!")
    else
        System.err.println("Jes Serializer is shit!")
}

class MyObj : JesObject {
    val var1: Int = 3
    val var2: String = "Hello"
    val var3: MyObj2 = MyObj2()
    override fun equals(other: Any?) = other is MyObj && var1 == other.var1 && var2 == other.var2 && var3 == other.var3
}

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

###### Reflection
Reflection extension is very powerful, be careful `:)`
```kotlin
fun main(vararg args: String) {
    val hello = "*****Hello*****"
    System.out.println(hello.invokeMethod("substring", 5, 10)) //Hello
    val myObj = MyObj()
    if (myObj.var1 == myObj.getField("var1", Int::class))
        System.out.println("Jes Serializer is cool!")
    else
        System.err.println("Jes Serializer is shit!")
}
```

#### Enjoy! `:D`
