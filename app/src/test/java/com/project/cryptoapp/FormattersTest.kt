package com.project.cryptoapp

import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.domain.model.OperationStatus
import com.project.cryptoapp.domain.model.OperationType
import com.project.cryptoapp.util.toDurationDisplay
import com.project.cryptoapp.util.toJsonExport
import com.project.cryptoapp.util.toPlainTextExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {
    @Test
    fun durationDisplayUsesReadablePrecision() {
        assertEquals("Not measured", null.toDurationDisplay())
        assertEquals("900 ns", 900L.toDurationDisplay())
        assertEquals("1.500 us", 1_500L.toDurationDisplay())
        assertEquals("1.500 ms", 1_500_000L.toDurationDisplay())
    }

    @Test
    fun historyExportIncludesRawAndReadableDuration() {
        val history = listOf(
            CryptoHistory(
                operationType = OperationType.SIGN,
                inputText = "Message signing",
                outputText = "Signature generated",
                status = OperationStatus.SUCCESS,
                timestamp = 0L,
                durationNanos = 1_500_000L,
            ),
        )

        assertTrue(history.toPlainTextExport().contains("durationNanos=1500000"))
        assertTrue(history.toPlainTextExport().contains("duration=1.500 ms"))
        assertTrue(history.toJsonExport().contains("\"durationNanos\": 1500000"))
    }
}
