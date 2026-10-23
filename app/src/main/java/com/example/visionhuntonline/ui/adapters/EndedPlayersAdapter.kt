package com.example.visionhuntonline.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.visionhuntonline.databinding.ItemEndedPlayerBinding
import com.example.visionhuntonline.network.EndedPlayer

class EndedPlayersAdapter : RecyclerView.Adapter<EndedPlayersAdapter.ViewHolder>() {

    private var playersList: List<EndedPlayer> = emptyList()

    fun submitData(players: List<EndedPlayer>) {
        this.playersList = players
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemEndedPlayerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEndedPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val player = playersList[position]
        with(holder.binding) {
            tvEndedRank.text = "#${player.rankPosition}"
            tvEndedUsername.text = player.username
            tvEndedScore.text = "${player.score} نقطة"
        }
    }

    override fun getItemCount(): Int = playersList.size
}
