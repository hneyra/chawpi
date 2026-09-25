plugins {
    id("chawpi.spring-module")
    id("chawpi.publishing")
    id("chawpi.integration-test")
}

description = "Chawpi agent: an AI assistant over the caller's own data, on Embabel"

dependencies {
    api(project(":chawpi-core"))
    // AgentService takes embabel's AgentPlatform: part of the api. provider-neutral -- EmbabelGate
    // only ever names the anthropic auto-config by string, never links against it. the default
    // provider (anthropic) is the starter's choice, made in chawpi-spring-boot-starter-agent.
    api(libs.embabel.agent.starter)
    // available_transitions asks workflow when an app has it: compileOnly, guarded by @ConditionalOnClass
    compileOnly(project(":chawpi-workflow"))

    testImplementation(project(":chawpi-test"))
    testImplementation(project(":chawpi-workflow"))
}
