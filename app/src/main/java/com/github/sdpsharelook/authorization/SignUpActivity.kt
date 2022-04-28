package com.github.sdpsharelook.authorization

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.sdpsharelook.GreetingActivity
import com.github.sdpsharelook.R

import com.github.sdpsharelook.databinding.ActivitySignUpBinding
import kotlinx.coroutines.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firstNameListener()
        lastNameListener()
        emailListener()
        prelimPasswordListener()
        passwordListener()
        auth = FireAuth()
    }

    private fun firstNameListener() {
        binding.firstName.setOnFocusChangeListener{ _, focus ->
            if (!focus) {
                binding.firstName.error = isFirstNameValid()
            }
        }
    }

    private fun isFirstNameValid() : String? {
        if(binding.firstName.text.toString() == "") {
            return "Required"
        } else {
            return null
        }
    }

    private fun lastNameListener() {
        binding.lastName.setOnFocusChangeListener{ _, focus ->
            if (!focus) {
                binding.lastName.error = isLastNameValid()
            }
        }
    }

    private fun isLastNameValid() : String? {
        if(binding.lastName.text.toString() != "") {
            return null
        } else {
            return "Required"
        }
    }

    private fun emailListener() {
        binding.email.setOnFocusChangeListener{_, focus ->
            if(!focus) {
                binding.email.error = isEmailValid()
            }
        }

    }

    private fun isEmailValid() : String? {
        val regex = "(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))".toRegex()
        if (binding.email.text.toString() == "") {
            return "Required"
        } else
            if(!binding.email.text.toString().matches(regex)) {
                return "Invalid Email Address"
            } else {
                return null
            }
    }

    private fun prelimPasswordListener() {
        binding.prelimpassword.setOnFocusChangeListener{ _, focus ->
            if (!focus) {
                binding.prelimpassword.error = isPrelimPassword()
                binding.prelimPasswordBox.helperText = isPrelimPassword()
            }
        }
    }

    private fun isPrelimPassword() : String? {
        val upperCase = "(.*?[A-Z].*)".toRegex()
        val lowerCase = "(.*?[a-z].*)".toRegex()
        val oneDigit = "(.*?[0-9].*)".toRegex()
        val specialChar = "(.*?[#?!()@\$ %^&*-].*)".toRegex()
        val minLength8 = ".{8,}".toRegex()
        val error: String?
        if(binding.prelimpassword.text.toString() == "") {
            error = "Required"
        } else
            if(!binding.prelimpassword.text.toString().matches(upperCase)) {
                error = "Must contain an uppercase letter"
            } else
                if(!binding.prelimpassword.text.toString().matches(lowerCase)) {
                    error = "Must contain a lowercase letter"
                } else
                    if(!binding.prelimpassword.text.toString().matches(oneDigit)) {
                        error = "Must contain at least one digit"
                    } else
                        if(!binding.prelimpassword.text.toString().matches(specialChar)) {
                            error = "Must contain at least one special character"
                        } else
                            if(!binding.prelimpassword.text.toString().matches(minLength8)) {
                                error = "Must contain at least 8 characters"
                            }
                            else {
                                error = null
                            }
        return error
    }

    private fun passwordListener() {
        binding.password.setOnFocusChangeListener{_, focus ->
            if(!focus) {
                binding.passwordBox.error = isPasswordValid()
                binding.passwordBox.helperText = isPasswordValid()
            }
        }
    }

    private fun isPasswordValid() : String? {
        when {
            binding.password.text.toString() != binding.prelimpassword.text.toString() -> {
                return "Passwords do not match"
            }
            else -> {
                return null
            }
        }
    }

    fun checkBeforeSignUp(view: View) {
        updateErrors()
        val firstNameValid = binding.firstName.error == null
        val lastNameValid = binding.lastName.error == null
        val emailValid = binding.email.error == null
        val prelimPassValid = binding.prelimPasswordBox.helperText == null
        val passValid = binding.password.error == null && binding.passwordBox.helperText ==  null
        if (firstNameValid && lastNameValid && emailValid && prelimPassValid && passValid) {
            signUp(view)
        } else {
            Toast.makeText(this, "Form is filled incorrectly", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateErrors() {
        binding.firstName.error = isFirstNameValid()
        binding.lastName.error = isLastNameValid()
        binding.email.error = isEmailValid()
        binding.prelimpassword.error = isPrelimPassword()
        binding.prelimPasswordBox.helperText = isPrelimPassword()
        binding.passwordBox.error = isPasswordValid()
        binding.passwordBox.helperText = isPasswordValid()
    }

    fun signUp(@Suppress("UNUSED_PARAMETER")view: View) {
        val email = findViewById<EditText>(R.id.email).text.toString()
        val password = findViewById<EditText>(R.id.password).text.toString()

        CoroutineScope(Dispatchers.IO).launch {
            val user = auth.createUserWithEmailAndPassword(email, password)
            when {
                user.isSuccess -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Signed Up !", Toast.LENGTH_LONG).show()
                    }
                    greet(user.getOrNull()!!.displayName)
                }
                user.isFailure -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            user.exceptionOrNull()!!.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun greet(name: String?) {
        val tName =
            if (name.isNullOrBlank() || auth.currentUser!!.isAnonymous) "anonymous" else name
        val intent = Intent(this, GreetingActivity::class.java).apply {
            putExtra(GREET_NAME_EXTRA, tName)
        }
        startActivity(intent)
    }
}