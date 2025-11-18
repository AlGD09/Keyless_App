package com.example.keyless_app.data

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream


// Wrapper für die Liste – DataStore speichert immer EIN Objekt-Typ
@Serializable
data class UnlockedMachinesState(
    val machines: List<UnlockedMachine> = emptyList()
)

// Serializer für DataStore
object UnlockedMachinesSerializer : Serializer<UnlockedMachinesState> {

    override val defaultValue: UnlockedMachinesState = UnlockedMachinesState()

    override suspend fun readFrom(input: InputStream): UnlockedMachinesState {
        return try {
            Json.decodeFromString(
                UnlockedMachinesState.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            // Falls irgendwas korrupt ist -> leere Liste zurückgeben
            defaultValue
        }
    }

    override suspend fun writeTo(t: UnlockedMachinesState, output: OutputStream) {
        val json = Json.encodeToString(UnlockedMachinesState.serializer(), t)
        output.write(json.toByteArray())
    }
}