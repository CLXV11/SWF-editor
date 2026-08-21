package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectName: String,
    val originalFileName: String,
    val originalFileUri: String,
    val cachedSwfPath: String,
    val totalTexts: Int,
    val modifiedTextsCount: Int,
    val lastModifiedTime: Long = System.currentTimeMillis(),
    val modificationsJson: String = "{}",
    val swfVersion: Int = 0,
    val fileSize: Long = 0
)
