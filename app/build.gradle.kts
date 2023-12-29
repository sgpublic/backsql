import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.lang.reflect.Type

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
            mainClass = "$group.backsql.App"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(backsql.clikt)
            }
        }
    }
}

buildkonfig {
    packageName = "$group.backsql"
    // objectName = 'YourAwesomeConfig'
    // exposeObjectWithName = 'YourAwesomePublicConfig'

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "VERSION_NAME", version.toString())
    }
}
