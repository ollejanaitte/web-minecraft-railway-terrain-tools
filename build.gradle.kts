plugins {
	id("java")
}

allprojects {
	apply(plugin = "eclipse")

	repositories {
		mavenCentral()
	}

	plugins.withId("java") {
		java {
			toolchain {
				languageVersion = JavaLanguageVersion.of(17)
			}
		}
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
	named("main") {
		java.srcDirs(
			"src/main/java",
			"src/game/java",
			"src/protocol-game/java",
			"src/protocol-relay/java",
			"src/platform-api/java",
			// Phase 1.1 production geometry core (pure math; also on harness)
			"src/geometry-core/java"
		)
	}

	// JVM-only Railway v2 test harness.
	// Deliberately independent of the game "main" source set so math/simulation
	// tests run fast on a plain JVM with NO Eaglercraft/TeaVM runtime.
	// Includes production railsys geometry for Phase 1.1 numerical tests.
	create("harness") {
		java.srcDirs("src/harness/java", "src/geometry-core/java")
		resources.srcDir("src/harness/resources")
	}
}

// Reference-math harness runner (dependency-free; no JUnit required).
val harnessTest = tasks.register<JavaExec>("harnessTest") {
	group = "verification"
	description = "Run the Railway v2 reference math test harness (pure JVM, no game runtime)."
	classpath = sourceSets["harness"].runtimeClasspath
	mainClass.set("railv2test.Runner")
	standardOutput = System.out
	errorOutput = System.err
}

tasks.named("check") {
	dependsOn(harnessTest)
}

// R10F: regenerate the Numerical Golden Dataset (doc/testing/phase1_r10f/golden).
// Run explicitly: ./gradlew goldenGenerate
val goldenGenerate = tasks.register<JavaExec>("goldenGenerate") {
	group = "verification"
	description = "Regenerate Railsys Foundation golden data fixtures (production RailPath pipeline)."
	classpath = sourceSets["harness"].runtimeClasspath
	mainClass.set("railv2test.tools.GoldenDataGenerator")
	standardOutput = System.out
	errorOutput = System.err
}

// R13: measure geometry capability to justify the frozen production limits.
// Run explicitly: ./gradlew limitMeasure
val limitMeasure = tasks.register<JavaExec>("limitMeasure") {
	group = "verification"
	description = "Measure Railsys geometry numeric capability for R13 production limits."
	classpath = sourceSets["harness"].runtimeClasspath
	mainClass.set("railv2test.tools.R13LimitMeasurement")
	standardOutput = System.out
	errorOutput = System.err
}

dependencies {
	implementation(libs.bundles.common)
}

tasks.withType<Jar> {
	entryCompression = ZipEntryCompression.STORED
	// TeaVM will fail if anything from platform-api is in the JAR
	fileTree("src/platform-api/java").visit {
		if (!isDirectory) {
			if (path.endsWith(".java")) {
				exclude(path.substring(0, path.length - 5) + ".class")
			}
		}
	}
}
