package com.letmcook.letmcook.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.FragmentSigninBinding
import com.letmcook.letmcook.models.UserModel
import com.letmcook.letmcook.services.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SignInFragment : Fragment() {

    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    private lateinit var authService: AuthService
    private lateinit var databaseService: DatabaseService
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseService = DatabaseService(requireContext())
        sessionManager = SessionManager(requireContext())
        
        if (sessionManager.isLoggedIn()) {
            findNavController().navigate(R.id.action_signInFragment_to_dashboardFragment)
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(AuthService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        authService = retrofit.create(AuthService::class.java)

        binding.btnSignIn.setOnClickListener {
            handleSignIn()
        }

        binding.tvSignUpLink.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }
    }

    private fun handleSignIn() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            return
        } else binding.tilEmail.error = null

        if (password.length < 8) {
            binding.tilPassword.error = "Password too short"
            return
        } else binding.tilPassword.error = null

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = authService.signIn(SignInRequest(email, password))
                if (response.isSuccessful) {
                    val authBody = response.body()
                    if (authBody != null && authBody.success == 1) {
                        // Save to local DB
                        val user = UserModel(
                            id = email, // Using email as ID for mock purposes if ID not returned
                            email = email,
                            fullName = authBody.fullName,
                            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        )
                        databaseService.upsertUser(user)

                        // Save Session
                        sessionManager.saveSession(authBody.accessToken ?: "", email)

                        findNavController().navigate(R.id.action_signInFragment_to_dashboardFragment)
                    } else {
                        showError(authBody?.message ?: "Sign in failed")
                    }
                } else {
                    showError("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSignIn.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
