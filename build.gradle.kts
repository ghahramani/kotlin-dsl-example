import org.gradle.kotlin.dsl.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Delete
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.IdeaModule
import org.junit.platform.gradle.plugin.JUnitPlatformExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.palantir.gradle.docker.DockerExtension

buildscript {
	val kotlinVer by extra { "1.1.50" }
	val junitPlatformVer by extra { "1.0.0" }

	val versionPluginVer = "0.15.0"
	val shadowPluginVer = "2.0.1"
	val dockerPluginVer = "0.13.0"

	repositories {
		jcenter()
		mavenCentral()
		maven { setUrl("https://plugins.gradle.org/m2/") }
	}

	dependencies {
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVer")
		classpath("com.github.jengelman.gradle.plugins:shadow:$shadowPluginVer")
		// gradle dependencyUpdates -Drevision=release
		classpath("com.github.ben-manes:gradle-versions-plugin:$versionPluginVer")
		classpath("gradle.plugin.com.palantir.gradle.docker:gradle-docker:$dockerPluginVer")
		classpath("org.junit.platform:junit-platform-gradle-plugin:$junitPlatformVer")
	}
}

val kotlinVer: String by extra
val junitPlatformVer: String by extra

val kotlinLoggingVer = "1.4.6"
val logbackVer = "1.2.3"
val jAnsiVer = "1.16"

val junitJupiterVer = "5.0.0"

apply {
	plugin("org.junit.platform.gradle.plugin")
	plugin("com.github.johnrengelman.shadow")
	plugin("com.github.ben-manes.versions")
	plugin("com.palantir.docker")
}

plugins {
	java
	application
	idea
	kotlin("jvm")
}

tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "1.8"
}

application {
	mainClassName = "app.AppKt"
	applicationName = "app"
	version = "1.0-SNAPSHOT"
	group = "li.barlog.template.kotlin"
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_7
	targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
	compile(kotlin("stdlib", kotlinVer))
	compile(kotlin("reflect", kotlinVer))

	compile("io.github.microutils:kotlin-logging:$kotlinLoggingVer")

	compile("ch.qos.logback:logback-classic:$logbackVer")
	compile("org.fusesource.jansi:jansi:$jAnsiVer")

	testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVer")
	testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVer")
	testRuntime("org.junit.platform:junit-platform-launcher:$junitPlatformVer")
}

repositories {
	jcenter()
}

configure<JUnitPlatformExtension> {
	enableStandardTestTask = true
}

configure<IdeaModel> {
	project {
		languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_8)
	}
	module {
		isDownloadJavadoc = true
		isDownloadSources = true
	}
}

val build: DefaultTask by tasks
val shadowJar = tasks["shadowJar"] as ShadowJar
build.dependsOn(shadowJar)

configure<DockerExtension> {
	name = "app"
	files(shadowJar.archivePath)
	setDockerfile(file("src/main/docker/Dockerfile"))
	buildArgs(mapOf(
		"PORT"   to  "8080",
		"JAVA_OPTS" to "-Xms64m -Xmx128m"
	))
	pull(true)
	dependsOn(shadowJar)
}

tasks.withType<ShadowJar> {
	baseName = "app"
	classifier = null
	version = null
}

tasks.withType<Test> {
	maxParallelForks = Runtime.getRuntime().availableProcessors()
}

task<Wrapper>("wrapper") {
	gradleVersion = "4.2"
	distributionUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-all.zip"
}

val clean: Delete by tasks
task("stage") {
	dependsOn(build, clean)
}
