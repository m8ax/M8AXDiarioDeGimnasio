package com.mviiiax.m8ax_diariogimnasio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GlucosaDao {
    @Query("SELECT * FROM Glucosa ORDER BY id ASC")
    fun getAll(): List<Glucosa>

    @Query("SELECT * FROM Glucosa ORDER BY id DESC LIMIT 30")
    fun getUltimos30(): List<Glucosa>

    @Query("DELETE FROM Glucosa WHERE valor = 0")
    fun borrarRegistrosVacios()

    @Insert
    fun insert(glucosa: Glucosa)

    @Update
    fun update(glucosa: Glucosa)
}