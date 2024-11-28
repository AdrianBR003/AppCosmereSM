package com.example.appsandersonsm

import androidx.recyclerview.widget.DiffUtil
import com.example.appsandersonsm.Modelo.Nota

class NotaDiffCallback(
    private val oldList: List<Nota>,
    private val newList: List<Nota>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
