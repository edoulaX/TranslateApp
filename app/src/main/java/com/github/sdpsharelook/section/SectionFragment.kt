package com.github.sdpsharelook.section

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.sdpsharelook.R
import com.github.sdpsharelook.Word
import com.github.sdpsharelook.databinding.CardSectionBinding
import com.github.sdpsharelook.databinding.FragmentSectionBinding
import com.github.sdpsharelook.databinding.PopupBinding
import com.github.sdpsharelook.storage.RTDBWordListRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

var edit = false

@AndroidEntryPoint
class SectionFragment : Fragment(), SectionClickListener {

    /**
     * This property is only valid between onCreateView and onDestroyView.
     */
    private val binding get() = _binding!!
    private var _binding: FragmentSectionBinding? = null
    private lateinit var popupBinding: PopupBinding
    private lateinit var cardBinding: CardSectionBinding

    @Inject
    lateinit var databaseWordList: RTDBWordListRepository

    private lateinit var dialog: Dialog
    var mainCountryList = initList()

    private var sectionWord: Word? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args: SectionFragmentArgs by navArgs()
        sectionWord = args.sectionWord
        popupBinding = PopupBinding.inflate(layoutInflater)
        cardBinding = CardSectionBinding.inflate(layoutInflater)

        //init list of possible languages for the spinner
        initList()

        // set up the popup when cliking on add button
        dialog = Dialog(requireContext())
        dialog.setContentView(popupBinding.root)
        dialog.setOnDismissListener { popupBinding.editSectionName.text.clear() }

        // set up the spinner
        popupBinding.spinnerCountries.adapter = CountryAdapter(requireContext(), mainCountryList)

        // set up the recyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val cardAdapter = CardAdapter(sectionList, this, dialog)
        binding.recyclerView.adapter = cardAdapter

        //TODO
//        /**Load all the Sections from the database**/
//        lifecycleScope.launch {
//            collectSectionFlow()
//        }

        binding.addingBtn.setOnClickListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            dialog.show()
        }

        popupBinding.popupAddBtn.setOnClickListener {
            val sectionName = popupBinding.editSectionName.text.toString()
            val countryIndex = popupBinding.spinnerCountries.selectedItemPosition
            val newSection = Section(
                sectionName,
                mainCountryList[countryIndex].flag,
                databaseWordList,
                sectionName + countryIndex
            )

            // Popu do 2 different things if it is editing a section or creating one
            if (edit) {
                cardAdapter.editItem(sectionName, mainCountryList.get(countryIndex).flag)
            } else if(addSection(newSection)){
                // TODO
                //lifecycleScope.launch {
                //    databaseWordList.insertSection(newSection)
                //}
                Toast.makeText(requireContext(), "Section: $sectionName saved", Toast.LENGTH_SHORT).show()
            }else{
                // if the section already exist
                Toast.makeText(requireContext(), "$sectionName already exist", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
    }

    private suspend fun collectSectionFlow() {
        Log.e("","Collecting section flow")
        databaseWordList.flow().collect{
            when {
                it.isSuccess -> {
                    val section = it.getOrNull() as Section
                    addSection(section)
                }
                it.isFailure -> {
                    it.exceptionOrNull()?.printStackTrace()
                }
            }
            binding.recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun initList(): List<CountryItem> {
        val list = mutableListOf<CountryItem>()
        list.add(CountryItem(R.drawable.spain))
        list.add(CountryItem(R.drawable.us))
        return list
    }

    private fun addSection(section: Section): Boolean {
        // if the section already exist do not add it
        return if(sectionList.contains(section)){
            false
        }else{
            sectionList.add(section)
            binding.recyclerView.adapter?.notifyItemInserted(sectionList.lastIndex)
            true
        }
    }

    override fun onClick(section: Section) {
        val action = SectionFragmentDirections.actionMenuSectionsLinkToSectionDetailFragment(
            section.sectionSize!!, sectionWord
        )
        findNavController().navigate(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSectionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}