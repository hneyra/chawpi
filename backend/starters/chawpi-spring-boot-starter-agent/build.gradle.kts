plugins {
    id("chawpi.kotlin-library")
    id("chawpi.publishing")
}

// no ANTHROPIC_API_KEY, no assistant: the app still boots (EmbabelGate)
description = "Chawpi agent starter: the Chawpi starter plus chawpi-agent"

dependencies {
    api(project(":chawpi-spring-boot-starter"))
    api(project(":chawpi-agent"))
    // chawpi-agent is provider-neutral; the starter picks the original app's default provider
    api(libs.embabel.agent.starter.anthropic)
}
