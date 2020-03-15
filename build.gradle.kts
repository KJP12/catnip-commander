import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

plugins {
    `java-library`
    `maven-publish`
}
val ver = if (env("BUILD_NUMBER") == "release") {
    val ver = env("GITHUB_REF")!!.split('/') // We trust that this exists as BUILD_NUMBER being set to release will only happen in the action 'release.yml'.
    val v = ver[ver.size - 1].substring(1).split('.')
    if (v.size < 3) throw Exception("Version isn't defined properly defined. Gotten GITHUB_REF of `${env("GITHUB_REF")}`; parsed down to `${v}`.")
    releaseVersion(v[0], v[1], v[2])
} else Version("1", "0", "0", env("BUILD_NUMBER") ?: env("GITHUB_RUN_ID") ?: env("GIT_COMMIT")?.substring(0..6) ?: env("GITHUB_SHA")?.substring(0..6) ?: "DEV")

group = "net.kjp12"
version = ver

println("Welcome to $group:$name:$ver")

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    api("com.mewna", "catnip", "9701219")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("org.codehaus.groovy", "groovy-jsr223", "3.0.1", classifier = "indy") {
        exclude(module = "groovy")
    }
    testImplementation("org.codehaus.groovy", "groovy", "3.0.1", classifier = "indy")
    testImplementation("org.mockito:mockito-core:3.3.0")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.6.0")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.6.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    val sysinfo = "net/kjp12/commands/CommandSystemInfo.java"
    val sourcesForRelease = register<Copy>("sourcesForRelease") {
        from(sourceSets.getByName("main").allJava) {
            include(sysinfo)
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
        val src = sourceSets.getByName("main").allJava.filter { !it.absolutePath.endsWith(sysinfo) }.toMutableList()
        src.add(sourcesForRelease.destinationDir)
        setSource(src)
    }.get()
    withType<JavaCompile> {
        dependsOn(generateJavaSources)
        source = generateJavaSources.source
        options.isFork = true
        options.forkOptions.executable = "javac"
        options.isIncremental = true
        options.compilerArgs.addAll(arrayOf("-XDignore.symbol.file", "-Xlint:deprecation", "-Xlint:unchecked", "-encoding", "UTF-8")) //-encoding UTF-8 required for some reason.
    }
    val jar = getByName<Jar>("jar") {
        manifest.attributes["Implementation-Version"] = archiveVersion.orNull
    }
    val sourcesJar = register<Jar>("sourcesJar") {
        dependsOn(sourcesForRelease)
        manifest.attributes["Implementation-Version"] = archiveVersion.orNull
        archiveClassifier.set("sources")
        from("src/main/java") { exclude(sysinfo) }
        from(sourcesForRelease.destinationDir)
    }
    "build" {
        dependsOn("test", jar, sourcesJar)
    }
    withType<Test> {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
        this.systemProperties("junit.jupiter.extensions.autodetection.enabled" to "true", "junit.jupiter.testinstance.lifecycle.default" to "per_class")
    }
    // This is only for GitHub Actions to run.
    register<Task>("gh-release") {
        dependsOn("build")
        actions.add(Action<Task> {
            val client = HttpClient.newHttpClient()
            val uri = env("upload-asset-url")!!.substringBeforeLast('{')
            for (f in File("./build/libs").listFiles()) {
                val res = client.send(HttpRequest.newBuilder(URI.create(uri + "?name=${f.name}")).POST(HttpRequest.BodyPublishers.ofFile(f.toPath())).header("Authorization", "token ${env("PASSWORD")}").header("Content-Type", "application/zip").build(), HttpResponse.BodyHandlers.ofString())
                val code = res.statusCode()
                if (code < 200 || code > 299) {
                    throw Exception("Failed to upload file. " + res.body())
                }
            }
        })
    }
}

// MUST come after tasks, this one's default task doesn't override on from.
java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    withType<Javadoc> {
        isFailOnError = false
        options.encoding = "UTF-8"
    }
}

publishing {
    repositories {
        // Disallows publishing to GitHub if it isn't running in its action runner.
        // Should avoid any accidental publishing to Github with invalid username and password.
        if (env("RUNNER") == "github") {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/${env("GITHUB_REPOSITORY")}")
                credentials {
                    username = env("USERNAME")
                    password = env("PASSWORD")
                }
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }
}

fun env(e: String): String? {
    val s = System.getenv(e)
    return if (s.isNullOrBlank()) {
        val t = System.getProperty(e)
        if (t.isNullOrBlank()) null else t
    } else s
}

data class Version(val major: String, val minor: String, val revision: String, val build: String, val version: String = "$major.$minor.${revision}_$build") : Map<String, String> {
    override val entries: Set<Map.Entry<String, String>> = setOf("major" entry major, "minor" entry minor, "revision" entry revision, "build" entry build, "version" entry version)
    override val keys = setOf("major", "minor", "revision", "build", "version")
    override val values = listOf(major, minor, revision, build, version)
    override val size = 5
    override fun containsKey(key: String) = keys.contains(key)
    override fun containsValue(value: String) = values.contains(value)

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

fun releaseVersion(major: String, minor: String, revision: String) = Version(major, minor, revision, "release", "$major.$minor.$revision")

infix fun String.entry(o: String) = VersionEntry(this, o)

data class VersionEntry(override val key: String, override val value: String) : Map.Entry<String, String>