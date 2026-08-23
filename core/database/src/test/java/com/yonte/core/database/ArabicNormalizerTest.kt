package com.yonte.core.database

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicNormalizerTest {
    @Test
    fun normalizesArabicAlefHamzaAndTaaMarbuta() {
        assertEquals("المدرسه", ArabicNormalizer.normalize("الْمَدْرَسَة"))
        assertEquals("اسلام", ArabicNormalizer.normalize("إسلام"))
    }

    @Test
    fun normalizesEnglishCaseAndWhitespace() {
        assertEquals("hello world", ArabicNormalizer.normalize("  Hello   World  "))
    }
}
