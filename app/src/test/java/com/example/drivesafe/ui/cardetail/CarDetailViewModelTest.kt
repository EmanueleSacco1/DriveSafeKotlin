package com.example.drivesafe.ui.cardetail

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.drivesafe.R
import com.example.drivesafe.data.local.Car
import com.example.drivesafe.data.local.CarDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.Calendar
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull


/**
 * Unit test class for [CarDetailViewModel].
 * Uses JUnit4 and Mockito to test the ViewModel's logic in isolation.
 */
@ExperimentalCoroutinesApi
class CarDetailViewModelTest {

    // Rule to execute LiveData synchronously.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Test dispatcher for coroutines, to control time execution.
    private val testDispatcher = StandardTestDispatcher()

    // Mock of CarDao, required for the ViewModel.
    @Mock
    private lateinit var mockCarDao: CarDao

    // Mock of Android context, required to access string resources.
    @Mock
    private lateinit var mockContext: Context

    // The ViewModel instance to be tested.
    private lateinit var viewModel: CarDetailViewModel

    // A MutableStateFlow to simulate CarDao emissions.
    private val carFlow = MutableStateFlow<Car?>(null)

    // MockedStatic for android.util.Log to handle static method calls like Log.d()
    private lateinit var mockedStaticLog: MockedStatic<Log>

    /**
     * Initial setup before each test.
     * Initializes mocks and the ViewModel.
     */
    @Before
    fun setup() {
        // Initializes mocks annotated with @Mock.
        MockitoAnnotations.openMocks(this)
        // Sets the main dispatcher for coroutines to the test dispatcher.
        Dispatchers.setMain(testDispatcher)

        // Mock the static Log class to prevent "Method d in android.util.Log not mocked" error.
        // This is necessary because unit tests run on a JVM, not on an Android device.
        mockedStaticLog = mockStatic(Log::class.java)
        // Stub the Log.d method to return 0 (or any int) to avoid NullPointerException.
        // The actual value returned doesn't matter for the test logic.
        mockedStaticLog.`when`<Int> { Log.d(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString()) }.thenReturn(0)
        mockedStaticLog.`when`<Int> { Log.w(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString()) }.thenReturn(0)
        mockedStaticLog.`when`<Int> { Log.e(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.any()) }.thenReturn(0)


        // Configures the CarDao mock to return the carFlow when getCarById is called.
        `when`(mockCarDao.getCarById(anyLong())).thenReturn(carFlow)

        // Initializes the ViewModel with a dummy ID and the mocked DAO.
        viewModel = CarDetailViewModel(1L, mockCarDao)

        // Configures the context mock to return strings for error messages.
        `when`(mockContext.getString(R.string.error_revision_date_in_past)).thenReturn("Data revisione nel passato")
        `when`(mockContext.getString(R.string.error_revision_odometer_positive)).thenReturn("Chilometraggio revisione deve essere positivo")
        `when`(mockContext.getString(R.string.error_bollo_cost_positive)).thenReturn("Costo bollo deve essere positivo")
        `when`(mockContext.getString(R.string.countdown_expired)).thenReturn("Scaduto!")
        `when`(mockContext.getString(R.string.countdown_due_today)).thenReturn("Scade oggi!")
        `when`(mockContext.getString(R.string.countdown_due_in_days, 1)).thenReturn("1 giorno")
        `when`(mockContext.getString(R.string.countdown_due_in_days, 5)).thenReturn("5 giorni")
        `when`(mockContext.getString(R.string.countdown_due_in_months_only, 1)).thenReturn("1 mese")
        `when`(mockContext.getString(R.string.countdown_due_in_months_only, 2)).thenReturn("2 mesi")
        `when`(mockContext.getString(R.string.unit_month_singular)).thenReturn("mese")
        `when`(mockContext.getString(R.string.unit_months_plural)).thenReturn("mesi")
        `when`(mockContext.getString(R.string.unit_day_singular)).thenReturn("giorno")
        `when`(mockContext.getString(R.string.unit_days_plural)).thenReturn("giorni")
        `when`(mockContext.getString(R.string.countdown_due_in_combined, "1 mese e 1 giorno")).thenReturn("Scade tra 1 mese e 1 giorno")
        `when`(mockContext.getString(R.string.countdown_due_in_combined, "2 mesi e 5 giorni")).thenReturn("Scade tra 2 mesi e 5 giorni")
    }

    /**
     * Cleanup after each test.
     * Resets the main dispatcher and closes the static mock.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Close the static mock to release resources and prevent interference with other tests.
        mockedStaticLog.close()
    }

    // Tests for calculateRcaExpirationDate

    @Test
    fun `calculateRcaExpirationDate should return null if rcaPaidTimestamp is null`() {
        val result = viewModel.rcaExpirationDate.value
        assertNull(result)
    }

    // --- Tests for getExpirationStatus ---

    @Test
    fun `getExpirationStatus should return UNKNOWN if timestamp is null`() {
        assertEquals(ExpirationStatus.UNKNOWN, viewModel.getExpirationStatus(null))
    }

    @Test
    fun `getExpirationStatus should return EXPIRED if date is in the past`() {
        val pastDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        assertEquals(ExpirationStatus.EXPIRED, viewModel.getExpirationStatus(pastDate))
    }

    @Test
    fun `getExpirationStatus should return TODAY if date is today`() {
        val today = Calendar.getInstance().timeInMillis
        assertEquals(ExpirationStatus.TODAY, viewModel.getExpirationStatus(today))
    }

    @Test
    fun `getExpirationStatus should return NEAR if date is within 30 days`() {
        val nearFuture = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 15) }.timeInMillis
        assertEquals(ExpirationStatus.NEAR, viewModel.getExpirationStatus(nearFuture))
    }

    @Test
    fun `getExpirationStatus should return FUTURE if date is beyond 30 days`() {
        val farFuture = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 45) }.timeInMillis
        assertEquals(ExpirationStatus.FUTURE, viewModel.getExpirationStatus(farFuture))
    }

    // Tests for validateCarData

    @Test
    fun `validateCarData should return null for valid car data`() {
        val validCar = Car(
            id = 1L,
            brand = "Fiat",
            model = "Panda",
            year = 2020,
            nextRevisionDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 10) }.timeInMillis,
            revisionOdometer = 10000,
            bolloCost = 150.0,
            bolloExpirationDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 20) }.timeInMillis
        )
        assertNull(viewModel.validateCarData(validCar, mockContext))
    }

    @Test
    fun `validateCarData should return error for past revision date`() {
        val pastRevisionCar = Car(
            id = 1L,
            brand = "Fiat",
            model = "Panda",
            year = 2020,
            nextRevisionDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -5) }.timeInMillis
        )
        assertEquals(R.string.error_revision_date_in_past, viewModel.validateCarData(pastRevisionCar, mockContext))
    }

    @Test
    fun `validateCarData should return error for negative revision odometer`() {
        val negativeOdometerCar = Car(
            id = 1L,
            brand = "Fiat",
            model = "Panda",
            year = 2020,
            revisionOdometer = -100
        )
        assertEquals(R.string.error_revision_odometer_positive, viewModel.validateCarData(negativeOdometerCar, mockContext))
    }

    @Test
    fun `validateCarData should return error for non-positive bollo cost`() {
        val zeroBolloCostCar = Car(
            id = 1L,
            brand = "Fiat",
            model = "Panda",
            year = 2020,
            bolloCost = 0.0
        )
        assertEquals(R.string.error_bollo_cost_positive, viewModel.validateCarData(zeroBolloCostCar, mockContext))

        val negativeBolloCostCar = Car(
            id = 1L,
            brand = "Fiat",
            model = "Panda",
            year = 2020,
            bolloCost = -50.0
        )
        assertEquals(R.string.error_bollo_cost_positive, viewModel.validateCarData(negativeBolloCostCar, mockContext))
    }

    // Tests for saveCar (DAO interaction verification)

    @Test
    fun `saveCar should call updateCar on CarDao`() = runTest {
        val carToSave = Car(id = 1L, brand = "Updated", model = "Car", year = 2021)
        viewModel.saveCar(carToSave)
        advanceUntilIdle() // Ensures the coroutine completes

        // Verifies that updateCar was called exactly once with the correct Car object.
        org.mockito.Mockito.verify(mockCarDao).updateCar(carToSave)
    }
}
