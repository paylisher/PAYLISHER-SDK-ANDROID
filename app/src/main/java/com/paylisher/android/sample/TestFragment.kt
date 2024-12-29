package com.paylisher.android.sample

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class TestFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_test, container, false)

        // Retrieve saved preferences
        val sharedPreferences = requireActivity().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val savedName = sharedPreferences.getString("name", "")
        val savedEmail = sharedPreferences.getString("email", "")
        val savedAlias = sharedPreferences.getString("alias", "")
        val savedGender = sharedPreferences.getString("gender", "")

        // Find views
        val nameEditText = view.findViewById<EditText>(R.id.input_name)
        val emailEditText = view.findViewById<EditText>(R.id.input_email)
        val aliasEditText = view.findViewById<EditText>(R.id.input_alias)
        val genderRadioGroup = view.findViewById<RadioGroup>(R.id.radio_group_gender)

        // Set saved values
        nameEditText.setText(savedName)
        emailEditText.setText(savedEmail)
        aliasEditText.setText(savedAlias)

        when (savedGender) {
            "Male" -> genderRadioGroup.check(R.id.radio_male)
            "Female" -> genderRadioGroup.check(R.id.radio_female)
            "Other" -> genderRadioGroup.check(R.id.radio_other)
        }

        // Set up the Save button
        val saveButton = view.findViewById<Button>(R.id.button_save)
        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val alias = aliasEditText.text.toString()
            val selectedGenderId = genderRadioGroup.checkedRadioButtonId
            val selectedGender = view.findViewById<RadioButton>(selectedGenderId)?.text.toString()

            // Save to SharedPreferences
            with(sharedPreferences.edit()) {
                putString("name", name)
                putString("email", email)
                putString("alias", alias)
                putString("gender", selectedGender)
                commit() // Synchronous save
            }

            val allPreferences = sharedPreferences.all
            Log.d("SharedPreferences", "Current Preferences: $allPreferences")

            Toast.makeText(requireContext(), "Details saved successfully", Toast.LENGTH_SHORT).show()
        }

        // Set up the "Go Back" button
        val goBackButton = view.findViewById<Button>(R.id.button_back)
        goBackButton.setOnClickListener {
            findNavController().navigate(R.id.action_TestFragment_to_FirstFragment)
        }

        return view
    }
}
