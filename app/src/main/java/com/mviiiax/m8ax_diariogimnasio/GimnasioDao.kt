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

    @Query("SELECT * FROM gimnasio ORDER BY RANDOM() LIMIT :cantidad")
    fun getRegistrosAleatorios(cantidad: Int): List<Gimnasio>

    @Query("SELECT SUM(valor) FROM gimnasio WHERE valor > 0")
    fun getSumaValoresPositivos(): Double?

    @Query("SELECT COUNT(*) FROM Gimnasio")
    fun getTotalRegistros(): Int

    @Query("SELECT COUNT(*) FROM Gimnasio WHERE valor > 0")
    fun getTotalRegistrosActivos(): Int

    @Query(
        """
    SELECT * FROM gimnasio
    WHERE valor > 0
    AND substr(fechaHora, 7, 4) = :yearStr
"""
    )
    fun getEntrenadosPorAno(yearStr: String): List<Gimnasio>

    @Query(
        """
    SELECT DISTINCT substr(fechaHora, 1, 2) AS dia
    FROM gimnasio
    WHERE valor > 0
      AND fechaHora LIKE '__/' || :mes || '/' || :anio || '%'
"""
    )
    fun getDiasConDatos(mes: String, anio: String): List<String>

    @Query(
        """
    SELECT fechaHora, valor
    FROM gimnasio
    WHERE valor > 0
      AND fechaHora LIKE '__/' || :mes || '/' || :anio || '%'
    ORDER BY 
      substr(fechaHora, 7, 4) || '-' ||  -- año
      substr(fechaHora, 4, 2) || '-' ||  -- mes
      substr(fechaHora, 1, 2)            -- día
    ASC
"""
    )
    fun getRegistrosMes(mes: String, anio: String): List<RegistroMes>

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
