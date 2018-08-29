// tag::use-plugin[]
plugins {
// end::use-plugin[]
    java
// tag::use-plugin[]
    `maven-publish`
}
// end::use-plugin[]

group = "org.gradle.sample"
version = "1.0"

// tag::publish-component[]
publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}
// end::publish-component[]
// tag::repositories[]
publishing {
    repositories {
        maven {
            // change to point to your repo, e.g. http://my.org/repo
            url = uri("$buildDir/repo")
        }
    }
}
// end::repositories[]

