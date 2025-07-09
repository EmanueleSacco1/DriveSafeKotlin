package com.example.drivesafe.ui.routes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drivesafe.R
import com.example.drivesafe.data.model.Route
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A [RecyclerView.Adapter] for displaying a list of [Route] objects.
 * This adapter manages the data and binds it to the individual item views within a [RecyclerView].
 *
 * @param usersMap A map where keys are user IDs and values are their corresponding usernames, used for displaying the route author.
 * @param routes The current list of [Route] objects to be displayed. Defaults to an empty list.
 * @param onItemClick An instance of [OnItemClickListener] to handle click events on route items.
 */
class RouteAdapter(
    private var usersMap: Map<String, String>,
    private var routes: List<Route> = emptyList(),
    private val onItemClick: OnItemClickListener
) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    /**
     * Interface definition for a callback to be invoked when an item in the RecyclerView is clicked.
     */
    interface OnItemClickListener {
        /**
         * Called when a route item has been clicked.
         * @param route The [Route] object associated with the clicked item.
         */
        fun onItemClick(route: Route)
    }

    /**
     * [RecyclerView.ViewHolder] that holds the views for a single route item.
     *
     * @param itemView The root view of the item layout.
     */
    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userDisplayNameTextView: TextView = itemView.findViewById(R.id.userDisplayNameTextView)
        val routeNameTextView: TextView = itemView.findViewById(R.id.routeNameTextView)
        val routeDateTextView: TextView = itemView.findViewById(R.id.routeDateTextView)
        val routeDistanceTextView: TextView = itemView.findViewById(R.id.routeDistanceTextView)

        /**
         * Binds a [Route] object's data to the views within this ViewHolder.
         *
         * @param route The [Route] object containing the data to be displayed.
         * @param usersMap A map of user IDs to usernames, used to resolve the route's author name.
         * @param clickListener The listener for item click events.
         */
        fun bind(route: Route, usersMap: Map<String, String>, clickListener: OnItemClickListener) {
            val username = usersMap[route.userId] ?: "Unknown User"
            userDisplayNameTextView.text = "By: $username"

            val startTimeDate = Date(route.startTime)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            routeNameTextView.text = "Route ${dateFormat.format(startTimeDate)}"

            val durationMinutes = (route.endTime - route.startTime) / 60000.0
            routeDateTextView.text = "Duration: ${String.format("%.0f", durationMinutes)} minutes"

            routeDistanceTextView.text =
                "Distance: ${String.format("%.2f", route.totalDistance / 1000)} km"

            itemView.setOnClickListener { clickListener.onItemClick(route) }
        }
    }

    /**
     * Called when [RecyclerView] needs a new [RouteViewHolder] of the given type to represent an item.
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [RouteViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    /**
     * Called by [RecyclerView] to display the data at the specified position.
     * This method updates the contents of the [RouteViewHolder.itemView] to reflect the item at the given position.
     * @param holder The [RouteViewHolder] which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routes[position], usersMap, onItemClick)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items.
     */
    override fun getItemCount(): Int = routes.size

    /**
     * Updates the adapter's data sets and notifies the [RecyclerView] to refresh its views.
     * This method should be called whenever the underlying data for the adapter changes.
     *
     * @param newRoutes The new list of [Route] objects.
     * @param newUsersMap The new map of user IDs to usernames.
     */
    fun updateData(newRoutes: List<Route>, newUsersMap: Map<String, String>) {
        routes = newRoutes
        usersMap = newUsersMap
        notifyDataSetChanged()
    }
}