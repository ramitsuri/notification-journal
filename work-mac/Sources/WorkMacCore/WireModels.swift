import Foundation

public enum WireDateFormatters {
    public static let localDateTime: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone.current
        return formatter
    }()
}

public struct JournalEntry: Codable, Equatable, Sendable {
    public let id: String
    public let entryTime: String
    public let text: String
    public let tag: String
    public let uploaded: Bool
    public let autoTagged: Bool
    public let deleted: Bool
    public let reconciled: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case entryTime
        case text
        case tag
        case uploaded
        case autoTagged = "auto_tagged"
        case deleted
        case reconciled
    }

    public init(
        id: String = UUID().uuidString.lowercased(),
        entryTime: String = WireDateFormatters.localDateTime.string(from: Date()),
        text: String,
        tag: String = "internal_no_tag_value",
        uploaded: Bool = false,
        autoTagged: Bool = false,
        deleted: Bool = false,
        reconciled: Bool = false
    ) {
        self.id = id
        self.entryTime = entryTime
        self.text = text
        self.tag = tag
        self.uploaded = uploaded
        self.autoTagged = autoTagged
        self.deleted = deleted
        self.reconciled = reconciled
    }
}

public struct Sender: Codable, Equatable, Sendable {
    public let name: String
    public let id: String

    public init(name: String = "Work Mac", id: String = "work-mac") {
        self.name = name
        self.id = id
    }
}

public struct EntriesPayload: Codable, Equatable, Sendable {
    public let type: String
    public let data: [JournalEntry]
    public let sender: Sender

    public init(data: [JournalEntry], sender: Sender = Sender()) {
        self.type = "entries"
        self.data = data
        self.sender = sender
    }
}

public struct OutgoingDataMessage: Codable, Equatable, Sendable {
    public let type: String
    public let id: String
    public let payload: EntriesPayload

    public init(
        id: String = UUID().uuidString.lowercased(),
        payload: EntriesPayload
    ) {
        self.type = "data"
        self.id = id
        self.payload = payload
    }
}

public struct IncomingAck: Codable, Equatable, Sendable {
    public let type: String
    public let id: String

    public init(id: String) {
        self.type = "ack"
        self.id = id
    }
}
