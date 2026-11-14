package com.example.aicashcounter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val navy = Color.rgb(10, 19, 36)
    private val panel = Color.rgb(22, 35, 55)
    private val emerald = Color.rgb(0, 190, 130)
    private val white = Color.WHITE
    private val muted = Color.rgb(165, 180, 198)

    private val denominations = listOf(500, 200, 100, 50, 20, 10, 5, 2, 1)
    private val counts = mutableMapOf<Int, Int>()
    private val inputs = mutableMapOf<Int, EditText>()
    private val amountLabels = mutableMapOf<Int, TextView>()

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var totalText: TextView
    private lateinit var notesText: TextView
    private lateinit var comparisonText: TextView
    private lateinit var extraInput: EditText
    private lateinit var extraTotalText: TextView

    private var extra = 0.0
    private var calculatorResult = 0.0
    private var currentScreen = "COUNTER"

    private val prefs by lazy {
        getSharedPreferences("cash_counter", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        loadCounts()
        showApp()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun makeText(
        value: String,
        size: Float = 15f,
        color: Int = white,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, 1)
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun makeButton(value: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = value
            textSize = 12f
            setTextColor(white)
            setBackgroundColor(emerald)
            setOnClickListener { action() }
        }
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(panel)
        }
    }

    private fun addSpace(parent: LinearLayout, height: Int = 10) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun showApp() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(navy)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
            setBackgroundColor(navy)
        }

        header.addView(makeText("AI CASH COUNTER", 21f, emerald, true))
        header.addView(makeText("Smart cash management", 12f, muted))
        root.addView(header)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
        root.addView(
            content,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(panel)
        }

        nav.addView(
            makeButton("COUNTER") { showCounter() },
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        nav.addView(
            makeButton("CALC") { showCalculator() },
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        root.addView(nav)

        setContentView(root)
        showCounter()
    }

    private fun showCounter() {
        currentScreen = "COUNTER"
        content.removeAllViews()
        inputs.clear()
        amountLabels.clear()

        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(page)
        content.addView(scroll)

        val summary = makeCard()
        summary.addView(makeText("TOTAL CASH AMOUNT", 12f, muted, true))
        totalText = makeText("₹0", 30f, emerald, true)
        summary.addView(totalText)
        notesText = makeText("Total Notes: 0", 14f, white, true)
        summary.addView(notesText)
        page.addView(summary)

        addSpace(page)

        val extraCard = makeCard()
        extraCard.addView(makeText("EXTRA AMOUNT", 13f, emerald, true))

        extraInput = EditText(this).apply {
            setTextColor(white)
            setHintTextColor(muted)
            hint = "Enter extra amount"
            inputType = 8194
            setSingleLine(true)
            setText(extra.toString())
            setPadding(dp(10), 0, dp(10), 0)
            setBackgroundColor(Color.rgb(35, 50, 70))
        }
        extraCard.addView(
            extraInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
            )
        )

        comparisonText = makeText("COMPARISON: EQUAL TO", 15f, emerald, true)
        extraCard.addView(comparisonText)
        extraTotalText = makeText("Grand Total: ₹0", 13f, muted)
        extraCard.addView(extraTotalText)

        extraInput.addTextChangedListener(watcher {
            extra = extraInput.text.toString().toDoubleOrNull() ?: 0.0
            refresh()
        })
        page.addView(extraCard)

        addSpace(page)

        val denominationCard = makeCard()
        denominationCard.addView(
            makeText("DENOMINATION COUNTER", 15f, emerald, true)
        )
        addSpace(denominationCard, 6)

        denominations.forEach { denomination ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, dp(5))
            }

            row.addView(
                makeText("₹$denomination", 15f, white, true),
                LinearLayout.LayoutParams(dp(58), dp(42))
            )

            row.addView(
                makeButton("−") {
                    setCount(denomination, (counts[denomination] ?: 0) - 1)
                },
                LinearLayout.LayoutParams(dp(42), dp(42))
            )

            val quantity = EditText(this).apply {
                setTextColor(white)
                setHintTextColor(muted)
                hint = "0"
                gravity = Gravity.CENTER
                inputType = 2
                setSingleLine(true)
                setText((counts[denomination] ?: 0).toString())
                setPadding(0, 0, 0, 0)
                setBackgroundColor(Color.rgb(35, 50, 70))
            }
            inputs[denomination] = quantity

            quantity.addTextChangedListener(watcher {
                counts[denomination] =
                    quantity.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
                refresh()
            })

            row.addView(
                quantity,
                LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    setMargins(dp(5), 0, dp(5), 0)
                }
            )

            row.addView(
                makeButton("+") {
                    setCount(denomination, (counts[denomination] ?: 0) + 1)
                },
                LinearLayout.LayoutParams(dp(42), dp(42))
            )

            val amount = makeText("₹0", 13f, emerald, true)
            amount.gravity = Gravity.END or Gravity.CENTER_VERTICAL
            amountLabels[denomination] = amount
            row.addView(
                amount,
                LinearLayout.LayoutParams(dp(76), dp(42))
            )

            denominationCard.addView(row)
            val line = View(this).apply {
                setBackgroundColor(Color.rgb(48, 63, 82))
            }
            denominationCard.addView(
                line,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                )
            )
        }

        page.addView(denominationCard)
        addSpace(page)

        val tools = makeCard()
        tools.addView(makeText("CASH TOOLS", 14f, emerald, true))

        val toolRow1 = LinearLayout(this)
        toolRow1.addView(
            makeButton("SCAN NOTES") { selectImage() },
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        toolRow1.addView(
            makeButton("SAVE COUNT") { saveCount() },
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        tools.addView(toolRow1)

        addSpace(tools, 6)

        val toolRow2 = LinearLayout(this)
        toolRow2.addView(
            makeButton("HISTORY") { showHistory() },
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        toolRow2.addView(
            makeButton("COPY TOTAL") {
                val clip = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clip.setPrimaryClip(
                    android.content.ClipData.newPlainText(
                        "Cash Total", denominationTotal().toString()
                    )
                )
                Toast.makeText(this, "Total copied", Toast.LENGTH_SHORT).show()
            },
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        tools.addView(toolRow2)

        addSpace(tools, 6)
        tools.addView(
            makeButton("CLEAR ALL") {
                AlertDialog.Builder(this)
                    .setTitle("Clear all?")
                    .setMessage("All denomination counts and extra amount will be cleared.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear") { _, _ ->
                        denominations.forEach { setCount(it, 0) }
                        extraInput.setText("0")
                    }.show()
            }
        )
        page.addView(tools)

        refresh()
    }

    private fun showCalculator() {
        currentScreen = "CALC"
        content.removeAllViews()

        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(page)
        content.addView(scroll)

        val card = makeCard()
        card.addView(makeText("SMART CALCULATOR", 18f, emerald, true))
        addSpace(card)

        val first = numberInput("First amount")
        val second = numberInput("Second amount")
        card.addView(first)
        addSpace(card, 8)
        card.addView(second)
        addSpace(card)

        val operator = Spinner(this)
        operator.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Addition (+)", "Subtraction (−)", "Multiplication (×)", "Division (÷)", "Percentage (%)")
        )
        card.addView(operator)
        addSpace(card)

        val resultText = makeText("Result: ₹0", 23f, emerald, true)
        card.addView(resultText)

        card.addView(
            makeButton("CALCULATE") {
                val a = first.text.toString().toDoubleOrNull() ?: 0.0
                val b = second.text.toString().toDoubleOrNull() ?: 0.0
                calculatorResult = when (operator.selectedItemPosition) {
                    0 -> a + b
                    1 -> a - b
                    2 -> a * b
                    3 -> if (b == 0.0) Double.NaN else a / b
                    else -> a * b / 100.0
                }
                resultText.text =
                    if (calculatorResult.isNaN()) "Cannot divide by zero"
                    else "Result: ₹${format(calculatorResult)}"
            }
        )

        addSpace(card)
        card.addView(
            makeButton("ADD RESULT TO EXTRA AMOUNT") {
                if (!calculatorResult.isNaN() && calculatorResult.isFinite()) {
                    extra += calculatorResult
                    Toast.makeText(this, "Result added to Extra Amount", Toast.LENGTH_SHORT).show()
                    showCounter()
                }
            }
        )
        page.addView(card)

        addSpace(page)
        val gst = makeCard()
        gst.addView(makeText("GST QUICK CALCULATOR", 16f, emerald, true))
        gst.addView(makeText("Enter base amount above, then select GST", 12f, muted))
        addSpace(gst)

        val gstRow1 = LinearLayout(this)
        listOf(3, 5, 12).forEach { rate ->
            gstRow1.addView(
                makeButton("+$rate%") {
                    val amount = first.text.toString().toDoubleOrNull() ?: 0.0
                    calculatorResult = amount * (1 + rate / 100.0)
                    resultText.text = "With $rate% GST: ₹${format(calculatorResult)}"
                },
                LinearLayout.LayoutParams(0, dp(46), 1f)
            )
        }
        gst.addView(gstRow1)

        val gstRow2 = LinearLayout(this)
        listOf(18, 28).forEach { rate ->
            gstRow2.addView(
                makeButton("+$rate%") {
                    val amount = first.text.toString().toDoubleOrNull() ?: 0.0
                    calculatorResult = amount * (1 + rate / 100.0)
                    resultText.text = "With $rate% GST: ₹${format(calculatorResult)}"
                },
                LinearLayout.LayoutParams(0, dp(46), 1f)
            )
        }
        gstRow2.addView(
            makeButton("REMOVE GST") {
                val amount = first.text.toString().toDoubleOrNull() ?: 0.0
                val rate = second.text.toString().toDoubleOrNull() ?: 18.0
                calculatorResult =
                    if (rate <= 0) amount else amount * 100.0 / (100.0 + rate)
                resultText.text = "Before GST: ₹${format(calculatorResult)}"
            },
            LinearLayout.LayoutParams(0, dp(46), 1f)
        )
        gst.addView(gstRow2)
        page.addView(gst)
    }

    private fun numberInput(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            setHintTextColor(muted)
            setTextColor(white)
            inputType = 8194
            setSingleLine(true)
            setPadding(dp(10), 0, dp(10), 0)
            setBackgroundColor(Color.rgb(35, 50, 70))
        }
    }

    private fun watcher(action: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                action()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
    }

    private fun setCount(denomination: Int, value: Int) {
        val safeValue = value.coerceAtLeast(0)
        counts[denomination] = safeValue
        val field = inputs[denomination]
        if (field != null && field.text.toString() != safeValue.toString()) {
            field.setText(safeValue.toString())
        }
        refresh()
    }

    private fun denominationTotal(): Long =
        denominations.sumOf {
            it.toLong() * (counts[it] ?: 0).toLong()
        }

    private fun grandTotal(): Double =
        denominationTotal().toDouble() + extra

    private fun refresh() {
        if (!::totalText.isInitialized) return

        denominations.forEach { denomination ->
            val quantity = counts[denomination] ?: 0
            amountLabels[denomination]?.text =
                "₹${format(denomination.toLong() * quantity)}"
        }

        val total = denominationTotal()
        val notes = denominations.sumOf { counts[it] ?: 0 }

        totalText.text = "₹${format(total)}"
        notesText.text = "Total Notes: $notes"

        if (::comparisonText.isInitialized) {
            comparisonText.text = when {
                total.toDouble() > extra -> "COMPARISON: GREATER THAN"
                total.toDouble() < extra -> "COMPARISON: LESS THAN"
                else -> "COMPARISON: EQUAL TO"
            }
        }

        if (::extraTotalText.isInitialized) {
            extraTotalText.text = "Grand Total: ₹${format(grandTotal())}"
        }
    }

    private fun format(value: Number): String =
        String.format(Locale.US, "%,.2f", value.toDouble())
            .removeSuffix(".00")

    private fun loadCounts() {
        denominations.forEach {
            counts[it] = prefs.getInt("count_$it", 0)
        }
        extra = prefs.getFloat("extra", 0f).toDouble()
    }

    private fun saveCount() {
        val total = denominationTotal()
        denominations.forEach {
            prefs.edit().putInt("count_$it", counts[it] ?: 0).apply()
        }
        prefs.edit().putFloat("extra", extra.toFloat()).apply()

        val history = JSONArray(prefs.getString("history", "[]"))
        val entry = JSONObject()
        entry.put("date", SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()))
        entry.put("total", total)
        entry.put("extra", extra)
        entry.put("grand", grandTotal())

        val detail = JSONObject()
        denominations.forEach { detail.put(it.toString(), counts[it] ?: 0) }
        entry.put("counts", detail)

        history.put(entry)
        prefs.edit().putString("history", history.toString()).apply()
        Toast.makeText(this, "Count saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showHistory() {
        val history = JSONArray(prefs.getString("history", "[]"))
        if (history.length() == 0) {
            AlertDialog.Builder(this)
                .setMessage("No saved entries yet.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val lines = StringBuilder()
        for (i in history.length() - 1 downTo 0) {
            val item = history.getJSONObject(i)
            lines.append(item.optString("date")).append("\n")
            lines.append("Cash: ₹").append(format(item.optDouble("total"))).append("\n")
            lines.append("Extra: ₹").append(format(item.optDouble("extra"))).append("\n")
            lines.append("Grand: ₹").append(format(item.optDouble("grand"))).append("\n\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Saved History")
            .setMessage(lines.toString())
            .setPositiveButton("Close", null)
            .setNeutralButton("Clear History") { _, _ ->
                prefs.edit().putString("history", "[]").apply()
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, 101)
    }

    @Deprecated("Deprecated Android callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val image = InputImage.fromFilePath(this, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        parseScan(result.text)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Could not read image", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open image", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun parseScan(text: String) {
        val found = mutableMapOf<Int, Int>()
        val pattern = Regex("""(500|200|100|50|20|10|5|2|1)\s*(?:[-xX×:]\s*|\s+)(\d{1,6})""")

        text.lines().forEach { line ->
            val match = pattern.find(line)
            if (match != null) {
                val denomination = match.groupValues[1].toInt()
                val quantity = match.groupValues[2].toIntOrNull() ?: 0
                if (denomination in denominations) {
                    found[denomination] = quantity
                }
            }
        }

        if (found.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No denominations found")
                .setMessage("Try a clearer photo. Supported examples: 500 - 10 or 500 x 10.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val preview = found.entries.joinToString("\n") {
            "₹${it.key}  ×  ${it.value}"
        }

        AlertDialog.Builder(this)
            .setTitle("Recognized denominations")
            .setMessage(preview + "\n\nApply these quantities?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply") { _, _ ->
                found.forEach { (denomination, quantity) ->
                    setCount(denomination, quantity)
                }
                Toast.makeText(this, "Scanner values applied", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
