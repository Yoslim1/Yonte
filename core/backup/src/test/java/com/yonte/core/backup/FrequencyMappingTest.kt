package com.yonte.core.backup

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class FrequencyMappingTest {

    @Test
    fun `weekly maps to 7 days`() {
        assertEquals(Duration.ofDays(7), frequencyToDuration(BackupFrequency.WEEKLY))
    }

    @Test
    fun `biweekly maps to 14 days`() {
        assertEquals(Duration.ofDays(14), frequencyToDuration(BackupFrequency.BIWEEKLY))
    }

    @Test
    fun `monthly maps to 30 days`() {
        assertEquals(Duration.ofDays(30), frequencyToDuration(BackupFrequency.MONTHLY))
    }

    @Test
    fun `off maps to zero duration`() {
        assertEquals(Duration.ZERO, frequencyToDuration(BackupFrequency.OFF))
    }
}
