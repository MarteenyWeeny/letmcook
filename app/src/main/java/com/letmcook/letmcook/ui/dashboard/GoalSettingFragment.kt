package com.letmcook.letmcook.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.FragmentGoalSettingBinding
import com.letmcook.letmcook.models.NutritionGoalModel
import com.letmcook.letmcook.services.DatabaseService
import com.letmcook.letmcook.services.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class GoalSettingFragment : Fragment() {

    private var _binding: FragmentGoalSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseService: DatabaseService
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGoalSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseService = DatabaseService(requireContext())
        sessionManager = SessionManager(requireContext())

        setupActivitySpinner()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSaveGoals.setOnClickListener {
            saveGoals()
        }
    }

    private fun setupActivitySpinner() {
        val levels = arrayOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerActivity.adapter = adapter
    }

    private fun saveGoals() {
        val age = binding.etAge.text.toString().toIntOrNull() ?: return
        val weight = binding.etWeight.text.toString().toDoubleOrNull() ?: return
        val height = binding.etHeight.text.toString().toDoubleOrNull() ?: return
        val isMale = binding.rbMale.isChecked
        val activityLevel = binding.spinnerActivity.selectedItemPosition
        val goalType = when (binding.rgGoal.checkedRadioButtonId) {
            R.id.rbLose -> -500
            R.id.rbGain -> 500
            else -> 0
        }

        // Mifflin-St Jeor
        val bmr = if (isMale) {
            10 * weight + 6.25 * height - 5 * age + 5
        } else {
            10 * weight + 6.25 * height - 5 * age - 161
        }

        val activityMultiplier = when (activityLevel) {
            0 -> 1.2    // Sedentary
            1 -> 1.375  // Lightly
            2 -> 1.55   // Moderately
            3 -> 1.725  // Very
            4 -> 1.9    // Extra
            else -> 1.2
        }

        val tdee = bmr * activityMultiplier
        val calorieTarget = (tdee + goalType).toInt()

        // Macros: 30% Protein, 40% Carbs, 30% Fat (Simplified)
        val proteinGrams = (calorieTarget * 0.30 / 4).toInt()
        val carbGrams = (calorieTarget * 0.40 / 4).toInt()
        val fatGrams = (calorieTarget * 0.30 / 9).toInt()

        val userId = sessionManager.getUserId() ?: "default_user"
        val goal = NutritionGoalModel(
            id = UUID.randomUUID().toString(),
            ownerId = userId,
            dailyCalorieTarget = calorieTarget,
            proteinTargetGrams = proteinGrams,
            carbTargetGrams = carbGrams,
            fatTargetGrams = fatGrams,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )

        databaseService.upsertNutritionGoal(goal)
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
