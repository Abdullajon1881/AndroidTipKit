package dev.nudgekit.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreFirstShownTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun TestScope.createManager(clock: () -> Long): DataStoreTipManager {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.root, "test.preferences_pb") },
        )
        return DataStoreTipManager(dataStore, clock = clock)
    }

    @Test
    fun `fresh state has null firstShownAtMillis`() = runTest {
        val manager = createManager(clock = { 1_000L })
        assertThat(manager.getTipState("tip").firstShownAtMillis).isNull()
    }

    @Test
    fun `markShown stamps firstShownAtMillis once and never overwrites it`() = runTest {
        var t = 100L
        val manager = createManager(clock = { t })

        manager.markShown("tip")
        assertThat(manager.getTipState("tip").firstShownAtMillis).isEqualTo(100L)
        assertThat(manager.getTipState("tip").lastShownAtMillis).isEqualTo(100L)

        t = 900L
        manager.markShown("tip")
        assertThat(manager.getTipState("tip").firstShownAtMillis).isEqualTo(100L)
        assertThat(manager.getTipState("tip").lastShownAtMillis).isEqualTo(900L)
    }

    @Test
    fun `reset clears firstShownAtMillis`() = runTest {
        val manager = createManager(clock = { 100L })
        manager.markShown("tip")
        assertThat(manager.getTipState("tip").firstShownAtMillis).isEqualTo(100L)

        manager.reset("tip")
        assertThat(manager.getTipState("tip").firstShownAtMillis).isNull()
    }
}
