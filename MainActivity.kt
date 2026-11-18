package com.example.sitevisitdetails

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("site_visit_data", MODE_PRIVATE) }
    private val entries = mutableListOf<JSONObject>()
    private lateinit var listContainer: LinearLayout
    private lateinit var formContainer: LinearLayout
    private lateinit var dateButton: Button
    private lateinit var monthSpinner: Spinner
    private lateinit var dayValue: TextView
    private lateinit var autoBus: EditText
    private lateinit var train: EditText
    private lateinit var totalValue: TextView
    private var selectedDate = Calendar.getInstance()
    private var editingId: String? = null

    private val months = arrayOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadEntries()
        showDashboard()
    }

    private fun bg(color: Int, radius: Float = 18f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(value: String, size: Float, color: Int = Color.BLACK, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun button(label: String, color: Int, action: () -> Unit): Button =
        Button(this).apply {
            text = label; setTextColor(Color.WHITE); textSize = 14f
            background = bg(color, 24f); isAllCaps = false
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { action() }
        }

    private fun baseColumn(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(18), dp(16), dp(24))
        setBackgroundColor(Color.rgb(244, 247, 255))
    }

    private fun showDashboard() {
        val scroll = ScrollView(this)
        val root = baseColumn()
        scroll.addView(root)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(48, 63, 159), Color.rgb(0, 150, 136))).apply { cornerRadius = dp(22).toFloat() }
        }
        header.addView(text("SITE VISIT DETAILS", 23f, Color.WHITE, true))
        header.addView(text("Daily field activity & travel tracker", 13f, Color.WHITE).apply {
            setPadding(0, dp(6), 0, 0)
        })
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(14), 0, dp(14)) }
        stats.addView(statCard("TOTAL VISITS", entries.size.toString(), Color.rgb(232, 240, 255)), LinearLayout.LayoutParams(0, dp(88), 1f))
        val expense = entries.sumOf { it.optDouble("total", 0.0) }
        stats.addView(statCard("TRAVEL TOTAL", "₹${"%.2f".format(expense)}", Color.rgb(231, 248, 242)), LinearLayout.LayoutParams(0, dp(88), 1f).apply { leftMargin = dp(10) })
        root.addView(stats)

        root.addView(button("+  ADD NEW SITE VISIT", Color.rgb(103, 58, 183)) {
            editingId = null; showForm()
        }, LinearLayout.LayoutParams(-1, dp(54)))
        root.addView(text("Recent site visits", 19f, Color.rgb(35, 45, 85), true).apply {
            setPadding(0, dp(22), 0, dp(10))
        })
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listContainer)
        if (entries.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No entries yet.\nTap “ADD NEW SITE VISIT” to record your first visit."
                textSize = 15f; gravity = Gravity.CENTER; setTextColor(Color.GRAY)
                setPadding(dp(20), dp(36), dp(20), dp(36)); background = bg(Color.WHITE)
            }
            listContainer.addView(empty)
        } else {
            entries.sortedByDescending { it.optString("dateKey") }.forEach { item ->
                listContainer.addView(entryCard(item))
            }
        }
        setContentView(scroll)
    }

    private fun statCard(label: String, value: String, color: Int): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(10), dp(10)); background = bg(color)
            addView(text(label, 10f, Color.DKGRAY, true))
            addView(text(value, 20f, Color.rgb(35, 45, 85), true).apply { setPadding(0, dp(6), 0, 0) })
        }

    private fun entryCard(item: JSONObject): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
            background = bg(Color.WHITE); elevation = dp(2).toFloat()
        }
        val title = item.optString("name", "Unnamed") + "  •  " + item.optString("designation")
        card.addView(text(title, 16f, Color.rgb(42, 52, 105), true))
        card.addView(text("${item.optString("date")}  |  ${item.optString("store")}  |  ${item.optString("format")}", 12f, Color.DKGRAY).apply {
            setPadding(0, dp(6), 0, dp(5))
        })
        card.addView(text("Planned: ${item.optString("agenda")}", 13f))
        card.addView(text("Executed: ${item.optString("executed")}", 13f).apply { setPadding(0, dp(4), 0, 0) })
        card.addView(text("Auto/Bus: ₹${item.optString("autoBus")}    Train: ₹${item.optString("train")}    Total: ₹${item.optString("total")}",
            13f, Color.rgb(0, 121, 107), true).apply { setPadding(0, dp(8), 0, dp(8)) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("EDIT", Color.rgb(25, 118, 210)) {
            editingId = item.optString("id"); showForm(item)
        })
        actions.addView(button("DELETE", Color.rgb(198, 40, 40)) {
            entries.removeAll { it.optString("id") == item.optString("id") }
            saveEntries(); showDashboard()
        }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        card.addView(actions)
        return card.apply { setPadding(0, 0, 0, dp(12)) }
    }

    private fun showForm(existing: JSONObject? = null) {
        selectedDate = Calendar.getInstance()
        if (existing != null) {
            val parts = existing.optString("dateKey").split("-")
            if (parts.size == 3) selectedDate.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        val scroll = ScrollView(this)
        formContainer = baseColumn()
        scroll.addView(formContainer)
        formContainer.addView(text(if (existing == null) "Add site visit" else "Edit site visit", 24f, Color.rgb(40, 53, 120), true))
        formContainer.addView(text("Enter the daily visit details below.", 13f, Color.GRAY).apply { setPadding(0, dp(5), 0, dp(16)) })

        val name = addInput("Name", existing?.optString("name") ?: "")
        val designation = addSpinner("Designation", arrayOf("ALPO", "MLP", "ELP", "Other"), existing?.optString("designation") ?: "ALPO")
        monthSpinner = addSpinner("Month", months, existing?.optString("month") ?: months[selectedDate.get(Calendar.MONTH)])
        dateButton = button("", Color.rgb(25, 118, 210)) { openDatePicker() }
        formContainer.addView(text("Date", 13f, Color.DKGRAY, true).apply { setPadding(0, dp(12), 0, dp(5)) })
        formContainer.addView(dateButton, LinearLayout.LayoutParams(-1, dp(50)))
        dayValue = text("", 13f, Color.rgb(0, 121, 107), true)
        formContainer.addView(dayValue.apply { setPadding(0, dp(7), 0, dp(4)) })
        updateDateLabels()
        val store = addInput("Store Name", existing?.optString("store") ?: "")
        val format = addSpinner("Format/Store", arrayOf("IBBN", "MDC"), existing?.optString("format") ?: "IBBN")
        val agenda = addInput("Agenda / Activity Planned by MLP/ELP", existing?.optString("agenda") ?: "", true)
        val executed = addInput("Activity Executed", existing?.optString("executed") ?: "", true)
        autoBus = addInput("Auto/Bus (₹)", existing?.optString("autoBus") ?: "0", false, true)
        train = addInput("Train (₹)", existing?.optString("train") ?: "0", false, true)
        totalValue = text("Total: ₹0.00", 18f, Color.rgb(0, 121, 107), true)
        formContainer.addView(totalValue.apply { setPadding(0, dp(14), 0, dp(14)) })
        val watcher = android.text.TextWatcherAdapter { updateTotal() }
        autoBus.addTextChangedListener(watcher); train.addTextChangedListener(watcher); updateTotal()

        formContainer.addView(button(if (existing == null) "SAVE SITE VISIT" else "UPDATE SITE VISIT", Color.rgb(0, 137, 123)) {
            if (name.text.toString().trim().isEmpty()) {
                name.error = "Please enter Name"; return@button
            }
            val record = JSONObject()
            record.put("id", existing?.optString("id") ?: java.util.UUID.randomUUID().toString())
            record.put("name", name.text.toString().trim())
            record.put("designation", designation.selectedItem.toString())
            record.put("month", monthSpinner.selectedItem.toString())
            record.put("date", dateButton.text.toString())
            record.put("dateKey", String.format(Locale.US, "%04d-%02d-%02d", selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH)+1, selectedDate.get(Calendar.DAY_OF_MONTH)))
            record.put("day", dayValue.text.toString().removePrefix("Day: "))
            record.put("store", store.text.toString().trim())
            record.put("format", format.selectedItem.toString())
            record.put("agenda", agenda.text.toString().trim())
            record.put("executed", executed.text.toString().trim())
            record.put("autoBus", amount(autoBus.text.toString()))
            record.put("train", amount(train.text.toString()))
            record.put("total", amount(autoBus.text.toString()) + amount(train.text.toString()))
            if (existing != null) {
                val index = entries.indexOfFirst { it.optString("id") == existing.optString("id") }
                if (index >= 0) entries[index] = record else entries.add(record)
            } else entries.add(record)
            saveEntries(); showDashboard()
        }, LinearLayout.LayoutParams(-1, dp(54)))
        formContainer.addView(button("CANCEL", Color.rgb(96, 105, 125)) { showDashboard() },
            LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })
        setContentView(scroll)
    }

    private fun addInput(label: String, value: String, multiline: Boolean = false, numeric: Boolean = false): EditText {
        formContainer.addView(text(label, 13f, Color.DKGRAY, true).apply { setPadding(0, dp(12), 0, dp(5)) })
        val field = EditText(this).apply {
            setText(value); textSize = 14f; setSingleLine(!multiline)
            setPadding(dp(12), dp(10), dp(12), dp(10)); background = bg(Color.WHITE, 12f)
            if (multiline) { minLines = 3; gravity = Gravity.TOP }
            if (numeric) inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        formContainer.addView(field, LinearLayout.LayoutParams(-1, if (multiline) dp(94) else dp(50)))
        return field
    }

    private fun addSpinner(label: String, options: Array<String>, selected: String): Spinner {
        formContainer.addView(text(label, 13f, Color.DKGRAY, true).apply { setPadding(0, dp(12), 0, dp(5)) })
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        val index = options.indexOf(selected).coerceAtLeast(0)
        spinner.setSelection(index)
        spinner.background = bg(Color.WHITE, 12f)
        formContainer.addView(spinner, LinearLayout.LayoutParams(-1, dp(50)))
        return spinner
    }

    private fun openDatePicker() {
        DatePickerDialog(this, { _, year, month, day ->
            selectedDate.set(year, month, day)
            updateDateLabels()
            monthSpinner.setSelection(month)
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateLabels() {
        dateButton.text = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(selectedDate.time)
        dayValue.text = "Day: " + SimpleDateFormat("EEEE", Locale.ENGLISH).format(selectedDate.time)
    }

    private fun amount(s: String): Double = s.toDoubleOrNull() ?: 0.0
    private fun updateTotal() {
        if (::totalValue.isInitialized && ::autoBus.isInitialized && ::train.isInitialized)
            totalValue.text = "Total: ₹${"%.2f".format(amount(autoBus.text.toString()) + amount(train.text.toString()))}"
    }

    private fun loadEntries() {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) entries.add(arr.getJSONObject(i))
    }
    private fun saveEntries() {
        val arr = JSONArray()
        entries.forEach { arr.put(it) }
        prefs.edit().putString("entries", arr.toString()).apply()
    }
}

private class TextWatcherAdapter(val changed: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
