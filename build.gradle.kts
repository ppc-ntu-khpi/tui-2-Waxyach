plugins {
	java
	application
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(fileTree("jars") {
		include("*.jar")
	})

	testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
	jar {
		manifest {
			attributes["Main-Class"] = "CLIDemo"
		}
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
	}

	test {
		useJUnitPlatform()

		testLogging {
			events("passed", "skipped", "failed")
		}
	}
}

application {
	mainClass.set("CLIDemo")
}