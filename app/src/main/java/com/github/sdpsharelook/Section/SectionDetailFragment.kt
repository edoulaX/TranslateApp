package com.github.sdpsharelook.Section

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.github.sdpsharelook.databinding.FragmentSectionDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SectionDetailFragment : Fragment() {

    private lateinit var binding: FragmentSectionDetailBinding

    private val wordList = mutableListOf<SectionWord>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args: SectionDetailFragmentArgs by navArgs()
        val section = sectionFromId(args.sectionID)
        val sectionWord = args.sectionWord

//        CoroutineScope(Dispatchers.IO).launch {
//            section!!.databaseRepo.flow().collect {
//                when {
//                    it.isSuccess -> {
//                        val message = it.getOrNull().toString()
//                        withContext(Dispatchers.Main) {
//                            findViewById<TextView>(R.id.database_contents).apply {
//                                text = message
//                            }
//                        }
//                    }
//                    it.isFailure -> {
//                        it.exceptionOrNull()?.printStackTrace()
//                    }
//                }
//            }
//        }


        // If we are adding a word from the translator Activity
        if (sectionWord != null) {
            CoroutineScope(Dispatchers.IO).launch {
                section?.databaseRepo?.insert(section.sectionRepo, sectionWord.toList())
            }
            addSectionWord(sectionWord)
        }

        if (section != null) {
            binding.sectionTitle.text = section.title
            binding.sectionFlag.setImageResource(section.flag)
        }

        binding.wordList.adapter = SectionWordAdapter(requireContext(), wordList)
    }

    fun addSectionWord(sw: SectionWord) {
        wordList.add(sw)
    }

    private fun sectionFromId(sectionID: Int): Section? {
        for (section in sectionList) {
            if (sectionID == section.id) {
                return section
            }
        }
        return null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSectionDetailBinding.inflate(layoutInflater)
        return binding.root
    }
}