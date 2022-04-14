plugins {
    java
    id("dev.nokee.jni-library")
    id("dev.nokee.cpp-language")
    `uber-jni-jar`
    `use-prebuilt-binaries`
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    implementation(projects.autoDarkModeBase)
    implementation(libs.darklaf.nativeUtils)

    // TODO add xdg-desktop-portal dependency
    implementation("com.github.hypfvieh:dbus-java:3.3.1")
    implementation("com.github.hypfview:dbus-java-transport-jnr-unixsocket:4.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.0")

    kapt(libs.autoservice.processor)
    compileOnly(libs.autoservice.annotations)
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20")
}

library {
    targetMachines.addAll(machines.linux.x86_64)
    variants.configureEach {
        resourcePath.set("com/github/weisj/darkmode/${project.name}/${targetMachine.variantName}")
        sharedLibrary {
            compileTasks.configureEach {
                compilerArgs.add("--std=c++11")
                compilerArgs.addAll(
                    toolChain.map {
                        when (it) {
                            is Gcc, is Clang -> compilerFlagsFor(
                                "glibmm-2.4",
                                "giomm-2.4",
                                "sigc++-2.0",
                                "gtk+-3.0"
                            )
                            else -> emptyList()
                        }
                    }
                )

                // Build type not modeled yet, assuming release
                compilerArgs.addAll(
                    toolChain.map {
                        when (it) {
                            is Gcc, is Clang -> listOf("-O2")
                            else -> emptyList()
                        }
                    }
                )
            }
            linkTask.configure {
                linkerArgs.addAll(
                    toolChain.map {
                        when (it) {
                            is Gcc, is Clang -> linkerFlagsFor(
                                "glibmm-2.4",
                                "giomm-2.4",
                                "sigc++-2.0",
                                "gtk+-3.0"
                            )
                            else -> emptyList()
                        }
                    }
                )
            }
        }
    }
}

fun compilerFlagsFor(vararg packages: String): List<String> =
    "pkg-config --cflags ${packages.joinToString(separator = " ")}".runCommand().split(" ").distinct()

fun linkerFlagsFor(vararg packages: String): List<String> =
    "pkg-config --libs ${packages.joinToString(separator = " ")}".runCommand().split(" ").distinct()

fun String.runCommand(): String {
    val process = ProcessBuilder(*split(" ").toTypedArray()).start()
    val output = process.inputStream.reader(Charsets.UTF_8).use {
        it.readText()
    }
    process.waitFor(10, TimeUnit.SECONDS)
    return output.trim()
}