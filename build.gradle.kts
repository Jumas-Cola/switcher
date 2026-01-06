import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

buildscript {
  dependencies {
    classpath("org.flywaydb:flyway-database-postgresql:11.20.0")
    classpath("org.postgresql:postgresql:42.7.4")
  }
}

plugins {
  kotlin("jvm") version "2.2.20"
  application
  id("com.gradleup.shadow") version "9.2.2"
  id("org.flywaydb.flyway") version "11.20.0"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "5.0.6"
val junitJupiterVersion = "5.9.1"

val mainVerticleName = "com.example.switcher.MainVerticle"
val launcherClassName = "io.vertx.launcher.application.VertxApplication"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-launcher-application")
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-pg-client")
  implementation("io.vertx:vertx-sql-client-templates")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation("io.vertx:vertx-lang-kotlin")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  implementation("io.vertx:vertx-config:4.5.0")
  implementation("io.vertx:vertx-config-hocon:4.5.0")
  implementation("ch.qos.logback:logback-classic:1.4.14")
  implementation("org.slf4j:slf4j-api:2.0.9")
  implementation("io.vertx:vertx-auth-jwt:5.0.6")
  implementation("io.vertx:vertx-web-openapi-router")
  implementation("de.mkammerer:argon2-jvm:2.12")
  implementation("org.flywaydb:flyway-database-postgresql:11.20.0")
  runtimeOnly("org.postgresql:postgresql:42.7.4")

  testImplementation("io.vertx:vertx-junit5")
  testImplementation("io.vertx:vertx-web-client")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget("21")
    languageVersion = KotlinVersion.fromVersion("2.0")
    apiVersion = KotlinVersion.fromVersion("2.0")
  }
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
  maxParallelForks = 1
}

tasks.withType<JavaExec> {
  args = listOf(mainVerticleName)
}

flyway {
  url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/switchapi"
  user = System.getenv("DB_USER") ?: "switchapi"
  password = System.getenv("DB_PASSWORD") ?: "switchapi"
  locations = arrayOf("classpath:db/migration")
}
