import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("jvm") version "2.2.0"
	kotlin("plugin.spring") version "2.2.0"
	kotlin("plugin.jpa") version "2.2.0"

	kotlin("plugin.noarg") version "2.2.0"
	kotlin("plugin.allopen") version "2.2.0"
}

group = "example.spring.batch"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
	mavenCentral()
}

allOpen{
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

dependencies {
	// Spring Boot 関連の依存
	implementation("org.springframework.boot:spring-boot-starter-batch")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// Blaze-Persistence: Spring Boot 4 / Hibernate 7.1 を前提にした JPA クエリ最適化とページング
	implementation("com.blazebit:blaze-persistence-core-api-jakarta:1.6.20")
	implementation("com.blazebit:blaze-persistence-core-impl-jakarta:1.6.20")
	implementation("com.blazebit:blaze-persistence-integration-hibernate-7.1:1.6.20")

	// PostgreSQL ドライバ: 42.7.4 は CVE-2025-49146 の影響があり、修正版へ更新
	implementation("org.postgresql:postgresql:42.7.13")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.batch:spring-batch-test")
}

tasks.withType<KotlinCompile>().configureEach {
	compilerOptions {
		freeCompilerArgs.add("-Xjsr305=strict")
		jvmTarget.set(JvmTarget.JVM_21)
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
