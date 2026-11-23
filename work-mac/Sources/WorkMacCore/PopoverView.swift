import AppKit
import SwiftUI

public struct PopoverView: View {
    @ObservedObject public var appState: WorkAppState

    public init(appState: WorkAppState) {
        self.appState = appState
    }

    public var body: some View {
        VStack(spacing: 14) {
            // Header & Status
            HStack {
                Text("NotificationJournal")
                    .font(.headline)

                Spacer()

                HStack(spacing: 6) {
                    Circle()
                        .fill(statusColor)
                        .frame(width: 8, height: 8)
                    Text(statusText)
                        .font(.caption)
                        .foregroundColor(statusColor)
                }
            }

            // QR Code Presentation
            VStack(spacing: 8) {
                QRCodeView(text: appState.pairingString, size: 170)

                Text("\(appState.currentHost):\(appState.port)")
                    .font(.system(.caption2, design: .monospaced))
                    .foregroundColor(.secondary)

                Button(action: {
                    Task {
                        await appState.rotateSecrets()
                    }
                }) {
                    Label("Rotate Secrets / Re-pair", systemImage: "arrow.triangle.2.circlepath")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
                .help("Drops current socket connection, generates new ephemeral keys in memory, and updates the QR code.")
            }

            Divider()

            // Network Interface Controls
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Network Interface")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    Button(action: {
                        appState.refreshInterfaces()
                    }) {
                        Image(systemName: "arrow.clockwise")
                            .font(.caption)
                    }
                    .buttonStyle(.borderless)
                    .help("Refresh network interfaces")
                    .disabled(appState.isUsbMode)
                }

                if appState.availableInterfaces.isEmpty {
                    Text("No active LAN interface found")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                } else {
                    Picker("", selection: $appState.selectedInterface) {
                        ForEach(appState.availableInterfaces) { iface in
                            Text("\(iface.name) (\(iface.ipAddress))")
                                .tag(Optional(iface))
                        }
                    }
                    .pickerStyle(.menu)
                    .disabled(appState.isUsbMode)
                }
            }

            // USB Mode (127.0.0.1) & ADB Reverse Command
            VStack(alignment: .leading, spacing: 6) {
                Toggle("USB Mode (127.0.0.1)", isOn: $appState.isUsbMode)
                    .toggleStyle(.switch)
                    .font(.caption)

                VStack(alignment: .leading, spacing: 4) {
                    Text("Port reverse command:")
                        .font(.caption2)
                        .foregroundColor(.secondary)

                    HStack {
                        Text(appState.adbReverseCommand)
                            .font(.system(.caption2, design: .monospaced))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 4)
                            .background(Color(NSColor.controlBackgroundColor))
                            .cornerRadius(4)

                        Spacer()

                        Button(action: {
                            appState.copyAdbCommand()
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: appState.adbCopied ? "checkmark" : "doc.on.doc")
                                Text(appState.adbCopied ? "Copied" : "Copy")
                            }
                            .font(.caption2)
                        }
                        .buttonStyle(.bordered)
                    }
                }
            }

            Divider()

            // Footer
            HStack {
                Text("RAM only · Zero disk footprint")
                    .font(.caption2)
                    .foregroundColor(.secondary)

                Spacer()

                Button("Quit") {
                    NSApplication.shared.terminate(nil)
                }
                .font(.caption2)
                .buttonStyle(.borderless)
            }
        }
        .padding(14)
        .frame(width: 320)
    }

    private var statusColor: Color {
        if appState.serverError != nil {
            return .red
        }
        return appState.isClientConnected ? .green : .orange
    }

    private var statusText: String {
        if appState.serverError != nil {
            return "Server error"
        }
        return appState.isClientConnected ? "Connected" : "Waiting for phone"
    }
}
