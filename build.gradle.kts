import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    `java-library`
    maven
    `maven-publish`
    kotlin("jvm") version "1.3.21"
}

val ver = Version("1", "0", "0", env("BUILD_NUMBER") ?: env("GIT_COMMIT")?.substring(0..6) ?: "DEV")

group = "net.kjp12"
version = ver

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    api("com.github.KJP12:catnip:7c2c2ea")
    api(kotlin("stdlib-jdk8"))
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testCompile(kotlin("script-runtime"))
    testCompile(kotlin("script-util"))
    testCompile(kotlin("reflect"))
    testCompile(kotlin("compiler-embeddable"))
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    val sourcesForRelease = register<Copy>("sourcesForRelease") {
        from(sourceSets.getByName("main").allJava) {
            include("**/CommanderInfo.java")
            filter(ver, ReplaceTokens::class.java)
        }
        into("build/filteredSrc")
        includeEmptyDirs = false
    }.get()
    val generateJavaSources = register<SourceTask>("generateJavaSources") {
        val src = sourceSets.getByName("main").allJava.filter { it.name != "CommanderInfo.java" }.toMutableList()
        src.add(sourcesForRelease.destinationDir)
        setSource(src)
        dependsOn(sourcesForRelease)
    }.get()
    withType<JavaCompile> {
        source = generateJavaSources.source
        dependsOn(generateJavaSources)
        options.isFork = true
        options.forkOptions.executable = "javac"
        options.isIncremental = true
        options.compilerArgs.addAll(arrayOf("-XDignore.symbol.file", "-Xlint:deprecation", "-Xlint:unchecked"))
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    val jar = getByName<Jar>("jar") {
        manifest.attributes["Implementation-Version"] = version
    }
    val sourcesJar = register<Jar>("sourcesJar") {
        classifier = "sources"
        from("src/main/java") { exclude("**/CommanderInfo.java") }
        from(sourcesForRelease.destinationDir)
        dependsOn(sourcesForRelease)
    }.get()
    "build" {
        dependsOn(jar, sourcesJar)
    }
}



fun env(e: String): String? {
    val s = System.getenv(e)
    return if (s.isNullOrBlank()) {
        val t = System.getProperty(e)
        if (t.isNullOrBlank()) null else t
    } else s
}

data class Version(val major: String, val minor: String, val revision: String, val build: String) : Map<String, String> {
    override val entries: Set<Map.Entry<String, String>> = setOf("major".entry(major), "minor".entry(minor), "revision".entry(revision), "build".entry(build))
    override val keys = setOf("major", "minor", "revision", "build")
    override val size = 4
    override val values = listOf(major, minor, revision, build)
    override fun containsKey(key: String) = keys.contains(key)
    override fun containsValue(value: String) = values.contains(value)

    private fun String.entry(o: String) = VersionEntry(this, o)

    override fun get(key: String) = when (key) {
        "major" -> major
        "minor" -> minor
        "revision" -> revision
        "build" -> build
        else -> null
    }

    override fun isEmpty() = false

    override fun toString() = "$major.$minor.${revision}_$build"
}

data class VersionEntry(override val key: String, override val value: String) : Map.Entry<String, String>