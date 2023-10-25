package com.wanhella.criminalintent

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.wanhella.criminalintent.databinding.ListItemCrimeBinding
import com.wanhella.criminalintent.databinding.ListItemSeriousCrimeBinding

class CrimeHolder(
    val binding: ListItemCrimeBinding
) : ViewHolder(binding.root) {
    fun bind(crime: Crime) {
        binding.crimeTitle.text = crime.title
        binding.crimeDate.text = crime.date.toString()

        binding.root.setOnClickListener {
            Toast.makeText(
                binding.root.context,
                "${crime.title} clicked!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

class SeriousCrimeHolder(
    val binding: ListItemSeriousCrimeBinding
) : ViewHolder(binding.root) {
    fun bind(crime: Crime) {
        binding.policeButton.setOnClickListener {
            Toast.makeText(binding.root.context,
                "Reporting crime ${crime.title}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private const val SERIOUS_CRIME_TYPE = 0
private const val CRIME_TYPE = 1

class CrimeListAdapter(
    private val crimes: List<Crime>
) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) : ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == SERIOUS_CRIME_TYPE) {
            val binding = ListItemSeriousCrimeBinding.inflate(inflater, parent, false)
            SeriousCrimeHolder(binding)
        } else {
            val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
            CrimeHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val crime = crimes[position]
        val type = getItemViewType(position)
        if (type == SERIOUS_CRIME_TYPE) {
            val seriousCrimeHolder: SeriousCrimeHolder =  holder as SeriousCrimeHolder
            seriousCrimeHolder.bind(crime)
        } else if (type == CRIME_TYPE) {
            val crimeHolder: CrimeHolder = holder as CrimeHolder
            crimeHolder.bind(crime)
        }
    }

    override fun getItemCount() = crimes.size

    override fun getItemViewType(position: Int): Int {
        super.getItemViewType(position)
        return if (crimes[position].requiresPolice) {
            SERIOUS_CRIME_TYPE
        } else {
            CRIME_TYPE
        }
    }
}