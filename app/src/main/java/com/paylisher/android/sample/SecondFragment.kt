package com.paylisher.android.sample

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.paylisher.Paylisher
import com.paylisher.android.sample.databinding.ForTestBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    // FragmentSecondBinding
    private var _binding: ForTestBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = ForTestBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

//        Paylisher.screen(
//            screenTitle = "Second", properties = mapOf(
//                "background" to "red",
//                "hero" to "holyCat"
//            )
//        )

        binding.btnCapture.setOnClickListener {
            Paylisher.capture(
                event = "btnCapture_clicked",
                properties = mapOf("color" to "blue"),
                userProperties = mapOf(
                    "string" to "value1",
                    "integer" to 2
                )
            )
        }

        binding.btnError.setOnClickListener {
            throw Exception("This is a test error event!")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}