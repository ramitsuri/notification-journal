import Testing
import Foundation
@testable import WorkMacCore

struct WireModelsTests {
    @Test func localDateTimeFormattingHasNoTimezoneOrZ() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone.current

        var components = DateComponents()
        components.year = 2026
        components.month = 11
        components.day = 21
        components.hour = 21
        components.minute = 15
        components.second = 30

        let date = calendar.date(from: components)!
        let formatted = WireDateFormatters.localDateTime.string(from: date)

        #expect(formatted == "2026-11-21T21:15:30")
        #expect(!formatted.contains("Z"))
        #expect(!formatted.contains("+"))
        #expect(!formatted.contains("-0"))
        #expect(!formatted.contains("+0"))
    }

    @Test func journalEntryJsonSerializationMatchesSchema() throws {
        let entry = JournalEntry(
            id: "7b82405e-f831-419b-bc75-123456789abc",
            entryTime: "2026-11-21T21:15:00",
            text: "Meeting notes with team",
            tag: "internal_no_tag_value",
            uploaded: false,
            autoTagged: false,
            deleted: false,
            reconciled: false
        )

        let payload = EntriesPayload(
            data: [entry],
            sender: Sender(name: "Work Mac", id: "work-mac")
        )

        let message = OutgoingDataMessage(
            id: "e9a0378f-62e9-4e4b-b5d1-987654321000",
            payload: payload
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        let jsonData = try encoder.encode(message)

        let jsonObject = try JSONSerialization.jsonObject(with: jsonData) as? [String: Any]
        #expect(jsonObject?["type"] as? String == "data")
        #expect(jsonObject?["id"] as? String == "e9a0378f-62e9-4e4b-b5d1-987654321000")

        let payloadObj = jsonObject?["payload"] as? [String: Any]
        #expect(payloadObj?["type"] as? String == "entries")

        let senderObj = payloadObj?["sender"] as? [String: Any]
        #expect(senderObj?["name"] as? String == "Work Mac")
        #expect(senderObj?["id"] as? String == "work-mac")

        let entriesList = payloadObj?["data"] as? [[String: Any]]
        #expect(entriesList?.count == 1)
        let firstEntry = entriesList?.first
        #expect(firstEntry?["id"] as? String == "7b82405e-f831-419b-bc75-123456789abc")
        #expect(firstEntry?["entryTime"] as? String == "2026-11-21T21:15:00")
        #expect(firstEntry?["text"] as? String == "Meeting notes with team")
        #expect(firstEntry?["tag"] as? String == "internal_no_tag_value")
        #expect(firstEntry?["uploaded"] as? Bool == false)
        #expect(firstEntry?["auto_tagged"] as? Bool == false)
        #expect(firstEntry?["deleted"] as? Bool == false)
        #expect(firstEntry?["reconciled"] as? Bool == false)
    }

    @Test func incomingAckDeserialization() throws {
        let ackJson = """
        {"type": "ack", "id": "e9a0378f-62e9-4e4b-b5d1-987654321000"}
        """
        let data = ackJson.data(using: .utf8)!
        let ack = try JSONDecoder().decode(IncomingAck.self, from: data)

        #expect(ack.type == "ack")
        #expect(ack.id == "e9a0378f-62e9-4e4b-b5d1-987654321000")
    }
}
