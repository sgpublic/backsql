import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(backsql.plugins.kotlin.multiplatform)
    alias(backsql.plugins.com.codingfeline.buildkonfig)
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
