package com.yonte.app

import android.content.Context
import com.yonte.core.backup.BackupGateway
import com.yonte.core.backup.BackupService
import com.yonte.core.database.NoteRepository
import com.yonte.core.database.YonteDatabase
import com.yonte.core.security.EncryptionManager
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.update.UpdateGateway
import com.yonte.core.update.UpdateService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YonteAppModule {
    @Provides
    @Singleton
    fun provideEncryptionManager(): EncryptionManager = EncryptionManager()

    @Provides
    @Singleton
    fun provideLocalKeyManager(
        @ApplicationContext context: Context,
        encryptionManager: EncryptionManager,
    ): LocalKeyManager = LocalKeyManager(context, encryptionManager)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        localKeyManager: LocalKeyManager,
    ): YonteDatabase {
        val key = localKeyManager.cachedSessionKey()
            ?: error("YonteDatabase requested before onboarding/unlock completed")
        return YonteDatabase.get(context, key)
    }

    @Provides
    @Singleton
    fun provideNoteRepository(database: YonteDatabase): NoteRepository = NoteRepository(database)

    @Provides
    @Singleton
    fun provideBackupGateway(encryptionManager: EncryptionManager): BackupGateway = BackupService(encryptionManager)

    @Provides
    @Singleton
    fun provideUpdateGateway(@ApplicationContext context: Context): UpdateGateway = UpdateService(context)
}
