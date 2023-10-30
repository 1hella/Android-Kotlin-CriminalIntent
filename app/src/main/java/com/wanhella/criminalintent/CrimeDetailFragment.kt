package com.wanhella.criminalintent

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.wanhella.criminalintent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date


private const val TAG = "CrimeDetailFragment"
private const val READ_CONTACTS_PERMISSION_REQUEST_CODE = 0

class CrimeDetailFragment : Fragment() {
    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val args: CrimeDetailFragmentArgs by navArgs()

    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels {
        CrimeDetailViewModelFactory(args.crimeId)
    }

    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { parseContactSelection(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCrimeDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(title = text.toString())
                }
            }


            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(isSolved = isChecked)
                }
            }

            crimeSuspect.setOnClickListener {
                selectSuspect.launch(null)
            }

            val selectSuspectIntent = selectSuspect.contract.createIntent(
                requireContext(),
                null
            )
            crimeSuspect.isEnabled = canResolveIntent(selectSuspectIntent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeDetailViewModel.crime.collect { crime ->
                    crime?.let { updateUI(it) }
                }
            }
        }

        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _, bundle ->
            val newDate = bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date
            crimeDetailViewModel.updateCrime { it.copy(date = newDate) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_CONTACTS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    val phoneNumber = parsePhoneNumber()
                    phoneNumber?.let { number ->
                        crimeDetailViewModel.updateCrime { oldCrime ->
                            oldCrime.copy(suspectPhone = number)
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.contacts_permission_denied_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun updateUI(crime: Crime) {
        binding.apply {
            if (crimeTitle.text.toString() != crime.title) {
                crimeTitle.setText(crime.title)
            }
            crimeDate.text = crime.date.toString()
            crimeDate.setOnClickListener {
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectDate(crime.date)
                )
            }
            crimeSolved.isChecked = crime.isSolved

            crimeReport.setOnClickListener {
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject)
                    )
                }
                val chooserIntent = Intent.createChooser(
                    reportIntent,
                    getString(R.string.send_report)
                )
                startActivity(chooserIntent)
            }

            crimeSuspect.text = crime.suspect.ifEmpty {
                getString(R.string.crime_suspect_text)
            }

            if (crime.suspectPhone.isNotBlank()) {
                callSuspect.isEnabled = true
                callSuspect.text = getString(R.string.call_suspect_number, crime.suspectPhone)
                callSuspect.setOnClickListener {
                    val intent = Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel: " + crime.suspectPhone)
                    )
                    startActivity(intent)
                }
            } else {
                callSuspect.isEnabled = false
                callSuspect.text = getString(R.string.call_suspect_no_number)
            }
        }
    }

    private fun getCrimeReport(crime: Crime): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.getDateInstance(DateFormat.FULL).format(crime.date).toString()
        val suspectText = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.crime_report,
            crime.title,
            dateString,
            solvedString,
            suspectText
        )
    }

    private fun parseContactSelection(contactUri: Uri) {
        val queryFields = arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts._ID
        )

        val queryCursor = requireActivity().contentResolver
            .query(contactUri, queryFields, null, null, null)

        queryCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val suspect = cursor.getString(0)
                val idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                crimeDetailViewModel.currentSuspectId = cursor.getString(idColumnIndex)
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(suspect = suspect)
                }
            }
        }

        getContactPhoneNumber()
    }

    private fun getContactPhoneNumber() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PERMISSION_GRANTED
        ) {
            val phoneNumber = parsePhoneNumber()
            phoneNumber?.let { number ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(suspectPhone = number)
                }
            }
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder.setMessage(getString(R.string.contacts_permission))
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ ->
                        requestPermissions(
                            arrayOf(Manifest.permission.READ_CONTACTS),
                            READ_CONTACTS_PERMISSION_REQUEST_CODE
                        )
                    }
                builder
                    .create()
                    .show()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    READ_CONTACTS_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun parsePhoneNumber(): String? {
        val phoneQueryCursor = requireActivity().contentResolver
            .query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(crimeDetailViewModel.currentSuspectId),
                null
            )

        var phone: String? = null
        phoneQueryCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val phoneColumnIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                phone = cursor.getString(phoneColumnIndex)
            }
        }
        crimeDetailViewModel.currentSuspectId = null
        return phone
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }
}