package com.mviiiax.m8ax_diariogimnasio

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Glucosa(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, val fechaHora: String, var valor: Int
)