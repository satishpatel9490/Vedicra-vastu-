package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ReportEntity::class,
        AnalysisRowEntity::class,
        SiteMediaEntity::class,
        ExportedPdfEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VastuDatabase : RoomDatabase() {
    abstract fun vastuDao(): VastuDao

    companion object {
        @Volatile
        private var INSTANCE: VastuDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE analysis_rows ADD COLUMN problemCategory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE analysis_rows ADD COLUMN remedyPriority TEXT NOT NULL DEFAULT 'Medium'")
                db.execSQL("ALTER TABLE analysis_rows ADD COLUMN waitingPeriod TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE analysis_rows ADD COLUMN activitiesToStop TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE analysis_rows ADD COLUMN objectsToRemove TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE analysis_rows ADD COLUMN objectsToAdd TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE analysis_rows ADD COLUMN utilitiesToChange TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE analysis_rows ADD COLUMN structuralCorrections TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): VastuDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VastuDatabase::class.java,
                    "vedicra_vastu.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
