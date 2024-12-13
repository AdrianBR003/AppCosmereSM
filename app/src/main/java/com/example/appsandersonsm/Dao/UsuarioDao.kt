package com.example.appsandersonsm.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.appsandersonsm.Modelo.Usuario

@Dao
interface UsuarioDao {

    @Query("DELETE FROM usuarios WHERE id = :userId")
    suspend fun borrarUsuario(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun obtenerUsuarioPorId(id: String): Usuario?

    @Query("UPDATE usuarios SET id = :newUserId WHERE id = 'id_default'")
    suspend fun actualizarUsuarioIdDefault(newUserId: String)


    @Query("DELETE FROM usuarios WHERE id = :id")
    suspend fun eliminarUsuario(id: String)

    @Query("SELECT * FROM usuarios")
    suspend fun obtenerTodosLosUsuarios(): List<Usuario>

    @Query("DELETE FROM usuarios")
    suspend fun deleteAllUsuarios()

}
