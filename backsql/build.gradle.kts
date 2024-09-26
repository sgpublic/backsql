import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(backsql.plugins.kotlin.multiplatform)
    alias(backsql.plugins.buildkonfig)
    alias(backsql.plugins.docker.api)
    alias(backsql.plugins.release.github)
    application
}

group = "io.github.sgpublic"
version = "1.0.0-alpha02"

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
                implementation(backsql.archiver)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(backsql.kotlin.test)
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
        buildConfigField(FieldSpec.Type.STRING, "APPLICATION_ID", rootProject.name)
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
        copyFile("./install/backsql", "/app")
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

    val githubRelease by getting {
        dependsOn(assembleDist)
    }
}

fun findEnv(name: String) = provider {
    return@provider findProperty(name)?.toString()?.takeIf { it.isNotBlank() }
            ?: System.getenv(name.replace(".", "_").uppercase())
}

docker {
    registryCredentials {
        username = findEnv("publishing.docker.username")
        password = findEnv("publishing.docker.password")
    }
}

githubRelease {
    token(findEnv("publishing.github.token"))
    owner = "sgpublic"
    repo = "backsql"
    tagName = "v$version"
    releaseName = "v$version"
    prerelease = version.toString().let { it.contains("alpha") || it.contains("beta") }
    releaseAssets = files("./build/distributions/${name}-${version}.zip")
    overwrite = true
}
