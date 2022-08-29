package com.clevertap.demo

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.clevertap.android.sdk.CleverTapAPI

class EmailDialogFragment : DialogFragment() {

    private var editTextTextEmailAddress: EditText? = null
    private var textViewOK: TextView? = null

    companion object {

        const val TAG = "EmailDialogFragment"
    }

    override fun onStart() {
        super.onStart()
        dialog?.window!!.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        dialog?.setCancelable(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        val defaultInstance = CleverTapAPI.getDefaultInstance(requireActivity().applicationContext)
        val view = inflater.inflate(R.layout.layout_dialog_email, container, false)
        editTextTextEmailAddress = view.findViewById(R.id.editTextTextEmailAddress)
        textViewOK = view.findViewById(R.id.textViewOK)
        val newProfile = HashMap<String, Any>()
        textViewOK?.setOnClickListener {
            newProfile["Email"] = editTextTextEmailAddress?.text.toString()
            defaultInstance?.onUserLogin(newProfile)
            with (sharedPref!!.edit()) {
                putString("email", editTextTextEmailAddress?.text.toString())
                apply()
            }
            dialog?.dismiss()
        }

        return view
    }
}