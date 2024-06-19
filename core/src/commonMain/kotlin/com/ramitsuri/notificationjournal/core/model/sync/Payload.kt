package com.ramitsuri.notificationjournal.core.model.sync

import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Payload {
    abstract val action: Action

    abstract val sender: Sender

    @Serializable
    @SerialName("tags")
    data class Tags(
        val data: List<Tag>,
        override val action: Action,
        override val sender: Sender
    ) : Payload()

    @Serializable
    @SerialName("entries")
    data class Entries(
        val data: List<JournalEntry>,
        override val action: Action,
        override val sender: Sender
    ) : Payload()

    @Serializable
    @SerialName("templates")
    data class Templates(
        val data: List<JournalEntryTemplate>,
        override val action: Action,
        override val sender: Sender
    ) : Payload()
}


/*
@Serializable(with = PayloadSerializer::class)
data class Payload<T>(
    @SerialName("data")
    val data: T,

    @SerialName("type")
    val type: Type,

    @SerialName("action")
    val action: Action,

    @SerialName("sender")
    val sender: Sender,
)

class PayloadSerializer<T: Any>(private val dataSerializer: KSerializer<T>) : KSerializer<Payload<T>> {

    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun deserialize(decoder: Decoder): Payload<T> {
        val inp = decoder.beginStructure(descriptor)
        lateinit var data: T
        lateinit var type: Type
        lateinit var action: Action
        lateinit var sender: Sender
        loop@ while (true) {
            when (val i = inp.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> data = inp.decodeSerializableElement(descriptor, i, dataSerializer)
                1 -> type = inp.decodeSerializableElement(descriptor, i, Type.serializer())
                2 -> action = inp.decodeSerializableElement(descriptor, i, Action.serializer())
                3 -> sender = inp.decodeSerializableElement(descriptor, i, Sender.serializer())
                else -> throw SerializationException("Unknown index $i")
            }
        }
        inp.endStructure(descriptor)
        return Payload(data, type, action, sender)
    }

    override fun serialize(encoder: Encoder, value: Payload<T>) {
        val out = encoder.beginStructure(descriptor)
        out.encodeSerializableElement(descriptor, 0, dataSerializer, value.data)
        out.encodeSerializableElement(descriptor, 1, Type.serializer(), value.type)
        out.encodeSerializableElement(descriptor, 2, Action.serializer(), value.action)
        out.encodeSerializableElement(descriptor, 3, Sender.serializer(), value.sender)
        out.endStructure(descriptor)
    }
}
*/
