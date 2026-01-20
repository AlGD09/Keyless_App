package com.example.keyless_app.ablauf

import com.example.keyless_app.data.BleEvent
import com.example.keyless_app.data.LockResult
import com.example.keyless_app.data.UnlockedMachine
import com.example.keyless_app.data.MainRepository
import com.example.keyless_app.testing.MainDispatcherRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AblaufViewModelIntegrationTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: MainRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun revokeAccessOnAbort_triggersLockAndRemovesUnlockedMachine() = runTest(dispatcher) {
        val rcuId = "rcu-123"
        val bleEvents = MutableSharedFlow<BleEvent>(extraBufferCapacity = 1)
        val unlockedMachines = MutableStateFlow<List<UnlockedMachine>>(emptyList())
        val lockedRcuIds = mutableListOf<String>()
        val savedUnlockedMachines = mutableListOf<List<UnlockedMachine>>()
        var stopScanCalls = 0
        var rssiCallback: ((String, Int) -> Unit)? = null

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.keyless_app", context.packageName)

        every { repository.bleEvents } returns bleEvents.asSharedFlow()
        every { repository.unlockedMachinesFlow() } returns unlockedMachines
        coEvery { repository.saveUnlockedMachines(any()) } coAnswers {
            val machines = firstArg<List<UnlockedMachine>>()
            savedUnlockedMachines.add(machines)
            unlockedMachines.value = machines
        }
        coEvery { repository.lockMachine(rcuId) } coAnswers {
            lockedRcuIds.add(rcuId)
            LockResult.ACCEPTED
        }
        every { repository.startGlobalRssiScan(any(), any()) } answers {
            rssiCallback = secondArg<(String, Int) -> Unit>()
        }
        every { repository.stopGlobalRssiScan() } answers {
            stopScanCalls += 1
        }
        val viewModel = AblaufViewModel(repository)

        bleEvents.emit(BleEvent.Unlocked(rcuId))

        advanceUntilIdle()
        advanceTimeBy(4_000)
        advanceUntilIdle()

        rssiCallback?.invoke(rcuId, -70)
        advanceUntilIdle()

        assertEquals(listOf(rcuId), lockedRcuIds)
        assertTrue(viewModel.unlockedMachines.value.isEmpty())
        assertTrue(savedUnlockedMachines.last().isEmpty())
        assertTrue(stopScanCalls > 0)
    }
}