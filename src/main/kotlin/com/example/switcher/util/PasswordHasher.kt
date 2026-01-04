package com.example.switcher.util

import de.mkammerer.argon2.Argon2Factory

object PasswordHasher {

  private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

  private const val ITERATIONS = 3
  private const val MEMORY = 65536  // 64 MB
  private const val PARALLELISM = 1

  fun hash(password: String): String {
    return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, password.toCharArray())
  }

  fun verify(password: String, hash: String): Boolean {
    return argon2.verify(hash, password.toCharArray())
  }
}
