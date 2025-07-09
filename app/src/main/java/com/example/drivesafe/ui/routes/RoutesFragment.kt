package com.example.drivesafe.ui.routes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drivesafe.R
import com.example.drivesafe.data.model.Route
import com.example.drivesafe.data.model.User
import com.example.drivesafe.databinding.FragmentRoutesBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * A [Fragment] responsible for displaying a list of recorded driving routes.
 * It fetches route data and associated user information from Firebase Firestore
 * and displays them in a [RecyclerView]. It also handles navigation to
 * the [RouteDetailFragment] when a route item is clicked.
 */
class RoutesFragment : Fragment(), RouteAdapter.OnItemClickListener {

    /**
     * View binding instance for accessing layout elements.
     * The backing property `_binding` is nullable to manage its lifecycle.
     */
    private var _binding: FragmentRoutesBinding? = null

    /**
     * A non-null accessor for the view binding instance.
     * This property should only be accessed between `onCreateView` and `onDestroyView`.
     */
    private val binding get() = _binding!!

    /**
     * Adapter for the [RecyclerView] that displays the list of routes.
     */
    private lateinit var routeAdapter: RouteAdapter

    /**
     * Firebase Firestore instance for database interactions.
     */
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * A [Job] for managing the lifecycle of coroutines launched within this fragment.
     * It allows for cancellation of all coroutines when the fragment's view is destroyed.
     */
    private val fragmentJob = Job()

    /**
     * A [CoroutineScope] tied to the fragment's lifecycle, ensuring that coroutines
     * are cancelled when the fragment's view is destroyed to prevent leaks.
     */
    private val fragmentScope = CoroutineScope(Dispatchers.Main + fragmentJob)

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutesBinding.inflate(inflater, container, false)
        val root = binding.root

        binding.routesRecyclerView.layoutManager = LinearLayoutManager(context)
        routeAdapter = RouteAdapter(emptyMap<String, String>(), emptyList(), this)
        binding.routesRecyclerView.adapter = routeAdapter

        return root
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been restored.
     * Initiates fetching of all routes and associated usernames.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchAllRoutesAndUsernames()
    }

    /**
     * Fetches all routes from Firestore and then retrieves the usernames for the unique user IDs
     * associated with those routes. This operation is performed asynchronously using coroutines.
     */
    private fun fetchAllRoutesAndUsernames() {
        fragmentScope.launch {
            try {
                val routesSnapshot = firestore.collection("routes")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val routes = mutableListOf<Route>()
                val userIds = mutableSetOf<String>()

                for (document in routesSnapshot.documents) {
                    try {
                        val route = document.toObject(Route::class.java)
                        route?.let {
                            routes.add(it)
                            userIds.add(it.userId)
                        }
                    } catch (e: Exception) {
                        Log.e("RoutesFragment", "Error parsing Route document: ${document.id}", e)
                    }
                }

                val usersMap = mutableMapOf<String, String>()

                if (userIds.isNotEmpty()) {
                    val userFetchTasks = userIds.map { userId ->
                        async(Dispatchers.IO) {
                            try {
                                firestore.collection("users").document(userId).get().await()
                            } catch (e: Exception) {
                                Log.e(
                                    "RoutesFragment",
                                    "Error fetching user document for ID: $userId", e
                                )
                                null
                            }
                        }
                    }

                    val userDocuments = userFetchTasks.awaitAll().filterNotNull()

                    for (document in userDocuments) {
                        try {
                            val user = document.toObject(User::class.java)
                            user?.let {
                                usersMap[document.id] =
                                    it.username.ifEmpty { "Anonymous" }
                            }
                        } catch (e: Exception) {
                            Log.e(
                                "RoutesFragment",
                                "Error parsing User document: ${document.id}", e
                            )
                        }
                    }
                }

                routeAdapter.updateData(routes, usersMap)

                if (routes.isEmpty()) {
                    Toast.makeText(requireContext(), "No routes found.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading routes: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("RoutesFragment", "Error loading routes", e)
                } else {
                    Log.d("RoutesFragment", "Coroutine cancelled", e)
                }
            }
        }
    }

    /**
     * Callback method invoked when a route item in the RecyclerView is clicked.
     * Navigates to the [RouteDetailFragment] with the selected route's ID.
     *
     * @param route The [Route] object corresponding to the clicked item.
     */
    override fun onItemClick(route: Route) {
        val bundle = Bundle().apply {
            putString("routeId", route.id)
        }
        findNavController().navigate(R.id.action_nav_routes_to_routeDetailFragment, bundle)
    }

    /**
     * Called when the view hierarchy associated with the fragment is being removed.
     * Cleans up resources by cancelling ongoing coroutines and nullifying the view binding.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        fragmentJob.cancel()
        _binding = null
    }
}