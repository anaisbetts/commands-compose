/*
 * Copyright (C) 2020 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.net.URL
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.0.0"
//  id("org.jetbrains.dokka") version "1.9.20"
  id("com.vanniktech.maven.publish") version "0.29.0"
//  id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

repositories { mavenCentral() }

java {
  toolchain { languageVersion.set(JavaLanguageVersion.of(11)) }
  tasks.withType<JavaCompile>().configureEach { options.release.set(8) }
}

tasks.withType<KotlinCompile>().configureEach {
  val isTest = name == "compileTestKotlin"
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_1_8)
    freeCompilerArgs.add("-progressive")
    if (isTest) {
      freeCompilerArgs.add("-opt-in=kotlin.ExperimentalStdlibApi")
    }
  }
}

tasks.withType<Detekt>().configureEach { jvmTarget = "1.8" }

/* 
tasks.named<DokkaTask>("dokkaHtml") {
  outputDirectory.set(rootDir.resolve("docs/0.x"))
  dokkaSourceSets.configureEach {
    skipDeprecated.set(true)
    externalDocumentationLink { url.set(URL("https://square.github.io/moshi/1.x/moshi/")) }
    // No GSON doc because they host on javadoc.io, which Dokka can't parse.
  }
}
*/

kotlin { explicitApi() }

dependencies {

  testImplementation("junit:junit:4.13.2")
}