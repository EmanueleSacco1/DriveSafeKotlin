package com.example.drivesafe.ui.logout

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import com.example.drivesafe.ui.auth.LoginActivity
import com.example.drivesafe.databinding.FragmentLogoutBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.drivesafe.R

/**
 * Fragment responsible for handling the user logout operation.
 * It displays a confirmation dialog and, if confirmed, performs the logout
 * and navigates the user back to the Login screen.
 */
class LogoutFragment : Fragment() {

    /**
     * View binding for the logout fragment layout.
     * This property is nullable because the binding exists only when the fragment's view is created.
     * It is nullified in [onDestroyView] to prevent memory leaks.
     */
    private var _binding: FragmentLogoutBinding? = null

    /**
     * Provides non-null access to the view binding.
     * It is safe to use only between [onCreateView] and [onDestroyView].
     */
    private val binding get() = _binding!!

    /**
     * Firebase Authentication instance for managing user authentication state.
     */
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Called to create and return the view hierarchy associated with the fragment.
     * @param inflater Used to inflate XML layouts.
     * @param container The parent ViewGroup in which the fragment's view will be attached.
     * @param savedInstanceState The previously saved state.
     * @return The root view of the fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called immediately after [onCreateView] has returned, when the view hierarchy has been created.
     * @param view The root view returned by [onCreateView].
     * @param savedInstanceState The previously saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_dialog_title)
                .setMessage(R.string.logout_dialog_message)
                .setPositiveButton(R.string.logout_dialog_positive) { _, _ ->
                    auth.signOut()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton(R.string.logout_dialog_negative, null)
                .show()
        }
    }

    /**
     * Called when the view associated with the fragment is being destroyed.
     * Cleans up the binding reference to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
