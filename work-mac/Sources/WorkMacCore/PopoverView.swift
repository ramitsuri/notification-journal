import AppKit
import SwiftUI

public struct PopoverView: View {
    @ObservedObject public var appState: WorkAppState
    @ObservedObject public var hudViewModel: HUDViewModel
    @FocusState private var isFieldFocused: Bool

    private static let timeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()

    public init(appState: WorkAppState) {
        self.appState = appState
        self.hudViewModel = appState.hudViewModel
    }

    public var body: some View {
        VStack(spacing: 12) {
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

            // Quick Capture Input Section
            VStack(alignment: .leading, spacing: 6) {
                TextField("Type a thought and press Enter...", text: $hudViewModel.inputText)
                    .textFieldStyle(.plain)
                    .font(.system(size: 13))
                    .focused($isFieldFocused)
                    .onSubmit {
                        hudViewModel.submit()
                    }
                    .padding(8)
                    .background(Color(NSColor.textBackgroundColor).opacity(0.6))
                    .cornerRadius(6)
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(Color.primary.opacity(0.15), lineWidth: 1)
                    )
                    .offset(x: hudViewModel.shakeOffset)
                    .animation(.easeInOut(duration: 0.08), value: hudViewModel.shakeOffset)

                if let error = hudViewModel.errorMessage {
                    HStack(spacing: 4) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                            .font(.caption2)
                        Text(error)
                            .font(.caption2)
                            .foregroundColor(.red)
                    }
                    .transition(.opacity)
                }

                if !hudViewModel.inFlightItems.isEmpty {
                    VStack(spacing: 6) {
                        ForEach(hudViewModel.inFlightItems) { item in
                            InFlightRowView(
                                item: item,
                                timeString: Self.timeFormatter.string(from: item.timestamp),
                                onRetry: {
                                    hudViewModel.retry(id: item.id)
                                },
                                onDiscard: {
                                    hudViewModel.discard(id: item.id)
                                }
                            )
                            .transition(.asymmetric(
                                insertion: .opacity.combined(with: .move(edge: .top)),
                                removal: .opacity
                            ))
                        }
                    }
                }
            }

            Divider()

            // QR Code Presentation
            if appState.isQrCodeVisible {
                VStack(spacing: 8) {
                    QRCodeView(text: appState.pairingString, size: 160)

                    Text("\(appState.currentHost):\(appState.port)")
                        .font(.system(.caption2, design: .monospaced))
                        .foregroundColor(.secondary)

                    HStack(spacing: 8) {
                        Button(action: {
                            Task {
                                await appState.rotateSecrets()
                            }
                        }) {
                            Label("Rotate Secrets", systemImage: "arrow.triangle.2.circlepath")
                                .font(.caption2)
                        }
                        .buttonStyle(.bordered)
                        .help("Drops current socket connection, generates new ephemeral keys in memory, and updates the QR code.")

                        if appState.isClientConnected {
                            Button(action: {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    appState.isQrCodeRevealed = false
                                }
                            }) {
                                Label("Hide QR", systemImage: "eye.slash")
                                    .font(.caption2)
                            }
                            .buttonStyle(.bordered)
                            .help("Hide pairing QR code.")
                        }
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            } else {
                HStack {
                    Label("Phone connected", systemImage: "link.circle.fill")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Spacer()

                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            appState.isQrCodeRevealed = true
                        }
                    }) {
                        Label("Show QR Code", systemImage: "qrcode")
                            .font(.caption2)
                    }
                    .buttonStyle(.bordered)
                    .help("Show pairing QR code to view connection details or pair another device.")
                }
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
                            Text("\(iface.displayName) (\(iface.ipAddress))")
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
        .frame(width: 340)
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                isFieldFocused = true
            }
        }
        .onChange(of: hudViewModel.focusTrigger) { _, _ in
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                isFieldFocused = true
            }
        }
        .onExitCommand {
            hudViewModel.dismiss()
        }
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
