package com.wanhella.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.os.Bundle
import android.widget.TimePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import java.util.Calendar

class TimePickerFragment : DialogFragment() {

    private val args: TimePickerFragmentArgs by navArgs()
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        calendar.time = args.crimeDate
        val listener = OnTimeSetListener { _: TimePicker, hour: Int, minute: Int ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)

            setFragmentResult(
                REQUEST_KEY_TIME,
                bundleOf(BUNDLE_KEY_TIME to calendar.time)
            )
        }
        val initialHour = calendar.get(Calendar.HOUR)
        val initialMinute = calendar.get(Calendar.MINUTE)
        return TimePickerDialog(
            requireContext(),
            listener,
            initialHour,
            initialMinute,
            false
        )
    }

    companion object {
        const val REQUEST_KEY_TIME = "REQUEST_KEY_TIME"
        const val BUNDLE_KEY_TIME = "BUNDLE_KEY_TIME"
    }
}