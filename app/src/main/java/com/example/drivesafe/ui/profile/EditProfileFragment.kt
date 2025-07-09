package com.example.drivesafe.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.drivesafe.databinding.FragmentProfileEditBinding
import com.example.drivesafe.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.button.MaterialButton
import com.google.android.flexbox.FlexboxLayout
import android.content.Context
import androidx.core.content.ContextCompat
import com.example.drivesafe.R
import android.content.res.ColorStateList

/**
 * A [Fragment] for editing the user's profile information.
 * Allows users to update their personal details and driving license information.
 * Interacts with the [ProfileViewModel] to fetch and save user data to Firestore.
 */
class EditProfileFragment : Fragment() {

    /**
     * View binding instance for `fragment_profile_edit.xml`.
     * This property is nullable as the binding exists only when the fragment's view is created.
     * It is nullified in [onDestroyView] to prevent memory leaks.
     */
    private var _binding: FragmentProfileEditBinding? = null

    /**
     * Provides a non-null reference to the view binding.
     * This accessor should only be used when the fragment's view is available
     * (i.e., between `onCreateView` and `onDestroyView`).
     */
    private val binding get() = _binding!!

    /**
     * Reference to the [ProfileViewModel] associated with this fragment.
     * This ViewModel handles fetching and saving user profile data.
     */
    private val viewModel: ProfileViewModel by viewModels()

    /**
     * Firebase Authentication instance to get the current user's ID and email.
     */
    private val auth = FirebaseAuth.getInstance()

    /**
     * Map defining the relationship between main license categories (A, B, C, D)
     * and their respective subcategories.
     * Subcategories like BE, CE, DE are included within the B, C, D lists respectively.
     */
    private val allLicenses = mapOf(
        "A" to listOf("AM", "A1", "A2", "A"),
        "B" to listOf("B1", "B", "BE"),
        "C" to listOf("C1", "C", "C1E", "CE"),
        "D" to listOf("D1", "D", "D1E", "DE")
    )

    /**
     * Map associating the main license category name (A, B, C, D) with the drawable resource ID
     * for its corresponding icon. Used for section titles (only A, B, C, D have icons here).
     */
    private val licenseCategoryIconMap = mapOf(
        "A" to R.drawable.ic_license_category_a,
        "B" to R.drawable.ic_license_category_b,
        "C" to R.drawable.ic_license_category_c,
        "D" to R.drawable.ic_license_category_d
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
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        val root: View = binding.root
        setupObservers()
        setupSaveButton()
        setupLicenseSubcategoryButtons()

        return root
    }

    /**
     * Sets up observers for the [ProfileViewModel]'s LiveData.
     * Updates the UI based on user profile data, loading state, save success, and error messages.
     */
    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner, Observer { user ->
            user?.let {
                binding.usernameEditTextEdit.setText(it.username)
                binding.favCarBrandEditTextEdit.setText(it.favoriteCarBrand)
                binding.placeOfBirthEditTextEdit.setText(it.placeOfBirth)
                binding.streetAddressEditTextEdit.setText(it.streetAddress)
                binding.cityOfResidenceEditTextEdit.setText(it.cityOfResidence)
                binding.yearOfBirthEditTextEdit.setText(it.yearOfBirth?.toString() ?: "")

                selectLicenseSubcategoryButtons(it.licenses)
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.profileProgressBarEdit.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.usernameEditTextEdit.isEnabled = !isLoading
            binding.favCarBrandEditTextEdit.isEnabled = !isLoading
            binding.placeOfBirthEditTextEdit.isEnabled = !isLoading
            binding.streetAddressEditTextEdit.isEnabled = !isLoading
            binding.cityOfResidenceEditTextEdit.isEnabled = !isLoading
            binding.yearOfBirthEditTextEdit.isEnabled = !isLoading

            binding.licensesFlexboxLayoutA.isEnabled = !isLoading
            binding.licensesFlexboxLayoutB.isEnabled = !isLoading
            binding.licensesFlexboxLayoutC.isEnabled = !isLoading
            binding.licensesFlexboxLayoutD.isEnabled = !isLoading

            disableFlexboxButtons(binding.licensesFlexboxLayoutA, isLoading)
            disableFlexboxButtons(binding.licensesFlexboxLayoutB, isLoading)
            disableFlexboxButtons(binding.licensesFlexboxLayoutC, isLoading)
            disableFlexboxButtons(binding.licensesFlexboxLayoutD, isLoading)

            binding.saveProfileButtonEdit.isEnabled = !isLoading
        })

        viewModel.saveSuccess.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess == true) {
                Toast.makeText(requireContext(), "Profile saved successfully!", Toast.LENGTH_SHORT)
                    .show()
                findNavController().navigate(R.id.action_editProfileFragment_to_nav_profile)
                viewModel.resetSaveSuccess()
            }
        })

        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.resetErrorMessage()
            }
        })
    }

    /**
     * Sets up the click listener for the save profile button.
     * Collects data from the input fields and selected license buttons,
     * creates an updated [User] object, and calls the ViewModel to save it.
     */
    private fun setupSaveButton() {
        binding.saveProfileButtonEdit.setOnClickListener {
            // Collect data from EditText fields, trimming whitespace.
            val username = binding.usernameEditTextEdit.text.toString().trim()
            val favCarBrand = binding.favCarBrandEditTextEdit.text.toString().trim()
            val placeOfBirth = binding.placeOfBirthEditTextEdit.text.toString().trim()
            val streetAddress = binding.streetAddressEditTextEdit.text.toString().trim()
            val cityOfResidence = binding.cityOfResidenceEditTextEdit.text.toString().trim()
            val yearOfBirthString = binding.yearOfBirthEditTextEdit.text.toString().trim()
            val yearOfBirth = yearOfBirthString.toIntOrNull()

            val selectedLicenses = getSelectedLicenses()

            val email = auth.currentUser?.email ?: ""
            val userId = auth.currentUser?.uid

            if (userId != null) {
                val updatedUser = User(
                    uid = userId,
                    email = email,
                    username = username,
                    favoriteCarBrand = favCarBrand,
                    placeOfBirth = placeOfBirth,
                    streetAddress = streetAddress,
                    cityOfResidence = cityOfResidence,
                    yearOfBirth = yearOfBirth,
                    licenses = selectedLicenses,
                    registrationTimestamp = viewModel.userProfile.value?.registrationTimestamp
                        ?: System.currentTimeMillis()
                )
                viewModel.saveUserProfile(updatedUser)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Error: User not authenticated.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Dynamically creates [MaterialButton]s for each license subcategory and adds them
     * to their corresponding [FlexboxLayout] based on the `allLicenses` map.
     * Configures buttons to be checkable and sets up click listeners to update their appearance.
     */
    private fun setupLicenseSubcategoryButtons() {
        val context = requireContext()
        val layoutParams = FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(
                context.resources.getDimensionPixelSize(R.dimen.license_button_margin),
                context.resources.getDimensionPixelSize(R.dimen.license_button_margin),
                context.resources.getDimensionPixelSize(R.dimen.license_button_margin),
                context.resources.getDimensionPixelSize(R.dimen.license_button_margin)
            )
        }

        val flexboxMap = mapOf(
            "A" to binding.licensesFlexboxLayoutA,
            "B" to binding.licensesFlexboxLayoutB,
            "C" to binding.licensesFlexboxLayoutC,
            "D" to binding.licensesFlexboxLayoutD
        )

        // Iterate through the `allLicenses` map.
        allLicenses.forEach { (category, subcategories) ->
            val flexboxLayout = flexboxMap[category]
            flexboxLayout?.let {
                subcategories.forEach { subcategory ->
                    val button = MaterialButton(context).apply {
                        this.layoutParams = layoutParams
                        text =
                            subcategory
                        isCheckable = true
                        updateLicenseButtonAppearance(this, false)
                    }
                    button.setOnClickListener {
                        updateLicenseButtonAppearance(it as MaterialButton, it.isChecked)
                    }

                    it.addView(button)
                }
            }
        }
    }

    /**
     * Selects the license subcategory buttons that match the list of licenses provided.
     * This is used to display the user's currently held licenses when the profile is loaded.
     *
     * @param licenses The list of license subcategory strings held by the user.
     */
    private fun selectLicenseSubcategoryButtons(licenses: List<String>) {
        val flexboxLayouts = listOf(
            binding.licensesFlexboxLayoutA,
            binding.licensesFlexboxLayoutB,
            binding.licensesFlexboxLayoutC,
            binding.licensesFlexboxLayoutD
        )

        flexboxLayouts.forEach { flexboxLayout ->
            for (i in 0 until flexboxLayout.childCount) {
                val button = flexboxLayout.getChildAt(i) as? MaterialButton
                button?.let {
                    val subcategory = it.text.toString()
                    it.isChecked = licenses.contains(subcategory)
                    updateLicenseButtonAppearance(it, it.isChecked)
                }
            }
        }
    }

    /**
     * Collects the list of selected license subcategory strings from all relevant FlexboxLayouts.
     *
     * @return A [List] of strings representing the selected license subcategories.
     */
    private fun getSelectedLicenses(): List<String> {
        val selected = mutableListOf<String>()
        val flexboxLayouts = listOf(
            binding.licensesFlexboxLayoutA,
            binding.licensesFlexboxLayoutB,
            binding.licensesFlexboxLayoutC,
            binding.licensesFlexboxLayoutD
        )

        flexboxLayouts.forEach { flexboxLayout ->
            for (i in 0 until flexboxLayout.childCount) {
                val button = flexboxLayout.getChildAt(i) as? MaterialButton
                if (button?.isChecked == true) {
                    selected.add(button.text.toString())
                }
            }
        }
        return selected
    }

    /**
     * Helper function to enable or disable all [MaterialButton]s within a given [FlexboxLayout].
     * This is used to prevent interaction with license buttons while saving is in progress.
     *
     * @param flexboxLayout The [FlexboxLayout] containing the buttons to enable/disable.
     * @param disable Boolean flag: true to disable buttons, false to enable them.
     */
    private fun disableFlexboxButtons(flexboxLayout: FlexboxLayout, disable: Boolean) {
        for (i in 0 until flexboxLayout.childCount) {
            flexboxLayout.getChildAt(i).isEnabled = !disable
        }
    }

    /**
     * Updates the visual appearance of a [MaterialButton] based on its checked state.
     * Changes background tint, text color, and stroke color to indicate selection.
     *
     * @param button The [MaterialButton] whose appearance should be updated.
     * @param isChecked The checked state of the button (true if selected, false if not).
     */
    private fun updateLicenseButtonAppearance(button: MaterialButton, isChecked: Boolean) {
        val context = button.context

        val selectedColor = ContextCompat.getColorStateList(context, R.color.purple_500)

        val deselectedStrokeColor =
            ContextCompat.getColorStateList(context, R.color.material_grey_700)
        val deselectedTextColor = ContextCompat.getColor(context, android.R.color.black)

        if (isChecked) {
            button.setBackgroundTintList(selectedColor)
            button.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            button.setStrokeColor(selectedColor)
        } else {
            button.setBackgroundTintList(
                ContextCompat.getColorStateList(
                    context,
                    android.R.color.transparent
                )
            )
            button.setStrokeColor(deselectedStrokeColor)
            button.setTextColor(deselectedTextColor)
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