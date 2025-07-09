package com.example.drivesafe.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.drivesafe.databinding.FragmentProfileViewBinding
import com.example.drivesafe.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.android.flexbox.FlexboxLayout
import android.widget.TextView
import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.example.drivesafe.R
import android.content.res.ColorStateList

/**
 * A [Fragment] for viewing the user's profile information.
 * It displays personal details and registered driving licenses.
 * Provides a button to navigate to the [EditProfileFragment] for making changes.
 * Data is fetched and observed from the [ProfileViewModel].
 */
class ProfileFragment : Fragment() {

    /**
     * View binding instance for `fragment_profile_view.xml`.
     * This property is nullable as the binding object is only valid
     * between `onCreateView` and `onDestroyView`.
     */
    private var _binding: FragmentProfileViewBinding? = null

    /**
     * Provides a non-null reference to the view binding.
     * This accessor should only be used when the fragment's view is available
     * (i.e., between `onCreateView` and `onDestroyView`).
     */
    private val binding get() = _binding!!

    /**
     * Reference to the [ProfileViewModel] associated with this fragment.
     * This ViewModel handles fetching user profile data from Firestore.
     */
    private val viewModel: ProfileViewModel by viewModels()

    /**
     * Firebase Authentication instance to get the current user's email.
     */
    private val auth = FirebaseAuth.getInstance()

    /**
     * Map associating the main license category name (e.g., "A", "B") with the drawable resource ID
     * for its corresponding icon. Used to display category icons next to subcategories.
     */
    private val licenseCategoryIconMap = mapOf(
        "A" to R.drawable.ic_license_category_a,
        "B" to R.drawable.ic_license_category_b,
        "C" to R.drawable.ic_license_category_c,
        "D" to R.drawable.ic_license_category_d
    )

    /**
     * Map associating each license subcategory (e.g., "AM", "B1") with its main category (e.g., "A", "B").
     * Used to determine which main category icon to display for a given subcategory.
     */
    private val subcategoryToMainCategoryMap = mapOf(
        "AM" to "A",
        "A1" to "A",
        "A2" to "A",
        "A" to "A",
        "B1" to "B",
        "B" to "B",
        "BE" to "B",
        "C1" to "C",
        "C" to "C",
        "C1E" to "C",
        "CE" to "C",
        "D1" to "D",
        "D" to "D",
        "D1E" to "D",
        "DE" to "D"
    )

    /**
     * Called to create and return the view hierarchy associated with the fragment.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previously saved state.
     * @return The root [View] for the fragment's UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileViewBinding.inflate(inflater, container, false)
        val root: View = binding.root
        setupObservers()
        setupEditButton()

        return root
    }

    /**
     * Called immediately after `onCreateView` has returned, but before any saved state has been restored
     * into the view. This is a good place to perform final initialization of the view before it is
     * displayed to the user.
     *
     * @param view The View returned by `onCreateView`.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previously saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.emailTextView.text = auth.currentUser?.email ?: "N/A"
    }

    /**
     * Sets up observers for the [ProfileViewModel]'s LiveData.
     * Updates the UI based on user profile data and loading state.
     */
    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner, Observer { user ->
            user?.let {
                binding.usernameTextView.text = it.username.ifEmpty { "Not set" }
                binding.favCarBrandTextView.text = it.favoriteCarBrand.ifEmpty { "Not set" }
                binding.placeOfBirthTextView.text = it.placeOfBirth.ifEmpty { "Not set" }
                binding.streetAddressTextView.text = it.streetAddress.ifEmpty { "Not set" }
                binding.cityOfResidenceTextView.text = it.cityOfResidence.ifEmpty { "Not set" }
                binding.yearOfBirthTextView.text = it.yearOfBirth?.toString() ?: "Not set"

                displayLicenseCategories(it.licenses)
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.editProfileButton.isEnabled = !isLoading
        })
    }

    /**
     * Sets up the click listener for the edit profile button.
     * Navigates to the [EditProfileFragment] when clicked.
     */
    private fun setupEditButton() {
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_nav_profile_to_editProfileFragment)
        }
    }

    /**
     * Dynamically displays the user's owned license subcategories within the `licensesDisplayFlexboxLayout`.
     * Each license subcategory is represented as a [Chip], and an icon for its main category is added.
     *
     * @param licenses The [List] of strings representing the license subcategories owned by the user.
     */
    private fun displayLicenseCategories(licenses: List<String>) {
        binding.licensesDisplayFlexboxLayout.removeAllViews()

        if (licenses.isEmpty()) {
            val textView = TextView(requireContext()).apply {
                layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = "No licenses registered"
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        android.R.color.darker_gray
                    )
                )
            }
            binding.licensesDisplayFlexboxLayout.addView(textView)
        } else {
            val context = requireContext()
            licenses.forEach { license ->
                val chip = Chip(context).apply {
                    layoutParams = FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(
                            context.resources.getDimensionPixelSize(R.dimen.license_display_margin),
                            context.resources.getDimensionPixelSize(R.dimen.license_display_margin),
                            context.resources.getDimensionPixelSize(R.dimen.license_display_margin),
                            context.resources.getDimensionPixelSize(R.dimen.license_display_margin)
                        )
                    }
                    text = license

                    val mainCategory = subcategoryToMainCategoryMap[license]

                    if (mainCategory != null) {
                        licenseCategoryIconMap[mainCategory]?.let { iconResId ->
                            setChipIconResource(iconResId)
                            chipIconTint = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    android.R.color.black
                                )
                            )
                        }
                    }

                    // Configure the Chip for display-only purposes:
                    isCloseIconVisible = false
                    isClickable = false
                    isCheckable = false
                }
                binding.licensesDisplayFlexboxLayout.addView(chip)
            }
        }
    }

    /**
     * Called when the view hierarchy associated with the fragment is being removed.
     * Cleans up resources by nullifying the view binding.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}