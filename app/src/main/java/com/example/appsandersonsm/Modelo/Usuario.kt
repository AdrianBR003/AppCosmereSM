package com.example.appsandersonsm.Modelo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey val id: String = "", // ID del usuario (OID para Google, UUID para invitados)
    val nombre: String? = null,        // Nombre del usuario (si está disponible)
    val email: String? = null,         // Email del usuario (si está disponible)
    val esInvitado: Boolean = false     // Indica si es un usuario invitado
)