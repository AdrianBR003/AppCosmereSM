package com.example.appsandersonsm.Adapter


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.appsandersonsm.Modelo.TutorialPage

class TutorialAdapter(
    fragmentActivity: FragmentActivity,
    private val tutorialPages: List<TutorialPage>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = tutorialPages.size

    override fun createFragment(position: Int): Fragment {
        val page = tutorialPages[position]
        return TutorialFragment.newInstance(page.title, page.imageRes, page.description)
    }
}

