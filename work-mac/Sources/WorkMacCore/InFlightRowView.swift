import SwiftUI

public struct InFlightRowView: View {
    public let item: InFlightItem
    public let timeString: String
    public let onRetry: () -> Void
    public let onDiscard: () -> Void

    public init(
        item: InFlightItem,
        timeString: String,
        onRetry: @escaping () -> Void,
        onDiscard: @escaping () -> Void
    ) {
        self.item = item
        self.timeString = timeString
        self.onRetry = onRetry
        self.onDiscard = onDiscard
    }

    public var body: some View {
        HStack(spacing: 8) {
            Text(item.text)
                .font(.system(size: 13))
                .lineLimit(2)
                .foregroundColor(.primary)

            Spacer()

            Text(timeString)
                .font(.caption2)
                .foregroundColor(.secondary)

            statusView
        }
        .padding(.vertical, 4)
        .padding(.horizontal, 8)
        .background(Color.primary.opacity(0.04))
        .cornerRadius(6)
    }

    @ViewBuilder
    private var statusView: some View {
        switch item.status {
        case .pending:
            ProgressView()
                .controlSize(.small)
                .frame(width: 16, height: 16)

        case .acknowledged:
            HStack(spacing: 4) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
                    .font(.system(size: 14))
                Text("Sent")
                    .font(.caption)
                    .foregroundColor(.green)
            }

        case .failed(let reason):
            HStack(spacing: 6) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.red)
                    .font(.system(size: 14))

                Text(reason)
                    .font(.caption)
                    .foregroundColor(.red)
                    .lineLimit(1)

                Button("Retry", action: onRetry)
                    .font(.caption)
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)

                Button("Discard", action: onDiscard)
                    .font(.caption)
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            }
        }
    }
}
