package io.github.mobdev

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView

    private var currentInput = StringBuilder()
    private var firstOperand = 0.0
    private var operator = ""
    private var waitingForSecond = false
    private var justCalculated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)

        val buttonIds = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9", R.id.btnDot to "."
        )

        for ((id, value) in buttonIds) {
            findViewById<Button>(id).setOnClickListener { onDigit(value) }
        }

        findViewById<Button>(R.id.btnPlus).setOnClickListener { onOperator("+") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { onOperator("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperator("×") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperator("÷") }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEquals() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { onClear() }
        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener { onPlusMinus() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { onPercent() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput.toString())
        outState.putDouble("firstOperand", firstOperand)
        outState.putString("operator", operator)
        outState.putBoolean("waitingForSecond", waitingForSecond)
        outState.putBoolean("justCalculated", justCalculated)
        outState.putString("displayText", tvDisplay.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentInput = StringBuilder(savedInstanceState.getString("currentInput", ""))
        firstOperand = savedInstanceState.getDouble("firstOperand", 0.0)
        operator = savedInstanceState.getString("operator", "")
        waitingForSecond = savedInstanceState.getBoolean("waitingForSecond", false)
        justCalculated = savedInstanceState.getBoolean("justCalculated", false)
        tvDisplay.text = savedInstanceState.getString("displayText", "0")
    }

    private fun onDigit(digit: String) {
        if (justCalculated) {
            currentInput.clear()
            justCalculated = false
        }
        if (waitingForSecond) {
            currentInput.clear()
            waitingForSecond = false
        }
        if (digit == "." && currentInput.contains(".")) return
        if (digit == "." && currentInput.isEmpty()) currentInput.append("0")
        currentInput.append(digit)
        tvDisplay.text = currentInput.toString()
    }

    private fun onOperator(op: String) {
        if (currentInput.isNotEmpty()) {
            if (operator.isNotEmpty() && !waitingForSecond) {
                calculate()
            } else {
                firstOperand = currentInput.toString().toDouble()
            }
        }
        operator = op
        waitingForSecond = true
        justCalculated = false
    }

    private fun onEquals() {
        if (operator.isEmpty() || waitingForSecond) return
        calculate()
        operator = ""
        justCalculated = true
    }

    private fun calculate() {
        val second = currentInput.toString().toDoubleOrNull() ?: return
        val result = when (operator) {
            "+" -> firstOperand + second
            "-" -> firstOperand - second
            "×" -> firstOperand * second
            "÷" -> if (second != 0.0) firstOperand / second else Double.NaN
            else -> return
        }
        val display = formatResult(result)
        tvDisplay.text = display
        currentInput = StringBuilder(display)
        firstOperand = result
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "Ошибка"
        return if (value == kotlin.math.floor(value) && !value.isInfinite()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun onClear() {
        currentInput.clear()
        firstOperand = 0.0
        operator = ""
        waitingForSecond = false
        justCalculated = false
        tvDisplay.text = "0"
    }

    private fun onPlusMinus() {
        val value = currentInput.toString().toDoubleOrNull() ?: return
        val result = formatResult(-value)
        currentInput = StringBuilder(result)
        tvDisplay.text = result
    }

    private fun onPercent() {
        val value = currentInput.toString().toDoubleOrNull() ?: return
        val result = formatResult(value / 100.0)
        currentInput = StringBuilder(result)
        tvDisplay.text = result
    }
}
