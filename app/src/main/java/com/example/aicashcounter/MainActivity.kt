package com.example.aicashcounter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import android.text.TextWatcher
import android.text.Editable
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val denominations = listOf(500, 200, 100, 50, 20, 10, 5, 2, 1)
    private val counts = mutableMapOf<Int, Int>()
    private val inputs = mutableMapOf<Int, EditText>()

    private lateinit var totalText: TextView
    private lateinit var denominationText: TextView
    private lateinit var comparisonText: TextView
    private lateinit var extraInput: EditText

    private var extra = 0.0
    private val prefs by lazy {
        getSharedPreferences("cash_counter", MODE_PRIVATE)
    }

    private fun dp(n: Int): Int =
        (n * resources.displayMetrics.density).toInt()

    private fun text(
        value: String,
        size: Float = 16f,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(0xFF17251E.toInt())
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(dp(4), dp(7), dp(4), dp(7))
    }

    private fun button(
        value: String,
        action: () -> Unit
    ) = Button(this).apply {
        text = value
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun section(value: String) = text(value, 18f, true).apply {
        setPadding(dp(4), dp(18), dp(4), dp(8))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        denominations.forEach { counts[it] = 0 }
        buildScreen()
        refresh()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF2F6F3.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(0xFF0D5C3D.toInt())
        }

        header.addView(TextView(this).apply {
            text = "₹ AI CASH COUNTER"
            textSize = 24f
            setTextColor(-1)
            setTypeface(null, Typeface.BOLD)
        })

        header.addView(TextView(this).apply {
            text = "Cash • Calculator • Handwritten Scanner"
            textSize = 13f
            setTextColor(0xFFD6F1E2.toInt())
        })

        page.addView(header)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(24))
        }

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(-1)
        }

        summary.addView(text("GRAND TOTAL", 13f, true))
        totalText = text("₹0", 32f, true)
        summary.addView(totalText)
        denominationText = text("Denomination Total: ₹0")
        summary.addView(denominationText)
        body.addView(summary)

        body.addView(section("1. CASH DENOMINATION COUNTER"))

        denominations.forEach { value ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setBackgroundColor(-1)
            }

            row.addView(
                text("₹$value", 18f, true),
                LinearLayout.LayoutParams(0, dp(48), 1f)
            )

            row.addView(button("−") {
                setCount(value, (counts[value] ?: 0) - 1)
            })

            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setText("0")
                textSize = 17f
                gravity = Gravity.CENTER
                setSingleLine(true)
                filters = arrayOf(android.text.InputFilter.LengthFilter(6))
                addTextChangedListener(watcher {
                    counts[value] = (it.toIntOrNull() ?: 0).coerceIn(0, 999999)
                    refresh()
                })
            }

            inputs[value] = input
            row.addView(input, LinearLayout.LayoutParams(dp(82), dp(48)))

            row.addView(button("+") {
                setCount(value, (counts[value] ?: 0) + 1)
            })

            body.addView(row)
            val gap = TextView(this)
            gap.height = dp(3)
            body.addView(gap)
        }

        body.addView(section("2. EXTRA AMOUNT COMPARISON"))

        extraInput = EditText(this).apply {
            hint = "Enter Extra Amount (₹)"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setText("0")
            textSize = 17f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(-1)
            addTextChangedListener(watcher {
                extra = (it.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                refresh()
            })
        }
        body.addView(extraInput)

        comparisonText = text("EQUAL TO", 20f, true)
        body.addView(comparisonText)

        body.addView(text(
            "तुलना Denomination Total और Extra Amount के बीच होगी।"
        ))

        body.addView(section("3. INDEPENDENT CALCULATOR"))
        body.addView(text(
            "यह Calculator Cash Counter से अलग काम करता है।"
        ))
        body.addView(button("OPEN CALCULATOR") {
            openCalculator()
        })

        body.addView(section("4. HANDWRITTEN DENOMINATION SCANNER"))
        body.addView(text(
            "उदाहरण: 500 - 10, 200 × 5 या दो कॉलम की सूची"
        ))
        body.addView(button("SELECT HANDWRITTEN LIST PHOTO") {
            selectImage()
        })

        body.addView(section("5. SAVE AND HISTORY"))
        body.addView(button("SAVE CASH COUNT") {
            saveCount()
        })
        body.addView(button("VIEW HISTORY") {
            showHistory()
        })
        body.addView(button("COPY GRAND TOTAL") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText(
                    "Grand Total", "₹${grandTotal()}"
                )
            )
            Toast.makeText(this, "Total copied", Toast.LENGTH_SHORT).show()
        })
        body.addView(button("CLEAR ALL") {
            clearAll()
        })

        body.addView(text(
            "₹2,000 denomination is excluded.",
            12f
        ))

        scroll.addView(body)
        page.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )
        setContentView(page)
    }

    private fun watcher(action: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                action(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }

    private fun setCount(value: Int, number: Int) {
        val n = number.coerceIn(0, 999999)
        counts[value] = n
        inputs[value]?.setText(n.toString())
        inputs[value]?.setSelection(inputs[value]?.text?.length ?: 0)
        refresh()
    }

    private fun denominationTotal(): Long =
        denominations.sumOf {
            it.toLong() * (counts[it] ?: 0).toLong()
        }

    private fun grandTotal(): Double =
        denominationTotal().toDouble() + extra

    private fun refresh() {
        val denTotal = denominationTotal()
        totalText.text = "₹${"%.2f".format(Locale.US, grandTotal())}"
        denominationText.text = "Denomination Total: ₹$denTotal"

        val comparison = when {
            denTotal.toDouble() > extra -> "GREATER THAN"
            denTotal.toDouble() < extra -> "LESS THAN"
            else -> "EQUAL TO"
        }

        comparisonText.text = comparison
        comparisonText.setTextColor(
            when (comparison) {
                "GREATER THAN" -> 0xFF168044.toInt()
                "LESS THAN" -> 0xFFC0392B.toInt()
                else -> 0xFF245A9A.toInt()
            }
        )
    }

    private fun openCalculator() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }

        val first = EditText(this).apply {
            hint = "First number"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
        }

        val second = EditText(this).apply {
            hint = "Second number"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
        }

        val operator = Spinner(this)
        operator.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("+", "−", "×", "÷", "%")
        )

        val result = text("Result: 0", 20f, true)
        var answer = 0.0

        layout.addView(first)
        layout.addView(operator)
        layout.addView(second)
        layout.addView(button("CALCULATE") {
            val a = first.text.toString().toDoubleOrNull() ?: 0.0
            val b = second.text.toString().toDoubleOrNull() ?: 0.0

            answer = when (operator.selectedItemPosition) {
                0 -> a + b
                1 -> a - b
                2 -> a * b
                3 -> if (b == 0.0) Double.NaN else a / b
                else -> a * b / 100.0
            }

            result.text = if (answer.isFinite()) {
                "Result: ${"%.2f".format(Locale.US, answer)}"
            } else {
                "Cannot divide by zero"
            }
        })
        layout.addView(result)

        layout.addView(button("ADD RESULT TO EXTRA AMOUNT") {
            if (answer.isFinite()) {
                val newExtra = extra + answer
                if (newExtra >= 0) {
                    extraInput.setText(newExtra.toString())
                }
            }
        })

        AlertDialog.Builder(this)
            .setTitle("Independent Calculator")
            .setView(layout)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, 101)
    }

    @Deprecated("Legacy image picker result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            scanImage(uri)
        }
    }

    private fun scanImage(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
            )

            Toast.makeText(this, "Reading image...", Toast.LENGTH_SHORT).show()

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val found = mutableMapOf<Int, Int>()

                    val pattern = Regex(
                        """(500|200|100|50|20|10|5|2|1)\s*(?:[-xX×:]\s*|\s+)(\d{1,6})"""
                    )

                    result.text.lines().forEach { line ->
                        pattern.findAll(line).forEach { match ->
                            val denomination = match.groupValues[1].toInt()
                            val quantity = match.groupValues[2].toIntOrNull() ?: 0
                            if (denomination in denominations) {
                                found[denomination] = quantity
                            }
                        }
                    }

                    if (found.isEmpty()) {
                        AlertDialog.Builder(this)
                            .setTitle("No denomination found")
                            .setMessage(
                                "Text could not be read clearly. Try a brighter, clearer photo with one entry per line."
                            )
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        showScanConfirmation(found, result.text)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Image reading failed: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .addOnCompleteListener {
                    recognizer.close()
                }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Could not open image: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showScanConfirmation(
        found: Map<Int, Int>,
        recognizedText: String
    ) {
        val message = buildString {
            found.forEach { (denomination, quantity) ->
                append("₹$denomination × $quantity\n")
            }
            append("\nPlease check the quantities before applying.")
        }

        AlertDialog.Builder(this)
            .setTitle("Scanned denominations")
            .setMessage(message)
            .setPositiveButton("APPLY TO COUNTER") { _, _ ->
                found.forEach { (denomination, quantity) ->
                    setCount(denomination, quantity)
                }
            }
            .setNeutralButton("SHOW RECOGNIZED TEXT") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Recognized text")
                    .setMessage(recognizedText)
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun saveCount() {
        val history = JSONArray(prefs.getString("history", "[]"))
        val record = org.json.JSONObject().apply {
            put(
                "date",
                SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
                ).format(Date())
            )
            put("denominationTotal", denominationTotal())
            put("extra", extra)
            put("grandTotal", grandTotal())
            put("counts", org.json.JSONObject().apply {
                denominations.forEach { put(it.toString(), counts[it] ?: 0) }
            })
        }

        history.put(record)
        prefs.edit().putString("history", history.toString()).apply()
        Toast.makeText(this, "Cash count saved", Toast.LENGTH_SHORT).show()
    }

    private fun showHistory() {
        val history = JSONArray(prefs.getString("history", "[]"))

        if (history.length() == 0) {
            AlertDialog.Builder(this)
                .setTitle("History")
                .setMessage("No saved records yet.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val lines = (history.length() - 1 downTo 0).map { index ->
            val item = history.getJSONObject(index)
            "${item.optString("date")}\nGrand Total: ₹${item.optDouble("grandTotal")}"
        }

        AlertDialog.Builder(this)
            .setTitle("Saved Cash History")
            .setItems(lines.toTypedArray(), null)
            .setPositiveButton("Close", null)
            .setNeutralButton("Clear History") { _, _ ->
                prefs.edit().putString("history", "[]").apply()
            }
            .show()
    }

    private fun clearAll() {
        denominations.forEach { setCount(it, 0) }
        extraInput.setText("0")
        extra = 0.0
        refresh()
    }
}
