plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit)
    implementation("org.json:json:20260814")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation(libs.guava)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    // The API server the frontend talks to. Run the scraper first with `gradlew ingest`.
    mainClass = "predictbackend.InsightApiServer"
}

/** Reads awesomefinder/.env into a map so both `run` and `ingest` see NEMOTRON_API_KEY. */
fun dotenv(): Map<String, String> {
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) return emptyMap()
    return envFile.readLines()
        .filter { it.contains("=") && !it.trim().startsWith("#") }
        .associate { line ->
            val (key, value) = line.split("=", limit = 2)
            key.trim() to value.trim().removeSurrounding("\"")
        }
}

tasks.withType<JavaExec>().configureEach {
    environment(dotenv())
    // Pin the working directory so `run` and `ingest` share one signals.db
    // instead of each creating one in whatever directory they started in.
    workingDir = rootProject.projectDir
}

/** Scrape every company on the watchlist, label it, and write it to the SQLite database. */
tasks.register<JavaExec>("ingest") {
    group = "application"
    description = "Fetch job boards for the watchlist and store them in signals.db"
    mainClass = "predictbackend.Ingest"
    classpath = sourceSets["main"].runtimeClasspath
}
