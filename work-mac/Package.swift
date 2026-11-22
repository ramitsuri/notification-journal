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
    ],
    targets: [
        .target(
            name: "WorkMacCore",
            path: "Sources/WorkMacCore"
        ),
        .testTarget(
            name: "WorkMacCoreTests",
            dependencies: ["WorkMacCore"],
            path: "Tests/WorkMacCoreTests"
        ),
    ]
)
