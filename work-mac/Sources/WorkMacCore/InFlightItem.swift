import Foundation

public enum InFlightStatus: Equatable, Sendable {
    case pending
    case acknowledged
    case failed(String)
}

public struct InFlightItem: Identifiable, Equatable, Sendable {
    public let id: UUID
    public let text: String
    public let timestamp: Date
    public var status: InFlightStatus

    public init(
        id: UUID = UUID(),
        text: String,
        timestamp: Date = Date(),
        status: InFlightStatus = .pending
    ) {
        self.id = id
        self.text = text
        self.timestamp = timestamp
        self.status = status
    }
}
