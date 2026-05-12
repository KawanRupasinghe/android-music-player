package com.example.take_homeactivity

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class MusicAdapter(
    context: Context,
    private val songs: List<Song>
) : ArrayAdapter<Song>(context, 0, songs) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val song = songs[position]
        view.findViewById<TextView>(android.R.id.text1).text = song.title
        view.findViewById<TextView>(android.R.id.text2).text = song.artist

        return view
    }
}