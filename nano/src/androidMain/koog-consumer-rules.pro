# Koog reaches Ktor 3.6.0's optional IntellijIdeaDebugDetector on Android.
# IntellijIdeaDebugDetectorJvm.kt catches Throwable and returns false when the
# JVM management API is absent. These are the two guarded references, not
# required Android functionality. Recheck this guard when upgrading Ktor.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
