package com.example.drivesafe.ui.garage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.example.drivesafe.data.local.Car
import com.example.drivesafe.databinding.ItemCarCardBinding
import com.example.drivesafe.R

/**
 * Custom Adapter for the RecyclerView in the [GarageFragment].
 * It is responsible for taking data (a list of [Car] objects) and creating/updating the Views
 * for each item in the list to be displayed in the RecyclerView.
 */
class CarAdapter(
    /**
     * The current list of cars in the adapter.
     */
    private var cars: List<Car>,
    /**
     * The listener for handling item click events.
     */
    private val listener: OnCarClickListener
) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    /**
     * Interface to define callbacks for item click events.
     * The Fragment (or whatever uses the adapter) must implement this interface.
     */
    interface OnCarClickListener {
        /**
         * Called when the entire car item is clicked.
         * @param car The [Car] object corresponding to the clicked item.
         */
        fun onCarClick(car: Car)

        /**
         * Called when the delete button on the car item is clicked.
         * @param car The [Car] object corresponding to the item to be deleted.
         */
        fun onDeleteClick(car: Car)
    }

    /**
     * Inner ViewHolder class representing the View of a single list item (a single car card).
     * It holds references to the internal Views of the item and binds data.
     * It uses [ItemCarCardBinding] for easy access to elements defined in item_car_card.xml.
     */
    inner class CarViewHolder(private val binding: ItemCarCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Initialization block: Executed when a ViewHolder instance is created.
         */
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onCarClick(cars[position])
                }
            }
            binding.deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(cars[position])
                }
            }
        }

        /**
         * Binds the data of a [Car] object to the View fields of the item.
         * @param car The [Car] object containing the data to be displayed in this item.
         */
        fun bind(car: Car) {
            binding.carName.text = "${car.brand} ${car.model}"
            binding.carDetails.text =
                binding.root.context.getString(R.string.car_year_format, car.year)
        }
    }

    /**
     * Called by the RecyclerView when it needs a new [CarViewHolder] to represent an item.
     * @param parent The ViewGroup (RecyclerView) to which the new View will be attached.
     * @param viewType The view type of the new View (useful if you have different item types).
     * @return A new instance of [CarViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    /**
     * Called by the RecyclerView to display the data at the specified position.
     * This method updates the contents of an existing [CarViewHolder] with the item data
     * from the given position in the data list.
     * @param holder The [CarViewHolder] to update.
     * @param position The position of the item in the data list.
     */
    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = cars[position]
        holder.bind(car)
    }

    /**
     * Returns the total number of items in the data list held by the adapter.
     * @return The total number of items available in this adapter.
     */
    override fun getItemCount(): Int {
        return cars.size
    }

    /**
     * Updates the list of cars displayed by the adapter with a new list.
     * Notifies the RecyclerView that the data set has changed.
     * @param newCars The new list of [Car] objects.
     */
    fun updateCars(newCars: List<Car>) {
        cars = newCars
        notifyDataSetChanged()
    }
}
