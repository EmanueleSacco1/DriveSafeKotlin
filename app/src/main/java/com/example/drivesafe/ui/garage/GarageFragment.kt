package com.example.drivesafe.ui.garage

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog

import com.example.drivesafe.data.local.AppDatabase
import com.example.drivesafe.data.local.Car
import com.example.drivesafe.R
import com.example.drivesafe.databinding.FragmentGarageBinding
import com.example.drivesafe.ui.garage.CarAdapter.OnCarClickListener

import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle

/**
 * Fragment for the Garage screen, which displays a list of cars and allows adding new ones.
 * It implements the [OnCarClickListener] interface defined in the Adapter to handle item click events.
 */
class GarageFragment : Fragment(), OnCarClickListener {

    /**
     * View binding for the garage fragment layout.
     * This property is nullable because the binding exists only when the fragment's view is created.
     */
    private var _binding: FragmentGarageBinding? = null

    /**
     * Provides non-null access to the view binding.
     * It is safe to use only between [onCreateView] and [onDestroyView].
     */
    private val binding get() = _binding!!

    /**
     * Reference to the ViewModel associated with this Fragment.
     * Manages UI-related data and database operations for the list of cars.
     */
    private lateinit var viewModel: GarageViewModel

    /**
     * Reference to the Adapter for the RecyclerView that displays the list of cars.
     */
    private lateinit var carAdapter: CarAdapter

    /**
     * Maintains a copy of the list of cars currently displayed.
     * Primarily used to handle click events safely.
     */
    private var currentCarList: List<Car> = emptyList()

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
        _binding = FragmentGarageBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val carDao = AppDatabase.getDatabase(requireContext()).carDao()
        val factory = GarageViewModelFactory(carDao)

        viewModel = ViewModelProvider(this, factory)[GarageViewModel::class.java]
        carAdapter = CarAdapter(emptyList(), this)

        binding.containerCards.layoutManager = LinearLayoutManager(context)
        binding.containerCards.adapter = carAdapter

        viewLifecycleOwner.lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.carList.collect { carList ->
                    Log.d("GarageFragment", "Collecting car list: ${carList.size} cars.")
                    currentCarList = carList
                    carAdapter.updateCars(carList)
                }
            }
        }

        binding.addButton.setOnClickListener {
            onAddCarClick()
        }
        return root
    }

    /**
     * Called when the view associated with the fragment is being destroyed.
     * Cleans up the binding reference to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d("GarageFragment", "onDestroyView: View destroyed, binding set to null.")
    }

    /**
     * Handles the logic when the "Add Car" button is clicked.
     * Collects data from input fields, validates it, and calls the ViewModel to add a new car.
     */
    private fun onAddCarClick() {
        val brand = binding.brandInput.text.toString().trim()
        val model = binding.modelInput.text.toString().trim()
        val yearStr = binding.yearInput.text.toString().trim()

        if (brand.isBlank() || model.isBlank() || yearStr.isBlank()) {
            Toast.makeText(requireContext(), R.string.toast_fill_all_details, Toast.LENGTH_SHORT)
                .show()
            return
        }

        val year = yearStr.toIntOrNull()

        if (year == null) {

            Toast.makeText(requireContext(), R.string.toast_invalid_year, Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val newCarId = viewModel.addCar(brand, model, year)
            Log.d("GarageFragment", "onAddCarClick: New car added with ID: $newCarId")

            binding.brandInput.text?.clear()
            binding.modelInput.text?.clear()
            binding.yearInput.text?.clear()

            val bundle = Bundle().apply {
                putLong("carId", newCarId)
            }
            Log.d("GarageFragment", "onAddCarClick: Navigating to details for carId: $newCarId")

            findNavController().navigate(R.id.action_nav_garage_to_carDetailFragment, bundle)
        }
    }

    /**
     * Implementation of the [onCarClick] method from the [OnCarClickListener] interface.
     * Handles the click on a car list item.
     * @param car The [Car] object corresponding to the clicked item.
     */
    override fun onCarClick(car: Car) {
        Log.d("GarageFragment", "onCarClick: Clicked on car with ID: ${car.id}")

        val bundle = Bundle().apply {
            putLong("carId", car.id)
        }

        findNavController().navigate(R.id.action_nav_garage_to_carDetailFragment, bundle)
    }

    /**
     * Implementation of the [onDeleteClick] method from the [OnCarClickListener] interface.
     * Handles the click on the delete button of a car item.
     * Shows a confirmation dialog before proceeding with deletion.
     * @param car The [Car] object to be deleted.
     */
    override fun onDeleteClick(car: Car) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_dialog_title))
            .setMessage(
                getString(
                    R.string.delete_dialog_message,
                    "${car.brand} ${car.model}",
                    car.year
                )
            )
            .setPositiveButton(R.string.delete_dialog_confirm) { dialog, which ->
                viewModel.deleteCar(car)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_car_deleted, "${car.brand} ${car.model}"),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.delete_dialog_cancel) { dialog, which ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
}
