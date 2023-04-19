plugins {
    application
    id("sw.shadow-conventions")
    id("edu.sc.seis.launch4j") version "2.5.4"
}

application {
    mainClass.set("net.pistonmaster.serverwrecker.Main")
}

dependencies {
    implementation(projects.serverwreckerCommon)
    implementation(projects.serverwreckerProtocol)

    implementation("info.picocli:picocli:4.7.3")
    annotationProcessor("info.picocli:picocli-codegen:4.7.3")

    implementation("com.mojang:brigadier:1.1.8")
    implementation("com.formdev:flatlaf:3.1.1")
    implementation("org.pf4j:pf4j:3.9.0")

    implementation("com.thealtening.api:api:4.1.0")
    implementation("com.google.guava:guava:31.1-jre")

    implementation("net.kyori:adventure-text-serializer-plain:4.13.1")
    implementation("net.kyori:adventure-text-serializer-gson:4.13.1")
    implementation("net.kyori:event-api:3.0.0")
}

tasks.compileJava.get().apply {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

tasks.named<Jar>("jar").get().manifest {
    attributes["Main-Class"] = "net.pistonmaster.serverwrecker.Main"
}

launch4j {
    mainClassName = "net.pistonmaster.serverwrecker.Main"
    icon = "${projectDir}/assets/robot.ico"
}
