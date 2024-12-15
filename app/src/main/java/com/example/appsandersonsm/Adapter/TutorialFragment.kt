package com.example.appsandersonsm.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.appsandersonsm.R

class TutorialFragment : Fragment() {

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_IMAGE = "image"
        private const val ARG_DESCRIPTION = "description"

        fun newInstance(title: String, imageRes: Int, description: String): TutorialFragment {
            val fragment = TutorialFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putInt(ARG_IMAGE, imageRes)
            args.putString(ARG_DESCRIPTION, description)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_tutorial, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titleTextView: TextView = view.findViewById(R.id.tituloFragment)
        val imageView: ImageView = view.findViewById(R.id.imagenFragment)
        val descriptionTextView: TextView = view.findViewById(R.id.descripcionFragment)

        arguments?.let {
            val title = it.getString(ARG_TITLE)
            val imageRes = it.getInt(ARG_IMAGE)
            val description = it.getString(ARG_DESCRIPTION)

            titleTextView.text = title
            imageView.setImageResource(imageRes)
            descriptionTextView.text = description
        }
    }
}
