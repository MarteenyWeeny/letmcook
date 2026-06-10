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
import com.letmcook.letmcook.databinding.FragmentSignupBinding
import com.letmcook.letmcook.services.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private lateinit var authService: AuthService
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val retrofit = Retrofit.Builder()
            .baseUrl(AuthService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        authService = retrofit.create(AuthService::class.java)

        binding.btnSignUp.setOnClickListener {
            handleSignUp()
        }

        binding.tvSignInLink.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }
    }

    private fun handleSignUp() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Name required"
            return
        } else binding.tilFullName.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email"
            return
        } else binding.tilEmail.error = null

        if (password.length < 8) {
            binding.tilPassword.error = "Password must be at least 8 characters"
            return
        } else binding.tilPassword.error = null

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = authService.signUp(SignUpRequest(fullName, email, password))
                if (response.isSuccessful) {
                    val authBody = response.body()
                    if (authBody != null && authBody.success == 1) {
                        // Save minimal session info for goal setting
                        sessionManager.saveSession(authBody.accessToken ?: "", email)
                        findNavController().navigate(R.id.action_signUpFragment_to_goalSettingFragment)
                    } else {
                        showError(authBody?.message ?: "Sign up failed")
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
        binding.btnSignUp.isEnabled = !isLoading
        binding.etFullName.isEnabled = !isLoading
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
