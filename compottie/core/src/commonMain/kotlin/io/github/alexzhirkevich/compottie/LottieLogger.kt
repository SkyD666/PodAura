package io.github.alexzhirkevich.compottie

private const val TAG = "Compottie"

public interface LottieLogger {

    public fun info(message: String)

    public fun warn(message: String)

    public fun error(message: String, throwable: Throwable)

    /**
     * Uses stdout / stderr.
     * */
    public object Default : LottieLogger {
        override fun info(message: String) {
            println("ℹ\uFE0F [$TAG] $message")
        }

        override fun warn(message: String) {
            println("⚠\uFE0F [$TAG] $message")
        }

        override fun error(message: String, throwable: Throwable) {
            println("⛔ [$TAG] $message")
            throwable.printStackTrace()
        }
    }
}
