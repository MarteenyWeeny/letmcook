package com.letmcook.letmcook.ui.dashboard

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.DialogProfileBinding
import com.letmcook.letmcook.databinding.FragmentDashboardBinding
import com.letmcook.letmcook.services.DatabaseService
import com.letmcook.letmcook.services.SeedService
import com.letmcook.letmcook.services.SessionManager
import com.letmcook.letmcook.ui.recipe.RecipeAdapter
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseService: DatabaseService
    private lateinit var sessionManager: SessionManager
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var dateAdapter: DateAdapter
    
    private var selectedDate: Date = Date()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseService = DatabaseService(requireContext())
        sessionManager = SessionManager(requireContext())

        setupProfileCard()
        setupSuggestions()
        setupWorkouts()
        setupDateSelector()
        setupHeaderDate()
        setupWaterCard()
        
        binding.btnBack.setOnClickListener {
            // Usually dashboard is top level, but if they want to go back to sign in/up
            findNavController().popBackStack()
        }
        
        // Temporary profile pic
        binding.profileCard.setImageResource(android.R.drawable.ic_menu_myplaces)
    }

    private fun setupWaterCard() {
        binding.cardWater.setOnClickListener {
            val userId = sessionManager.getUserId() ?: "default_user"
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
            
            val options = arrayOf("250ml", "500ml", "750ml", "Custom")
            AlertDialog.Builder(requireContext())
                .setTitle("Add Water Intake")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> addWater(userId, dateString, 250)
                        1 -> addWater(userId, dateString, 500)
                        2 -> addWater(userId, dateString, 750)
                        3 -> showCustomWaterDialog(userId, dateString)
                    }
                }
                .show()
        }
    }

    private fun addWater(userId: String, date: String, amount: Int) {
        databaseService.addWaterIntake(userId, date, amount)
        loadAllData()
    }

    private fun showCustomWaterDialog(userId: String, date: String) {
        val input = EditText(requireContext())
        input.hint = "Amount in ml"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(60, 20, 60, 0)
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Custom Water Amount")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val amount = input.text.toString().toIntOrNull() ?: 0
                if (amount > 0) addWater(userId, date, amount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadAllData()
    }

    private fun setupDateSelector() {
        dateAdapter = DateAdapter(emptyList(), selectedDate) { newDate ->
            selectedDate = newDate
            loadAllData()
        }
        
        binding.rvDateSelector.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 7)
        binding.rvDateSelector.adapter = dateAdapter
        
        refreshDateStrip()
    }

    private fun setupHeaderDate() {
        binding.llDateHeader.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val newCalendar = Calendar.getInstance()
                    newCalendar.set(year, month, dayOfMonth)
                    selectedDate = newCalendar.time
                    
                    refreshDateStrip()
                    loadAllData()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun refreshDateStrip() {
        val dates = mutableListOf<Date>()
        val calendar = Calendar.getInstance()
        
        // Center the 7-day window on today
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        
        repeat(7) {
            dates.add(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        dateAdapter.updateDates(dates)
        dateAdapter.updateSelectedDate(selectedDate)
    }

    private fun setupSuggestions() {
        recipeAdapter = RecipeAdapter(emptyList()) { recipe ->
            val bundle = Bundle().apply {
                putString("recipeId", recipe.id)
            }
            findNavController().navigate(R.id.action_dashboardFragment_to_recipeDetailFragment, bundle)
        }
        binding.rvSuggestions.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSuggestions.isNestedScrollingEnabled = false
        binding.rvSuggestions.adapter = recipeAdapter
    }

    private fun setupWorkouts() {
        workoutAdapter = WorkoutAdapter(emptyList()) { workout ->
            val userId = sessionManager.getUserId() ?: "default_user"
            if (workout.ownerId == "system") {
                workout.ownerId = userId
            }
            databaseService.upsertWorkout(workout)
            loadAllData()
        }
        binding.rvWorkouts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorkouts.isNestedScrollingEnabled = false
        binding.rvWorkouts.adapter = workoutAdapter
    }

    private fun loadAllData() {
        val userId = sessionManager.getUserId() ?: "default_user"
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
        val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val sdf = SimpleDateFormat("MMMM d", Locale.getDefault())
        binding.tvDateRange.text = sdf.format(selectedDate)

        val goal = databaseService.getNutritionGoal(userId)
        val calorieTarget = goal?.dailyCalorieTarget ?: 2000
        val proteinTarget = goal?.proteinTargetGrams ?: 120
        val carbTarget = goal?.carbTargetGrams ?: 200
        val fatTarget = goal?.fatTargetGrams ?: 60

        val intakeList = databaseService.getIntakeForDate(userId, dateString)
        val totalCal = intakeList.sumOf { it.calories }
        val totalProtein = intakeList.sumOf { it.protein }
        val totalCarbs = intakeList.sumOf { it.carbs }
        val totalFat = intakeList.sumOf { it.fat }

        val userWorkouts = databaseService.getWorkoutsForDate(userId, dateString)
        var systemWorkouts = databaseService.getWorkoutsForDate("system", dateString)
        
        // Fallback for system workouts if date mismatch
        if (systemWorkouts.isEmpty()) {
            systemWorkouts = databaseService.getWorkoutsForDate("system", todayString)
        }
        
        val displayWorkouts = (userWorkouts + systemWorkouts).distinctBy { it.title }
        workoutAdapter.updateData(displayWorkouts)
        
        val burnedCal = displayWorkouts.filter { it.isCompleted }.sumOf { it.caloriesBurned }

        val netCal = totalCal - burnedCal
        val remainingCal = calorieTarget - netCal
        
        val displayCal = netCal.coerceAtLeast(0.0).toInt()
        val calPercent = if (calorieTarget > 0) (displayCal * 100 / calorieTarget) else 0
        binding.tvCaloriesPercent.text = getString(R.string.percent_format, calPercent)
        binding.pbCalories.max = calorieTarget
        binding.pbCalories.progress = displayCal
        binding.tvCaloriesProgress.text = getString(R.string.calories_format, displayCal, calorieTarget)

        val proteinPercent = if (proteinTarget > 0) (totalProtein * 100 / proteinTarget).toInt() else 0
        binding.tvProteinPercent.text = getString(R.string.percent_format, proteinPercent)
        binding.pbProtein.max = proteinTarget
        binding.pbProtein.progress = totalProtein.toInt()
        binding.tvProteinProgress.text = getString(R.string.macro_format_g, totalProtein.toInt(), proteinTarget)

        val carbPercent = if (carbTarget > 0) (totalCarbs * 100 / carbTarget).toInt() else 0
        binding.tvCarbsPercent.text = getString(R.string.percent_format, carbPercent)
        binding.pbCarbs.max = carbTarget
        binding.pbCarbs.progress = totalCarbs.toInt()
        binding.tvCarbsProgress.text = getString(R.string.macro_format_g, totalCarbs.toInt(), carbTarget)

        val fatPercent = if (fatTarget > 0) (totalFat * 100 / fatTarget).toInt() else 0
        binding.tvFatPercent.text = getString(R.string.percent_format, fatPercent)
        binding.pbFat.max = fatTarget
        binding.pbFat.progress = totalFat.toInt()
        binding.tvFatProgress.text = getString(R.string.macro_format_g, totalFat.toInt(), fatTarget)

        val waterIntake = databaseService.getWaterIntakeForDate(userId, dateString)
        val waterTarget = 2660
        binding.tvWaterProgress.text = getString(R.string.water_format, waterIntake, waterTarget)

        var allRecipes = databaseService.getAllRecipes()
        
        // Final fallback: if DB is still empty (seeding not finished), force it now
        if (allRecipes.isEmpty()) {
            SeedService(databaseService).seedData()
            allRecipes = databaseService.getAllRecipes()
        }

        val pantryItems = databaseService.getPantryItems(userId).associateBy { it.ingredientId }
        val suggestions = allRecipes.filter { 
            it.totalCalories <= (remainingCal + 600)
        }.shuffled().take(5).map { recipe ->
            val ingredients = databaseService.getRecipeIngredients(recipe.id)
            val matchCount = ingredients.count { ri ->
                val pi = pantryItems[ri.ingredientId]
                pi != null && pi.currentQuantity >= ri.requiredQuantity
            }
            val score = if (ingredients.isEmpty()) 0.0 else matchCount.toDouble() / ingredients.size
            recipe to score
        }
        
        recipeAdapter.updateData(suggestions)
        dateAdapter.updateSelectedDate(selectedDate)
    }

    private fun setupProfileCard() {
        binding.profileCard.setOnClickListener {
            showProfileDialog()
        }
    }

    private fun showProfileDialog() {
        val dialogBinding = DialogProfileBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val userId = sessionManager.getUserId() ?: ""
        val user = databaseService.getUser(userId)
        val settings = databaseService.getAppSettings()

        dialogBinding.tvFullName.text = user?.fullName ?: "Guest"
        dialogBinding.tvEmail.text = user?.email ?: ""
        dialogBinding.switchLargeText.isChecked = settings.useLargeText
        dialogBinding.switchKeepScreenOn.isChecked = settings.keepScreenOn

        dialogBinding.switchLargeText.setOnCheckedChangeListener { _, isChecked ->
            val newSettings = settings.copy(useLargeText = isChecked)
            databaseService.updateAppSettings(newSettings)
            requireActivity().recreate()
            dialog.dismiss()
        }

        dialogBinding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            val newSettings = settings.copy(keepScreenOn = isChecked)
            databaseService.updateAppSettings(newSettings)
            requireActivity().recreate()
            dialog.dismiss()
        }

        dialogBinding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            findNavController().navigate(R.id.signInFragment)
            dialog.dismiss()
        }
        
        dialogBinding.btnEditGoals.setOnClickListener {
            findNavController().navigate(R.id.goalSettingFragment)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
