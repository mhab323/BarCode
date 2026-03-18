package com.example.barcode.ui

import android.R
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.icu.util.Calendar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.barcode.databinding.ActivityNewEventBinding
import com.example.barcode.firebase.FirebaseManager
import com.example.barcode.model.Cocktail
import java.lang.String.format
import java.util.Locale
import androidx.core.graphics.toColorInt

class NewEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewEventBinding
   private var isOpenBarSelected = true
    private var finalCocktailMenu = ArrayList<Cocktail>()
    private var selectedDateString: String = ""
    private var selectedBartenderId: String = ""

    private val menuBuilderLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {

            val bundle = result.data!!.extras
            val returnedMenu = bundle?.getParcelableArrayList<Cocktail>(CocktailSwipeActivity.KEY_SELECTED_MENU)

            if (returnedMenu != null) {
                finalCocktailMenu = returnedMenu

                Toast.makeText(this, "Menu locked! ${finalCocktailMenu.size} drinks selected.", Toast.LENGTH_SHORT).show()

            }
        }
    }

    companion object {
        private const val STATE_BILLING_TOGGLE = "state_billing_toggle"
        const val BARTENDER_ID = "BARTENDER_ID"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNewEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        if(savedInstanceState != null){
            isOpenBarSelected = savedInstanceState.getBoolean(STATE_BILLING_TOGGLE,true)
        }

        updateBillingToggleUI()
        setUpListeners()
        calendarListener()
    }
    @SuppressLint("DefaultLocale")
    private fun calendarListener() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val formattedMonth = String.format("%02d", month + 1)
            val formattedDay = String.format("%02d", dayOfMonth)
            selectedDateString = "$year-$formattedMonth-$formattedDay"
        }
    }
    private fun setUpListeners() {
            binding.btnBack.setOnClickListener { navigateBack() }
            binding.btnOpenBar.setOnClickListener { selectOpenBar() }
            binding.btnPayPerDrink.setOnClickListener { selectPayPerDrink() }
            binding.btnOpenMenuArrow.setOnClickListener { openMenuBuilder() }
            binding.btnLaunchEvent.setOnClickListener { validateAndLaunchEvent() }
            binding.btnSetTime.setOnClickListener { openTimePicker() }
    }

    private fun updateBillingToggleUI() {
        if (isOpenBarSelected) {
            binding.btnOpenBar.setBackgroundResource(com.example.barcode.R.drawable.bg_rounded_white)
            binding.btnOpenBar.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B328C6"))
            binding.btnOpenBar.setTextColor(Color.WHITE)

            binding.btnPayPerDrink.background = null
            binding.btnPayPerDrink.setTextColor(Color.parseColor("#9E9E9E"))
        } else {
            binding.btnPayPerDrink.setBackgroundResource(com.example.barcode.R.drawable.bg_rounded_white)
            binding.btnPayPerDrink.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B328C6"))
            binding.btnPayPerDrink.setTextColor(Color.WHITE)

            binding.btnOpenBar.background = null
            binding.btnOpenBar.setTextColor(Color.parseColor("#9E9E9E"))
        }
    }
    private fun selectOpenBar() {
        if (!isOpenBarSelected) {
            isOpenBarSelected = true
            updateBillingToggleUI()
        }
    }
    private fun selectPayPerDrink() {
        if (isOpenBarSelected) {
            isOpenBarSelected = false
            updateBillingToggleUI()
        }
    }
    private fun navigateBack() {
        finish()
    }

    private fun openMenuBuilder() {
        val editText = android.widget.EditText(this)
        editText.hint = "Enter Bartender Code (e.g. A7X9P2)"
        editText.isAllCaps = true
        editText.setSingleLine()
        editText.setPadding(50, 50, 50, 50)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Connect Bartender")
            .setMessage("Enter your Bartender's code to view their menu.")
            .setView(editText)
            .setPositiveButton("Search") { _, _ ->
                val code = editText.text.toString().trim()
                if (code.isNotEmpty()) {
                    verifyBartenderAndOpenMenu(code)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun verifyBartenderAndOpenMenu(code: String) {
        FirebaseManager.getBartenderByShareCode(code,
            onSuccess = { bartender ->
                selectedBartenderId = bartender.uid
                Toast.makeText(this, "Connected to ${bartender.name}!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, CocktailSwipeActivity::class.java)
                intent.putExtra(BARTENDER_ID, bartender.uid)
                menuBuilderLauncher.launch(intent)
            },
            onFailure = { error ->
                Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun validateAndLaunchEvent() {
        //TODO TO MAKE THIS FUNC MORE MODULAR
        val eventName = binding.etEventName.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val guestsCount = binding.etGuests.text.toString().toIntOrNull() ?: 0

        if (selectedDateString.isEmpty()) {
            Toast.makeText(this, "Please select a date from the calendar", Toast.LENGTH_SHORT).show()
            return
        }
        val date = selectedDateString
        val timeStr = binding.btnSetTime.text.toString()
        if (timeStr.contains("Time", ignoreCase = true)) {
            Toast.makeText(this, "Please set a time for the event", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        var scheduledMillis = 0L
        try {
            val dateTimeString = "$date $timeStr"
            val format = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a",Locale.getDefault())
            val dateObj = format.parse(dateTimeString)
            scheduledMillis = dateObj?.time ?: 0L
        } catch (e: Exception) {
           Toast.makeText(this, "Error formatting time", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val billingType = if (isOpenBarSelected) "Open Bar" else "Pay per Drink"

        if (eventName.isEmpty() || location.isEmpty() || finalCocktailMenu.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and add cocktails", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseManager.auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: User session lost. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        if (selectedBartenderId.isEmpty()) {
            Toast.makeText(this, "Please select a menu first!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnLaunchEvent.isEnabled = false
        binding.btnLaunchEvent.text = "SAVING..."

        val uniqueJoinCode = generateJoinCode()

        val menuMapList = finalCocktailMenu.map { cocktail ->
            hashMapOf(
                "cocktailId" to cocktail.cocktailId,
                "bartenderId" to cocktail.bartenderId,
                "description" to cocktail.description,
                "name" to cocktail.name,
                "ingredients" to cocktail.ingredients,
                "imageUrl" to cocktail.imageUrl
            )
        }

        val eventMap = hashMapOf(
            "eventName" to eventName,
            "hostId" to userId,
            "dateString" to date,
            "timestamp" to System.currentTimeMillis(),
            "timeString" to timeStr,
            "scheduledTimeMillis" to scheduledMillis,
            "location" to location,
            "billingType" to billingType,
            "status" to "upcoming",
            "numOfGuests" to guestsCount,
            "menu" to menuMapList,
            "joinCode" to uniqueJoinCode,
            "bartenderIds" to listOf(selectedBartenderId)
        )
        saveEventToDb(eventMap)
    }

    private fun saveEventToDb(eventMap: HashMap<String, Any>) {
        try {
            FirebaseManager.saveEvent(
                eventData = eventMap,
                onSuccess = {
                    Toast.makeText(this, "Event Launched!", Toast.LENGTH_SHORT).show()
                    routeUserToDashboard()
                },
                onFailure = { exception ->
                    binding.btnLaunchEvent.isEnabled = true
                    binding.btnLaunchEvent.text = "LAUNCH EVENT"
                    Toast.makeText(this, "Firebase Error: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        } catch (e: Exception) {
            binding.btnLaunchEvent.isEnabled = true
            binding.btnLaunchEvent.text = "LAUNCH EVENT"
            Toast.makeText(this, "App Crash Caught: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("FIREBASE_ERROR", "Crash during save", e)
        }
    }
    private fun routeUserToDashboard() {
        val role = com.example.barcode.utils.UserManager.currentUser?.role
        val intent = Intent(this, AdminHostActivity::class.java)

        if (role == "admin") {
            intent.putExtra(AdminHostActivity.USER_ROLE, "ADMIN")
        } else {
            intent.putExtra(AdminHostActivity.USER_ROLE, "HOST")
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun openTimePicker() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val isPM = selectedHour >= 12
                val displayHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                val amPm = if (isPM) "PM" else "AM"

                val formattedTime =
                    format(Locale.getDefault(), "%02d:%02d %s", displayHour, selectedMinute, amPm)

                binding.btnSetTime.text = formattedTime

                binding.btnSetTime.setTextColor("#B328C6".toColorInt())
            },
            currentHour,
            currentMinute,
            false
        )
        timePickerDialog.show()
    }

    private fun generateJoinCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..5)
            .map { allowedChars.random() }
            .joinToString("")
    }

}









