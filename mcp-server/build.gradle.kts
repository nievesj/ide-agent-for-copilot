plugins {
    id("java")
    application
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:${providers.gradleProperty("junitVersion").get()}")
}

application {
    mainClass.set("com.github.copilot.mcp.McpStdioProxy")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.github.copilot.mcp.McpStdioProxy"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
