package com.example.barcode.utils

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.barcode.R
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.core.graphics.toColorInt

class CalendarHelper(
    private val context: Context,
    private val calendarView: CalendarView,
    private val monthHeader: TextView,
    private val btnNextMonth: View,
    private val btnPreviousMonth: View,
    private val onDateSelected: (LocalDate?) -> Unit
) {
    var selectedDate: LocalDate? = null
        private set

    private var currentMonth: YearMonth = YearMonth.now()

    fun setup() {
        val startMonth = currentMonth.minusMonths(10)
        val endMonth = currentMonth.plusMonths(10)
        val firstDayOfWeek = firstDayOfWeekFromLocale()

        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        monthHeader.text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase()

        btnNextMonth.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            calendarView.smoothScrollToMonth(currentMonth)
        }

        btnPreviousMonth.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            calendarView.smoothScrollToMonth(currentMonth)
        }

        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView: TextView = view.findViewById(R.id.tvCalendarDay)
            val eventDot: View = view.findViewById(R.id.viewEventDot)
            lateinit var day: CalendarDay

            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
                        val currentSelection = selectedDate
                        if (currentSelection == day.date) {
                            selectedDate = null
                            calendarView.notifyDateChanged(day.date)
                        } else {
                            selectedDate = day.date
                            calendarView.notifyDateChanged(day.date)
                            if (currentSelection != null) {
                                calendarView.notifyDateChanged(currentSelection)
                            }
                        }
                        onDateSelected(selectedDate)
                    }
                }
            }
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                container.textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    container.textView.visibility = View.VISIBLE

                    if (data.date == selectedDate) {
                        container.textView.setBackgroundResource(R.drawable.bg_circle_purple)
                        container.textView.setTextColor(android.graphics.Color.WHITE)
                    } else if (data.date == LocalDate.now()) {
                        container.textView.background = null
                        container.textView.setTextColor("#3B205E".toColorInt())
                    } else {
                        container.textView.background = null
                        container.textView.setTextColor("#3B205E".toColorInt())
                    }
                } else {
                    container.textView.visibility = View.INVISIBLE
                }
            }
        }

        calendarView.monthScrollListener = { month ->
            currentMonth = month.yearMonth
            monthHeader.text = month.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase()
        }
    }
}