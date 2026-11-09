# kotlin-logging's Logback backend is optional; this app uses the default SLF4J backend.
-dontwarn ch.qos.logback.**

# Jackson uses JVM signature-polymorphic invokeExact calls. Their typed descriptors are
# valid at runtime but do not appear as separate methods in MethodHandle's class file.
-dontwarn java.lang.invoke.MethodHandle
