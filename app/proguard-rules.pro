# kotlinx.serialization keeps generated serializers reachable through reflection.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$* *;
}
-keepclassmembers class **$* implements kotlinx.serialization.KSerializer {
    static <1>$* INSTANCE;
}

-keep,includedescriptorclasses class com.abaye.derechkohanim.data.model.** { *; }
