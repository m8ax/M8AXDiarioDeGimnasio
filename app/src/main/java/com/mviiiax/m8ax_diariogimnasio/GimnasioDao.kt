package com.mviiiax.m8ax_diariogimnasio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GimnasioDao {
    @Query("SELECT * FROM Gimnasio ORDER BY id ASC")
    fun getAll(): List<Gimnasio>

    @Insert
    fun insert(gimnasio: Gimnasio)

    @Update
    fun update(gimnasio: Gimnasio)

    @Query("DELETE FROM Gimnasio WHERE id = :id")
    fun deleteById(id: Int)

    @Query("UPDATE Gimnasio SET diario = :texto WHERE id = :id")
    fun updateDiario(id: Int, texto: String)

    @Query("SELECT diario FROM Gimnasio WHERE id = :id")
    fun getDiario(id: Int): String

    @Query(
        """
    SELECT * FROM gimnasio
    WHERE valor > 0
    AND substr(fechaHora, 7, 4) = :yearStr
"""
    )
    fun getEntrenadosPorAno(yearStr: String): List<Gimnasio>

    @Query("SELECT fechaHora FROM Gimnasio WHERE strftime('%Y', fechaHora) = :anio AND strftime('%m', fechaHora) = :mes")
    fun obtenerDiasConDatos(anio: String, mes: String): List<String>

    @Query("SELECT COUNT(*) FROM gimnasio")
    fun getTotalRegistros(): Int

    @Query("SELECT SUM(valor) FROM gimnasio WHERE valor > 0")
    fun getSumaValoresPositivos(): Double?

    @Query(
        """
    SELECT strftime('%d/%m/%Y', MIN(substr(fechaHora, 7, 4) || '-' || substr(fechaHora, 4, 2) || '-' || substr(fechaHora, 1, 2))) 
    FROM gimnasio
"""
    )
    fun getFechaInicio(): String?

    @Query(
        """
    SELECT strftime('%d/%m/%Y', MAX(substr(fechaHora, 7, 4) || '-' || substr(fechaHora, 4, 2) || '-' || substr(fechaHora, 1, 2))) 
    FROM gimnasio
"""
    )
    fun getFechaFin(): String?
}