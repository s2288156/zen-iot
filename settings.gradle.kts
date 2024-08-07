@file:Suppress("UnstableApiUsage")

rootProject.name = "zen-iot"

dependencyResolutionManagement {
    repositories {
        maven {
            setUrl("https://maven.aliyun.com/repository/public/")
        }
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            library("bcprov-jdk15on", "org.bouncycastle:bcprov-jdk15on:1.70")
            library("bcpkix-jdk15on", "org.bouncycastle:bcpkix-jdk15on:1.70")
            library("nimbus-jose-jwt", "com.nimbusds:nimbus-jose-jwt:9.23")
            library("mapstruct", "org.mapstruct:mapstruct:1.5.3.Final")
            library("mapstruct-processor", "org.mapstruct:mapstruct-processor:1.5.3.Final")
            library("j2mod", "com.ghgande:j2mod:3.1.1")
            library("netty-all", "io.netty:netty-all:4.1.85.Final")
            library("plc4j-api", "org.apache.plc4x:plc4j-api:0.12.0")
            library("plc4j-protocols-ads", "org.apache.plc4x:plc4x-protocols-ads:0.12.0")
            library("plc4j-driver-ads", "org.apache.plc4x:plc4j-driver-ads:0.12.0")
        }
    }
}
include("app-ui")
include("application")
include("dao")
include("service")
include("api")
include("start")
include("common")
include("common:util")
findProject(":common:util")?.name = "util"
include("common:mqtt")
findProject(":common:mqtt")?.name = "mqtt"
include("common:modbus")
findProject(":common:modbus")?.name = "modbus"
include("common:data")
findProject(":common:data")?.name = "data"
include("common:transport-api")
findProject(":common:transport-api")?.name = "transport-api"
include("tools")
include("workflow")
include("workflow:workflow-api")
findProject(":workflow:workflow-api")?.name = "workflow-api"
include("workflow:workflow-engine")
findProject(":workflow:workflow-engine")?.name = "workflow-engine"
include("common:ethernet-ip")
findProject(":common:ethernet-ip")?.name = "ethernet-ip"
include("common:ads")
findProject(":common:ads")?.name = "ads"
