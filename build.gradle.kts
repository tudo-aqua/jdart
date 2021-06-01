import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.w3c.dom.Node
import java.io.ByteArrayOutputStream

fun gitCommitHash(): String{
    val os = ByteArrayOutputStream()
    project.exec {
        commandLine = "git rev-parse --verify --short HEAD".split(" ")
        standardOutput = os
    }
    return String(os.toByteArray()).trim()
}


group = "tools.aqua"
version = "0.1.0-${gitCommitHash()}"
description = "JDart is a dynamic symbolic execution enginen for Java"
java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
}
plugins {
    antlr
    id("com.github.johnrengelman.shadow") version "6.1.0"
    `java-library`
    `maven-publish`
    id("com.github.sherter.google-java-format") version "0.9"
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    antlr("org.antlr:antlr:3.5.2")
    api("com.google.guava:guava:30.1-jre")
    implementation("com.github.tudo-aqua.jconstraints:jconstraints-core:develop-SNAPSHOT")
    implementation("com.github.tudo-aqua.jconstraints:jconstraints-cvc4-all:develop-SNAPSHOT")
    implementation("com.github.tudo-aqua.jconstraints:jconstraints-z3-all:develop-SNAPSHOT")
    implementation("com.github.mmuesly.jpf-core:jpf-core:d848e44")
    implementation("com.github.mmuesly.jpf-core:jpf-annotations:d848e44")
    implementation("com.github.mmuesly.jpf-core:jpf-classes:d848e44")
    implementation("org.antlr:antlr-runtime:3.5.2")
    implementation("org.antlr:ST4:4.0.5")
    implementation("com.google.code.gson:gson:2.2.4")
    implementation("dk.brics:automaton:1.12-1")
}


sourceSets {
    main {
        java{
            setSrcDirs(listOf("src/main", "src/peers"))
        }
        resources{
            setSrcDirs(listOf("src/resources"))
        }
    }
    create("jdart-classes") {
        java{
            setSrcDirs(listOf("src/classes"))
        }
    }
    create("jdart-annotations") {
        java{
            setSrcDirs(listOf("src/annotations"))
        }
    }
    create("examples"){
        java{
            setSrcDirs(listOf("src/examples"))
        }
        compileClasspath += sourceSets["main"].runtimeClasspath
        compileClasspath += sourceSets["main"].output
        compileClasspath += sourceSets["jdart-annotations"].output
    }
}



val jdartAnnotationsJar by tasks.register<Jar>("jdartAnnotationsJar") {
    archiveBaseName.set("jdart-annotations")
    from(sourceSets["jdart-annotations"].output)
}

val jdartClassesJar by tasks.register<Jar>("jdartClassesJar") {
    archiveBaseName.set("jdart-classes")
    from(sourceSets["jdart-classes"].output)
}


val runJdartJar by tasks.register<ShadowJar>("runJdartJar") {
    archiveBaseName.set("RunJdartJPF")
    from(sourceSets["main"].output)
    dependsOn("classes")
    configurations = listOf(project.configurations["runtimeClasspath"])
    manifest {
        attributes["Implementation-Title"] =  "Java Pathfinder core launch system"
        attributes["Main-Class"] = "gov.nasa.jpf.tool.RunJPF"
    }
    mergeServiceFiles()
}

tasks.jar{
    dependsOn(jdartClassesJar)
    dependsOn(jdartAnnotationsJar)
    dependsOn(runJdartJar)
}
// publishing {
//     publications {
//         named<MavenPublication>("mavenJava") {
//             artifacts.clear()
//             artifact(tasks.shadowJar) { classifier = null }
//             pom {
//                 withXml {
//                     val elem = asElement()
//                     val dependencies = elem.getElementsByTagName("artifactId")
//                     repeat(dependencies.length) {
//                         val dep: Node? = dependencies.item(it)
//                         if (dep != null && dep.textContent == "jSMTLIB") {
//                             dep.parentNode.parentNode.removeChild(dep.parentNode)
//                         }
//                     }
//                 }
//             }
//         }
//     }
// }
