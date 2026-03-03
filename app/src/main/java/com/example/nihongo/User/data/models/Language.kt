package com.example.nihongo.User.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "languages")
data class Language(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String
)
