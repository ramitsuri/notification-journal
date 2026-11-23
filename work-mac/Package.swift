// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "WorkMacCore",
    platforms: [
        .macOS(.v14),
    ],
    products: [
        .library(
            name: "WorkMacCore",
            targets: ["WorkMacCore"]
        ),
        .executable(
            name: "WorkMacApp",
            targets: ["WorkMacApp"]
        ),
    ],
    targets: [
        .target(
            name: "WorkMacCore",
            path: "Sources/WorkMacCore"
        ),
        .executableTarget(
            name: "WorkMacApp",
            dependencies: ["WorkMacCore"],
            path: "Sources/WorkMacApp"
        ),
        .testTarget(
            name: "WorkMacCoreTests",
            dependencies: ["WorkMacCore"],
            path: "Tests/WorkMacCoreTests"
        ),
    ]
)
