import SwiftUI

public struct HUDView: View {
    @ObservedObject public var viewModel: HUDViewModel
    @FocusState private var isFieldFocused: Bool

    private static let timeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()

    public init(viewModel: HUDViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header / status
            HStack {
                Image(systemName: "pencil.line")
                    .foregroundColor(.accentColor)
                Text("Quick Capture")
                    .font(.headline)
                Spacer()
                Text("Esc to close")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

            // Input bar
            TextField("Type a thought and press Enter...", text: $viewModel.inputText)
                .textFieldStyle(.plain)
                .font(.system(size: 14))
                .focused($isFieldFocused)
                .onSubmit {
                    viewModel.submit()
                }
                .padding(10)
                .background(Color(NSColor.textBackgroundColor).opacity(0.6))
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.primary.opacity(0.15), lineWidth: 1)
                )
                .offset(x: viewModel.shakeOffset)
                .animation(.easeInOut(duration: 0.08), value: viewModel.shakeOffset)

            // Disconnected / error prompt
            if let error = viewModel.errorMessage {
                HStack(spacing: 6) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.red)
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                }
                .transition(.opacity)
            }

            // In-flight items list
            if !viewModel.inFlightItems.isEmpty {
                Divider()
                    .padding(.vertical, 2)

                VStack(spacing: 8) {
                    ForEach(viewModel.inFlightItems) { item in
                        InFlightRowView(
                            item: item,
                            timeString: Self.timeFormatter.string(from: item.timestamp),
                            onRetry: {
                                viewModel.retry(id: item.id)
                            },
                            onDiscard: {
                                viewModel.discard(id: item.id)
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
        .padding(16)
        .frame(width: 560)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(.ultraThinMaterial)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.primary.opacity(0.15), lineWidth: 1)
        )
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                isFieldFocused = true
            }
        }
        .onChange(of: viewModel.focusTrigger) { _, _ in
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                isFieldFocused = true
            }
        }
        .onExitCommand {
            viewModel.dismiss()
        }
    }
}
