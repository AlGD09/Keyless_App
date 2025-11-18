package com.example.keyless_app.data

import android.content.Context
import androidx.datastore.dataStore

val Context.unlockedMachinesDataStore by dataStore(
    fileName = "unlocked_machines.json",
    serializer = UnlockedMachinesSerializer
)
