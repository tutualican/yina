# Keep Anki backend / UniFFI entry points once the Rust core is linked.
-keep class io.github.yina.core.engine.** { *; }
-keep class uniffi.** { *; }

# Kotlin metadata used by reflection in debug tooling.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.coroutines.**
