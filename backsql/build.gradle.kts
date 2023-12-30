import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.jetbrains.kotlin.cli.jvm.main

plugins {
    alias(backsql.plugins.kotlin.multiplatform)
    alias(backsql.plugins.buildkonfig)
    alias(backsql.plugins.docker.api)
    application
}

group = "io.github.sgpublic"
version = "1.0.0-alpha01"

kotlin {
    jvm {
        withJava()
        withSourcesJar()
        mainRun {
            mainClass = "$group.backsql.AppKt"
        }
        application {
            mainClass = "$group.backsql.AppKt"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(backsql.clikt)
                implementation(backsql.quartz)
                implementation(backsql.uuid)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(backsql.slf4j)
                implementation(backsql.logback.classic)
                implementation(backsql.uniktx.kotlin.logback)

                runtimeOnly(backsql.mysql)
                runtimeOnly(backsql.mariadb)
            }
        }
    }
}

buildkonfig {
    packageName = "$group.backsql"
    objectName = "BuildConfig"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "VERSION_NAME", version.toString())
    }
}

tasks {
    val clean by getting
    val assembleDist by getting
    val installDist by getting

    val dockerCreateDockerfile by creating(Dockerfile::class) {
        group = "docker"
        from("openjdk:17-slim-bullseye")
        workingDir("/app")
        copy {
            copyFile("./install/backsql", "/app")
        }
        runCommand(listOf(
                "useradd -u 1000 runner",
                "apt-get update",
                "apt-get install findutils -y",
                "chown -R runner:runner /app"
        ).joinToString(" &&\\\n "))
        user("runner")
        volume("/var/tmp/backsql")
        volume("/var/log/backsql")
        volume("/app/backsql")
        entryPoint("/app/bin/backsql")
    }

    val tag = "mhmzx/backsql"
    val dockerBuildImage by creating(DockerBuildImage::class) {
        group = "docker"
        dependsOn(assembleDist, installDist, dockerCreateDockerfile)
        inputDir = project.file("./build")
        dockerFile = dockerCreateDockerfile.destFile
        images.add("$tag:$version")
        images.add("$tag:latest")
        noCache = true
    }

    val dockerPushImageOfficial by creating(DockerPushImage::class) {
        group = "docker"
        dependsOn(dockerBuildImage)
        images.add("$tag:$version")
        images.add("$tag:latest")
    }
}

docker {
    registryCredentials {
        username = findProperty("publishing.docker.username")!!.toString()
        password = findProperty("publishing.docker.password")!!.toString()
        email = findProperty("publishing.developer.email")!!.toString()
    }
}
