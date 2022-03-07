package com.example.todolist.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity

data class ToDoEntity(
    @PrimaryKey(autoGenerate = true) var id : Int? = null,
    @ColumnInfo(name = "title") var title : String,
    @ColumnInfo(name = "importance") var importance : Int
)