package com.mviiiax.m8ax_diariogimnasio

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Gimnasio(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var fechaHora: String,
    var valor: Int,
    var diario: String = ""
)