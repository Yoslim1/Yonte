package com.yonte.feature.settings

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import com.yonte.core.backup.BackupFrequency
import com.yonte.core.backup.BackupGateway
import com.yonte.core.backup.ScheduledBackupWorker
import com.yonte.core.database.NoteRepository
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.update.UpdateGateway
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SettingsViewModelTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockContext: Context
    private lateinit var mockRepository: NoteRepository
    private lateinit var mockBackupGateway: BackupGateway
    private lateinit var mockUpdateGateway: UpdateGateway
    private lateinit var mockLocalKeyManager: LocalKeyManager

    @Before
    fun setUp() {
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)
        mockContext = mock(Context::class.java)
        mockRepository = mock(NoteRepository::class.java)
        mockBackupGateway = mock(BackupGateway::class.java)
        mockUpdateGateway = mock(UpdateGateway::class.java)
        mockLocalKeyManager = mock(LocalKeyManager::class.java)

        `when`(mockContext.getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(mockEditor)
        `when`(mockPrefs.getString(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(null)
    }

    @Test
    fun `frequency change persists to prefs and schedules auto backup`() {
        val viewModel = createViewModel()
        val contentResolver = mock(ContentResolver::class.java)

        mockStatic(com.yonte.core.backup.AutoBackupScheduler::class.java).use { mockedScheduler ->
            viewModel.setAutoBackupFrequency(BackupFrequency.WEEKLY, contentResolver)

            verify(mockEditor).putString("auto_backup_frequency", BackupFrequency.WEEKLY.name)
            verify(mockEditor).apply()
            assertEquals(BackupFrequency.WEEKLY, viewModel.uiState.value.frequency)
            mockedScheduler.verify { com.yonte.core.backup.AutoBackupScheduler.schedule(mockContext, BackupFrequency.WEEKLY) }
        }
    }

    @Test
    fun `setting frequency to OFF calls clearAutoBackupKey`() {
        val viewModel = createViewModel()
        val contentResolver = mock(ContentResolver::class.java)

        viewModel.setAutoBackupFrequency(BackupFrequency.OFF, contentResolver)

        verify(mockLocalKeyManager).clearAutoBackupKey()
        assertEquals(BackupFrequency.OFF, viewModel.uiState.value.frequency)
    }

    @Test
    fun `setting frequency to non-OFF does not call clearAutoBackupKey`() {
        val viewModel = createViewModel()
        val contentResolver = mock(ContentResolver::class.java)

        viewModel.setAutoBackupFrequency(BackupFrequency.MONTHLY, contentResolver)

        verify(mockLocalKeyManager, org.mockito.Mockito.never()).clearAutoBackupKey()
        assertEquals(BackupFrequency.MONTHLY, viewModel.uiState.value.frequency)
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            repository = mockRepository,
            backupGateway = mockBackupGateway,
            updateGateway = mockUpdateGateway,
            localKeyManager = mockLocalKeyManager,
            currentVersionCode = 1,
            appContext = mockContext,
        )
    }
}
