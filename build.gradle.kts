import java.net.URI

plugins {
    `java-library`
    maven
    `maven-publish`
}

val ver = Version("1", "0", "0", env("BUILD_NUMBER") ?: env("GIT_COMMIT")?.substring(0..6) ?: "DEV")

group = "net.kjp12"
version = ver

println("Welcome to $group:$name:$ver")

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    api(/*"com.mewna"*/"com.github.kjp12", "catnip", "b85b70817089")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("org.codehaus.groovy", "groovy-jsr223", "3.0.0-rc-1", classifier = "indy") {
        exclude(module = "groovy")
    }
    testImplementation("org.codehaus.groovy", "groovy", "3.0.0-rc-1", classifier = "indy")
    testImplementation("org.mockito:mockito-core:3.1.0")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.5.1")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.5.1")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    val sysinfo = "CommandSystemInfo.java"
    val sourcesForRelease = register<Copy>("sourcesForRelease") {
        from(sourceSets.getByName("main").allJava) {
            include("**/$sysinfo")
            filter {
                var i = it
                for (e in ver) i = i.replace("@${e.key}@", e.value)
                i
            }
        }
        into("build/filteredSrc")
        includeEmptyDirs = false
    }.get()
    val generateJavaSources = register<SourceTask>("generateJavaSources") {
        dependsOn(sourcesForRelease)
        val src = sourceSets.getByName("main").allJava.filter { it.name != sysinfo }.toMutableList()
        src.add(sourcesForRelease.destinationDir)
        setSource(src)
    }.get()
    withType<JavaCompile> {
        dependsOn(generateJavaSources)
        source = generateJavaSources.source
        options.isFork = true
        options.forkOptions.executable = "javac"
        options.isIncremental = true
        options.compilerArgs.addAll(arrayOf("-XDignore.symbol.file", "-Xlint:deprecation", "-Xlint:unchecked"))
    }
    val jar = getByName<Jar>("jar") {
        manifest.attributes["Implementation-Version"] = archiveVersion.orNull
    }
    val sourcesJar = register<Jar>("sourcesJar") {
        dependsOn(sourcesForRelease)
        archiveClassifier.set("sources")
        from("src/main/java") { exclude("**/$sysinfo") }
        from(sourcesForRelease.destinationDir)
    }.get()
    "build" {
        dependsOn("test", jar, sourcesJar)
    }
    withType<Test> {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
        this.systemProperties(
            "junit.jupiter.extensions.autodetection.enabled" to "true",
            "junit.jupiter.testinstance.lifecycle.default" to "per_class"
        )
    }
    /*register<Task>("gitpatch") {
        //All of these must run successfully for the tag to be allowed to be created.
        dependsOn("compileKotlin", "compileJava", "compileTestKotlin", "compileTestJava", "test", "check")
        doLast {
            val bytes = Files.readAllBytes(NioPath.of(project.rootDir.toString(), "build.gradle.kts"))
            val verInfoStart = "// <versionInfo>\n".toByteArray()
            val verInfoEnd = "// </versionInfo>\n".toByteArray()
            //This is to know where in the code it is exactly.
            val s = Bytes.indexOf(bytes, verInfoStart)
            val e = Bytes.indexOf(bytes, verInfoEnd)
            val startingBytes = bytes.copyOfRange(0, s + verInfoStart.size)
            val midBytes = bytes.copyOfRange(s + verInfoStart.size + 1, e)
            val endingBytes = bytes.copyOfRange(e, bytes.size)
            //GIT Releases
        }
    }*/

}

fun env(e: String): String? {
    val s = System.getenv(e)
    return if (s.isNullOrBlank()) {
        val t = System.getProperty(e)
        if (t.isNullOrBlank()) null else t
    } else s
}

data class Version(val major: String, val minor: String, val revision: String, val build: String) : Map<String, String> {
    private val version = "$major.$minor.${revision}_$build"
    override val entries: Set<Map.Entry<String, String>> = setOf("major" entry major, "minor" entry minor, "revision" entry revision, "build" entry build, "version" entry version)
    override val keys = setOf("major", "minor", "revision", "build", "version")
    override val size = 4
    override val values = listOf(major, minor, revision, build, version)
    override fun containsKey(key: String) = keys.contains(key)
    override fun containsValue(value: String) = values.contains(value)

    private infix fun String.entry(o: String) = VersionEntry(this, o)

    override fun get(key: String) = when (key) {
        "major" -> major
        "minor" -> minor
        "revision" -> revision
        "build" -> build
        "version" -> version
        else -> null
    }

    override fun isEmpty() = false

    override fun toString() = version
}

data class VersionEntry(override val key: String, override val value: String) : Map.Entry<String, String>