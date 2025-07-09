package com.example.drivesafe.ui.cardetail

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.util.Log
import androidx.core.content.ContextCompat

import com.example.drivesafe.databinding.FragmentCarDetailBinding
import com.example.drivesafe.data.local.AppDatabase
import com.example.drivesafe.data.local.Car
import com.example.drivesafe.R

import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.lang.NumberFormatException

/**
 * Fragment responsible for displaying and editing the details of a single car.
 * It observes the car data (exposed as LiveData) from its associated ViewModel.
 */
class CarDetailFragment : Fragment() {

    /**
     * View binding for the car detail fragment layout.
     * This property is only valid between onCreateView and onDestroyView.
     */
    private var _binding: FragmentCarDetailBinding? = null

    /**
     * Provides non-null access to the view binding.
     * Valid only between onCreateView and onDestroyView.
     */
    private val binding get() = _binding!!

    /**
     * Reference to the ViewModel associated with this Fragment.
     * Manages UI-related data and business logic for car details.
     */
    private lateinit var viewModel: CarDetailViewModel

    /**
     * The ID of the car whose details are being displayed/edited.
     * Initialized to -1L (an invalid value).
     */
    private var carId: Long = -1L

    /**
     * Date formatter for converting between timestamps (Long) and date strings (e.g., dd/MM/yyyy).
     * Uses the format specified in the string resources.
     */
    private lateinit var dateFormatter: SimpleDateFormat

    /**
     * Called when the fragment is first created.
     * Performs initial setup not tied to the view creation.
     * @param savedInstanceState The previously saved state of the fragment, if any.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("CarDetailFragment", "onCreate: Fragment created.")
        dateFormatter =
            SimpleDateFormat(getString(R.string.date_format_display), Locale.getDefault())

        carId = arguments?.getLong("carId", -1L) ?: -1L
        Log.d("CarDetailFragment", "onCreate: Received carId: $carId")

        // Check if the received car ID is valid (-1L indicates an error or missing ID).
        if (carId == -1L) {
            Toast.makeText(requireContext(), R.string.error_car_not_found, Toast.LENGTH_SHORT)
                .show()
            findNavController().popBackStack()
            Log.e("CarDetailFragment", "onCreate: Received invalid carId.")
            return
        }

        val carDao = AppDatabase.getDatabase(requireContext()).carDao()

        val factory = CarDetailViewModelFactory(carId, carDao)

        viewModel = ViewModelProvider(this, factory)[CarDetailViewModel::class.java]
        Log.d("CarDetailFragment", "onCreate: ViewModel initialized with carId: $carId")
    }

    /**
     * Called to create the view hierarchy associated with the fragment.
     * @param inflater Used to inflate any views in the fragment.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState The previously saved state.
     * @return The root View for the fragment's UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCarDetailBinding.inflate(inflater, container, false)

        viewModel.car.observe(viewLifecycleOwner) { car -> // 'car' is the value emitted by the LiveData (Car or null).
            // Use the Elvis operator (?.let) to execute the block only if 'car' is not null.
            car?.let {
                Log.d("CarDetailFragment", "Car data observed: Found car with ID: ${it.id}")
                updateUIWithCarDetails(it)
            } ?: run {
                Log.d("CarDetailFragment", "Car data observed: Car is null.")
                Snackbar.make(binding.root, R.string.error_car_not_found, Snackbar.LENGTH_SHORT)
                    .show()
                findNavController().popBackStack()
            }
        }

        // Observe the calculated RCA expiration date LiveData.
        viewModel.rcaExpirationDate.observe(viewLifecycleOwner) { expirationDate ->
            expirationDate?.let { binding.tvRcaExpirationDate.text = dateFormatter.format(it) }
                ?: binding.tvRcaExpirationDate.setText("")
        }

        // Observe the RCA countdown text LiveData.
        viewModel.rcaCountdown.observe(viewLifecycleOwner) { countdownTextPart ->
            val status = viewModel.getExpirationStatus(viewModel.rcaExpirationDate.value?.time)
            binding.tvRcaCountdown.text = formatCountdownString(status, countdownTextPart)
            binding.tvRcaCountdown.setTextColor(getCountdownColor(status))
        }

        // Observe the Revision countdown text LiveData.
        viewModel.revisionCountdown.observe(viewLifecycleOwner) { countdownTextPart ->
            val status = viewModel.getExpirationStatus(viewModel.car.value?.nextRevisionDate)
            binding.tvRevisionCountdown.text = formatCountdownString(status, countdownTextPart)
            binding.tvRevisionCountdown.setTextColor(getCountdownColor(status))
        }

        // Observe the Bollo countdown text LiveData.
        viewModel.bolloCountdown.observe(viewLifecycleOwner) { countdownTextPart ->
            val status = viewModel.getExpirationStatus(viewModel.car.value?.bolloExpirationDate)
            binding.tvBolloCountdown.text = formatCountdownString(status, countdownTextPart)
            binding.tvBolloCountdown.setTextColor(getCountdownColor(status))
        }

        // Set listeners for date fields to show the DatePicker ---
        binding.etRcaPaidDate.setOnClickListener { showDatePickerDialog(binding.etRcaPaidDate) }
        binding.etNextRevisionDate.setOnClickListener { showDatePickerDialog(binding.etNextRevisionDate) }
        binding.etRegistrationDate.setOnClickListener { showDatePickerDialog(binding.etRegistrationDate) }
        binding.etBolloExpirationDate.setOnClickListener { showDatePickerDialog(binding.etBolloExpirationDate) }
        // Listeners for non-date fields (kept empty as in the previous example).
        binding.etRevisionOdometer.setOnClickListener { /* No specific action for odometer click */ }
        binding.etBolloCost.setOnClickListener { /* No specific action for bollo cost click */ }

        // Set listener for the Save button ---
        binding.btnSave.setOnClickListener { saveCarDetails() }

        return binding.root
    }

    /**
     * Called when the view associated with the fragment is being destroyed.
     * Cleans up the binding reference to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("CarDetailFragment", "onDestroyView: View destroyed, binding set to null.")
    }

    /**
     * Updates the UI elements of the screen with the details of the provided Car object.
     * @param car The Car object containing the details to display.
     */
    private fun updateUIWithCarDetails(car: Car) {
        binding.tvBrand.text = car.brand
        binding.tvModel.text = car.model
        binding.tvYear.text = car.year.toString()

        car.rcaPaidDate?.let { timestamp ->
            binding.etRcaPaidDate.setText(dateFormatter.format(Date(timestamp)))
        } ?: binding.etRcaPaidDate.setText("")

        car.nextRevisionDate?.let { timestamp ->
            binding.etNextRevisionDate.setText(dateFormatter.format(Date(timestamp)))
        } ?: binding.etNextRevisionDate.setText("")

        car.revisionOdometer?.let { odometer -> binding.etRevisionOdometer.setText(odometer.toString()) }
            ?: binding.etRevisionOdometer.setText("")

        car.registrationDate?.let { timestamp ->
            binding.etRegistrationDate.setText(dateFormatter.format(Date(timestamp)))
        } ?: binding.etRegistrationDate.setText("")

        car.bolloCost?.let { cost ->
            binding.etBolloCost.setText(String.format(Locale.getDefault(), "%.2f", cost))
        } ?: binding.etBolloCost.setText("")

        car.bolloExpirationDate?.let { timestamp ->
            binding.etBolloExpirationDate.setText(dateFormatter.format(Date(timestamp)))
        } ?: binding.etBolloExpirationDate.setText("")
    }

    /**
     * Shows a DatePickerDialog to select a date for the specified input field.
     * @param editText The TextInputEditText field where the selected date will be placed.
     */
    private fun showDatePickerDialog(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val currentText = editText.text.toString()

        try {
            dateFormatter.isLenient = false // Disable lenient mode for stricter parsing.
            val existingDate = dateFormatter.parse(currentText)
            // If parsing is successful, set the calendar's date to the existing date.
            existingDate?.let { calendar.time = it }
        } catch (e: Exception) {
            // If parsing fails, log the error but continue with the current date.
            e.printStackTrace()
        } finally {
            dateFormatter.isLenient = true // Re-enable lenient mode after parsing.
        }

        // Get the year, month, and day from the current (or parsed) date.
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create and show the DatePickerDialog.
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Create a new calendar with the selected date.
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                // Clear the time components to store only the date in the timestamp.
                selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
                selectedCalendar.set(Calendar.MINUTE, 0)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                // Format the selected date and set the text in the input field.
                editText.setText(dateFormatter.format(selectedCalendar.time))
                // Remove any error messages associated with the parent TextInputLayout.
                (editText.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error =
                    null
            },
            // Pass the initial year, month, and day for the dialog.
            year, month, day
        )
        datePickerDialog.show() // Show the dialog.
    }

    /**
     * Collects modified data from UI fields, validates it, and calls the ViewModel to save the car details.
     * This function launches a coroutine to perform the suspend operation in the ViewModel.
     */
    private fun saveCarDetails() {
        // Launch a coroutine using the fragment's viewLifecycleScope.
        // This ensures the coroutine is automatically cancelled when the view is destroyed.
        viewLifecycleOwner.lifecycleScope.launch {
            // Reset error messages on fields before new validation.
            binding.tilRcaPaidDate.error = null
            binding.tilNextRevisionDate.error = null
            binding.tilRegistrationDate.error = null
            binding.tilBolloExpirationDate.error = null
            binding.tilRevisionOdometer.error = null
            binding.tilBolloCost.error = null

            // Collect text from input fields, trimming leading/trailing spaces.
            val rcaPaidDateStr = binding.etRcaPaidDate.text.toString().trim()
            val nextRevisionDateStr = binding.etNextRevisionDate.text.toString().trim()
            val registrationDateStr = binding.etRegistrationDate.text.toString().trim()
            val bolloExpirationDateStr = binding.etBolloExpirationDate.text.toString().trim()
            val revisionOdometerStr = binding.etRevisionOdometer.text.toString().trim()
            val bolloCostStr = binding.etBolloCost.text.toString().trim()

            // Attempt to parse date strings into Long timestamps, handling errors.
            // If an error occurs, set the error message on the corresponding TextInputLayout and stop the coroutine.
            val rcaPaidDateTimestamp: Long? = try {
                parseDateStringToTimestamp(rcaPaidDateStr)
            } catch (e: Exception) {
                binding.tilRcaPaidDate.error = getString(
                    R.string.error_invalid_date_format,
                    getString(R.string.label_rca_paid_date)
                )
                return@launch // Stop the current coroutine.
            }
            val nextRevisionDateTimestamp: Long? = try {
                parseDateStringToTimestamp(nextRevisionDateStr)
            } catch (e: Exception) {
                binding.tilNextRevisionDate.error = getString(
                    R.string.error_invalid_date_format,
                    getString(R.string.label_next_revision_date)
                )
                return@launch
            }
            val registrationDateTimestamp: Long? = try {
                parseDateStringToTimestamp(registrationDateStr)
            } catch (e: Exception) {
                binding.tilRegistrationDate.error = getString(
                    R.string.error_invalid_date_format,
                    getString(R.string.label_registration_date)
                )
                return@launch
            }
            val bolloExpirationDateTimestamp: Long? = try {
                parseDateStringToTimestamp(bolloExpirationDateStr)
            } catch (e: Exception) {
                binding.tilBolloExpirationDate.error = getString(
                    R.string.error_invalid_date_format,
                    getString(R.string.label_bollo_expiration_date)
                )
                return@launch
            }

            // Attempt to convert number strings to Int or Double, handling errors.
            val revisionOdometer: Int? = try {
                revisionOdometerStr.toIntOrNull()
            } catch (e: NumberFormatException) {
                null
            }
            val bolloCost: Double? = try {
                bolloCostStr.replace(',', '.').toDoubleOrNull()
            } catch (e: NumberFormatException) {
                null
            }

            // Specifically check if number strings are not blank but conversion failed.
            if (revisionOdometerStr.isNotBlank() && revisionOdometer == null) {
                binding.tilRevisionOdometer.error = getString(
                    R.string.error_invalid_number_format,
                    getString(R.string.label_revision_odometer)
                )
                return@launch
            }
            if (bolloCostStr.isNotBlank() && bolloCost == null) {
                binding.tilBolloCost.error = getString(
                    R.string.error_invalid_number_format,
                    getString(R.string.label_bollo_cost)
                )
                return@launch
            }

            // Get the current Car object from the ViewModel via its LiveData.
            // Use .value to access the current value of the LiveData. It can be null.
            val currentCar = viewModel.car.value

            // If the current car has not been loaded (is null), we cannot save.
            if (currentCar == null) {
                Snackbar.make(
                    binding.root,
                    R.string.error_car_not_found_save,
                    Snackbar.LENGTH_SHORT
                ).show()
                return@launch
            }

            // Create a copy of the current Car object with updated fields from the collected values.
            val updatedCar = currentCar.copy(
                rcaPaidDate = rcaPaidDateTimestamp,
                nextRevisionDate = nextRevisionDateTimestamp,
                revisionOdometer = revisionOdometer,
                registrationDate = registrationDateTimestamp,
                bolloCost = bolloCost,
                bolloExpirationDate = bolloExpirationDateTimestamp
            )

            // Call the validation function in the ViewModel to check data consistency (e.g., dates in the future).
            val validationErrorStringResId = viewModel.validateCarData(updatedCar, requireContext())

            // If validation is successful (no error)
            if (validationErrorStringResId == null) {
                // Call the saveCar function in the ViewModel to update the database.
                viewModel.saveCar(updatedCar)
                // Show a success message to the user.
                Snackbar.make(binding.root, R.string.toast_details_saved, Snackbar.LENGTH_SHORT)
                    .show()
                // Navigate back to the previous screen.
                findNavController().popBackStack()
            } else {
                // If validation fails, show an error message.
                Snackbar.make(binding.root, validationErrorStringResId, Snackbar.LENGTH_LONG).show()

                // Set a specific error message on the corresponding TextInputLayout for the validation error.
                when (validationErrorStringResId) {
                    R.string.error_revision_date_in_past -> binding.tilNextRevisionDate.error =
                        getString(validationErrorStringResId)

                    R.string.error_revision_odometer_positive -> binding.tilRevisionOdometer.error =
                        getString(validationErrorStringResId)

                    R.string.error_bollo_cost_positive -> binding.tilBolloCost.error =
                        getString(validationErrorStringResId)
                }
            }
        }
    }

    /**
     * Converts a date string (in the format specified by dateFormatter) into a Long timestamp.
     * @param dateString The date string to parse.
     * @return A Long timestamp representing the date, or null if the string is blank.
     * @throws Exception if the date string format is invalid.
     */
    private fun parseDateStringToTimestamp(dateString: String): Long? {
        if (dateString.isBlank()) {
            return null
        }
        return try {
            dateFormatter.isLenient = false // Use strict parsing.
            val date = dateFormatter.parse(dateString) // Attempt to parse the string.
            if (date == null) {
                throw Exception("Invalid date format for $dateString")
            } // Throw exception if parse returns null.
            val calendar = Calendar.getInstance().apply { time = date }
            // Clear time components to get a start-of-day timestamp.
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis // Return the timestamp in milliseconds.
        } catch (e: Exception) {
            e.printStackTrace() // Log the exception.
            throw e // Re-throw the exception to be handled by the caller.
        } finally {
            dateFormatter.isLenient = true // Re-enable lenient mode.
        }
    }

    /**
     * Formats the complete countdown string (e.g., "Scade tra 3 mesi e 15 giorni")
     * based on the expiration status (EXPIRED, TODAY, NEAR, FUTURE) and the text part
     * calculated by the ViewModel (e.g., "3 e 15" or "30").
     * @param status The ExpirationStatus of the item.
     * @param countdownTextPart The calculated text part for the countdown (e.g., number of days/months).
     * @return The formatted countdown string.
     */
    private fun formatCountdownString(status: ExpirationStatus, countdownTextPart: String): String {
        val context = requireContext() // Get the context to access string resources.
        return when (status) {
            ExpirationStatus.EXPIRED -> context.getString(R.string.countdown_expired) // "Scaduto!"
            ExpirationStatus.TODAY -> context.getString(R.string.countdown_due_today) // "Scade oggi!"
            ExpirationStatus.UNKNOWN -> "" // No information, empty string.
            else -> { // NEAR or FUTURE
                val parts =
                    countdownTextPart.split(" e ") // Split the text part (e.g., "3 e 15") by " e ".
                val formattedParts = parts.mapIndexed { index, valueStr ->
                    val value = valueStr.toIntOrNull() ?: 0 // Convert the part to a number.
                    if (parts.size == 1) { // If there is only one part (e.g., only days or only months).
                        if (status == ExpirationStatus.NEAR) {
                            context.getString(
                                R.string.countdown_due_in_days,
                                value
                            ) // Format "X giorni".
                        } else {
                            context.getString(
                                R.string.countdown_due_in_months_only,
                                value
                            ) // Format "X mesi" (if only months and not NEAR).
                        }
                    } else { // If there are two parts (months and days).
                        if (index == 0) {
                            "$value ${context.getString(if (value == 1) R.string.unit_month_singular else R.string.unit_months_plural)}" // Format months.
                        } else {
                            "$value ${context.getString(if (value == 1) R.string.unit_day_singular else R.string.unit_days_plural)}" // Format days.
                        }
                    }
                }
                if (parts.size > 1) {
                    context.getString(
                        R.string.countdown_due_in_combined,
                        formattedParts.joinToString(" e ")
                    ) // Combined format "X mesi e Y giorni".
                } else if (formattedParts.isNotEmpty()) {
                    formattedParts.first() // Format for only months or only days.
                } else {
                    "" // Fallback case.
                }
            }
        }
    }

    /**
     * Gets the text color for the countdown based on the expiration status.
     * @param status The ExpirationStatus of the item.
     * @return The color resource ID for the countdown text.
     */
    private fun getCountdownColor(status: ExpirationStatus): Int {
        val context = requireContext() // Get the context.
        // Return the appropriate color from Android resources or themes.
        return when (status) {
            ExpirationStatus.EXPIRED -> ContextCompat.getColor(
                context,
                android.R.color.holo_red_dark
            ) // Dark red for expired.
            ExpirationStatus.TODAY, ExpirationStatus.NEAR -> ContextCompat.getColor(
                context,
                android.R.color.holo_orange_dark
            ) // Dark orange for today or near.
            ExpirationStatus.FUTURE -> ContextCompat.getColor(
                context,
                android.R.color.holo_green_dark
            ) // Dark green for future.
            ExpirationStatus.UNKNOWN -> ContextCompat.getColor(
                context,
                android.R.color.tab_indicator_text
            ) // Default or less visible color for unknown.
        }
    }
}
