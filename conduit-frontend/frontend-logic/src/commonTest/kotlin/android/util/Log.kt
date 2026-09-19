package android.util

import kotlin.jvm.JvmStatic

/**
 * Solution from https://stackoverflow.com/a/69634728/8529009
 */
object Log {
  @JvmStatic
  fun v(tag: String, msg: String): Int {
    println("VERBOSE: $tag: $msg")
    return 0
  }

  @JvmStatic
  fun d(tag: String, msg: String): Int {
    println("DEBUG: $tag: $msg")
    return 0
  }

  @JvmStatic
  fun i(tag: String, msg: String): Int {
    println("INFO: $tag: $msg")
    return 0
  }

  @JvmStatic
  fun w(tag: String, msg: String): Int {
    println("WARN: $tag: $msg")
    return 0
  }

  @JvmStatic
  fun w(tag: String, msg: String, exception: Throwable): Int {
    println("WARN: $tag: $msg , $exception")
    return 0
  }

  @JvmStatic
  fun e(tag: String, msg: String): Int {
    println("ERROR: $tag: $msg")
    return 0
  }

  @JvmStatic
  fun e(tag: String, msg: String, exception: Throwable): Int {
    println("ERROR: $tag: $msg , $exception")
    return 0
  }

  @JvmStatic
  fun isLoggable(tag: String, level: Int): Boolean {
    return true
  }
}