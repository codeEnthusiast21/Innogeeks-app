package edu.kiet.innogeeks.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.UserDataManager
import edu.kiet.innogeeks.databinding.FragmentReasonOfAbsenceBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "reasonOfAbsenceFragment"

class reasonOfAbsenceFragment : Fragment() {

    private var _binding: FragmentReasonOfAbsenceBinding? = null
    private val binding get() = _binding!!
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val currentDate: String by lazy {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReasonOfAbsenceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editTextDate.text = currentDate
        binding.btnSubmit.setOnClickListener { submitReason() }
        displayUserDetails()
    }
    private fun displayUserDetails() {
        val userData = UserDataManager.getCurrentUser()
        if (userData == null) {
            showToast("User data not available")
            return
        }

        binding.apply {
            labelStdName.text = "Name: ${userData.name}"
            labelGrade.text = "Domain: ${userData.domain}"
        }
    }

    private fun submitReason() {
        val reason = binding.editTextTextMultiLine.text.toString()
        if (reason.isEmpty()) {
            showToast("Please enter a reason")
            return
        }
        val userData = UserDataManager.getCurrentUser()
        if (userData == null) {
            showToast("User data not available")
            return
        }
        val studentRef = db.collection("Domains")
            .document(userData.domain)
            .collection("Students")
            .document(userData.uid)

        studentRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val existingReasons = document.get("reasonOfAbsence") as? Map<String, String> ?: mapOf()
                    val updatedReasons = existingReasons.toMutableMap().apply {
                        put(currentDate, reason)
                    }
                    studentRef.update("reasonOfAbsence", updatedReasons)
                        .addOnSuccessListener {
                            hideKeyboard()
                            showToast("Reason submitted successfully")
                            binding.editTextTextMultiLine.text.clear()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error submitting reason", e)
                            showToast("Failed to submit reason: ${e.message}")
                        }
                } else {
                    showToast("Student document not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching existing reasons", e)
                showToast("Failed to fetch existing reasons: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}