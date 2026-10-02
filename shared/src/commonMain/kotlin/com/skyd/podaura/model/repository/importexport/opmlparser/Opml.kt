package com.skyd.podaura.model.repository.importexport.opmlparser

import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.model.repository.importexport.opmlparser.entity.Opml
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import net.devrieze.xmlutil.serialization.kxio.encodeToSink
import net.devrieze.xmlutil.serialization.kxio.decodeFromSource
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig

class OPML(
    val config: OpmlConfiguration,
    serializersModule: SerializersModule = EmptySerializersModule()
) : StringFormat {
    internal val xml = XML(config.xmlConfig, serializersModule)

    override val serializersModule: SerializersModule = xml.serializersModule

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String =
        xml.encodeToString(serializer, value)

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T =
        xml.decodeFromString(deserializer, string)
}

fun OPML(
    serializersModule: SerializersModule = EmptySerializersModule(),
    builderAction: OpmlBuilder.() -> Unit = {},
): OPML {
    val builder = OpmlBuilder()
    builder.builderAction()
    val conf = builder.build()
    return OPML(conf, serializersModule)
}

class OpmlBuilder internal constructor() {
    internal var xmlConfig: XmlConfig = XML {
        autoPolymorphic = true
        indentString = "  "
        defaultPolicy {
            pedantic = false
            ignoreUnknownChildren()
        }
    }.config

    internal fun build(): OpmlConfiguration {
        return OpmlConfiguration(
            xmlConfig = xmlConfig,
        )
    }
}

fun OPML.decodeFromSource(source: Source): Opml {
    // xmlutil 0.91.3's Native Source reader mishandles UTF-8 bytes and buffer refills.
    // Keep byte-based encoding detection on Android/JVM.
    return if (platform == Platform.iOS || platform == Platform.macOS_Native) {
        xml.decodeFromString(Opml.serializer(), source.readString())
    } else {
        xml.decodeFromSource(Opml.serializer(), source)
    }
}

fun OPML.encodeToSink(sink: Sink, value: Opml) {
    xml.encodeToSink(sink, Opml.serializer(), value)
}
