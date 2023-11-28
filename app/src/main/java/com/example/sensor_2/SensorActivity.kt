package com.example.sensor_2


import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.File
import java.io.FileOutputStream
import java.time.LocalTime
import java.util.*


class SensorActivity : AppCompatActivity(), SensorEventListener {

    private var plotData: Boolean = true
    private var writeData: Boolean = false
    private lateinit var sensorManager: SensorManager
    private lateinit var square: TextView
    private var seriesX: LineGraphSeries<DataPoint>? = null
    private var seriesY: LineGraphSeries<DataPoint>? = null
    private var seriesZ: LineGraphSeries<DataPoint>? = null
    private var lastValues: ArrayList<Vector<Double>>? = null
    private var lastTimes: ArrayList<String>? = null

    private var hiloGrafica: Thread? = null
    private var hiloEscribir: Thread? = null
    private var isRunning: Boolean = false

    private var currentX = 0.0

    private val READING = 50 // milliseconds
    private val WRITING = 60000 // milliseconds

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Elementos de la UI
        square = findViewById(R.id.square)
        val graph = findViewById<View>(R.id.graph) as GraphView

        // Sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }

        // Lineas del gráfico
        seriesX = LineGraphSeries<DataPoint>()
        seriesY = LineGraphSeries<DataPoint>()
        seriesZ = LineGraphSeries<DataPoint>()

        seriesX?.color = Color.GREEN
        seriesY?.color = Color.BLUE
        seriesZ?.color = Color.RED

        graph.addSeries(seriesX)
        graph.addSeries(seriesY)
        graph.addSeries(seriesZ)

        // activate horizontal zooming and scrolling
        graph.viewport.isScalable = true
        // activate horizontal scrolling
        graph.viewport.isScrollable = true
        // activate horizontal and vertical zooming and scrolling
        graph.viewport.setScalableY(true)
        // activate vertical scrolling
        graph.viewport.setScrollableY(true)
        // To set a fixed manual viewport use this:
        // set manual X bounds
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.setMinX(0.5)
        graph.viewport.setMaxX(6.5)
        // set manual Y bounds
        graph.viewport.isYAxisBoundsManual = true
        graph.viewport.setMinY(0.0)
        graph.viewport.setMaxY(10.0)
        graph.gridLabelRenderer.isHorizontalLabelsVisible = false
        graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.NONE

        // Initialization
        currentX = 0.0
        lastValues = ArrayList()
        lastTimes = ArrayList()

        // Hilo que cada 30 milisegundos levantará flag para actualizar gráfico
        isRunning = true
        hiloGrafica = Thread {
            var times = 0
            while (isRunning) {
                plotData = true
                Thread.sleep(READING.toLong())
                if (times > WRITING / READING) {
                    times = 0
                    writeData = true
                }
                times++
            }
        }
        hiloGrafica?.start()

        hiloEscribir = Thread {
            var fileName = ""
            val path = "lectura"
            var lectura = 0
            while (isRunning) {
                if (writeData && lastValues != null && lastTimes != null) {
                    val lastValuesCopy = lastValues
                    val lastTimesCopy = lastTimes
                    lastValues = ArrayList()
                    lastTimes = ArrayList()
                    fileName = "sensor_2_lectura_$lectura.txt"

                    val myFile = File(applicationContext.getExternalFilesDir(path), fileName)
                    if (myFile.exists()) {
                        myFile.delete()
                    }

                    val myfile = File(applicationContext.getExternalFilesDir(path), fileName)
                    FileOutputStream(myfile, true).bufferedWriter().use { writer ->
                        for ((iter, value: Vector<Double>) in lastValuesCopy!!.withIndex()) {
                            writer.appendLine("\"${lastTimesCopy!![iter]}\", ${value[0]}, ${value[1]}, ${value[2]}")
                        }
                    }
                    runOnUiThread {
                        Toast.makeText(this, "archivo n° $lectura escrito :)", Toast.LENGTH_SHORT).show()
                    }
                    lectura++
                    writeData = false
                }
            }
        }
        hiloEscribir?.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAccelerometer(event: SensorEvent) {
        val values = event.values
        // Movement
        val x = values[0].toDouble()
        val y = values[1].toDouble()
        val z = values[2].toDouble()
        seriesX?.appendData(DataPoint(currentX, x), true, 100)
        seriesY?.appendData(DataPoint(currentX, y), true, 100)
        seriesZ?.appendData(DataPoint(currentX, z), true, 100)
        val vector = Vector<Double>(3)
        vector.addElement(x)
        vector.addElement(y)
        vector.addElement(z)
        lastValues?.add(vector)
        lastTimes?.add(LocalTime.now().toString())

        currentX += 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                if (plotData) {
                    getAccelerometer(event)
                    plotData = false
                }
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not implemented
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            hiloEscribir?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        sensorManager.unregisterListener(this)
    }
}



//    inner class AccelerationChartHandler : Handler() {
//        override fun handleMessage(msg: Message) {
//            var accelerationY = 0.0
//            if (!msg.data.getString("ACCELERATION_VALUE").equals(null) && !msg.data
//                    .getString("ACCELERATION_VALUE").equals("null")
//            ) {
//                accelerationY = msg.data.getString("ACCELERATION_VALUE")!!.toDouble()
//            }
//            series?.appendData(DataPoint(currentX, accelerationY), true, 10)
//            currentX += 1
//        }
//    }
//
//    inner class AccelerationChart(private val handler: Handler) : Runnable {
//        private val drawChart = true
//        override fun run() {
//            while (drawChart) {
//                val accelerationY: Double = try {
//                    Thread.sleep(300) // Speed up the X axis
//                    accelerationQueue.poll()!!
//                } catch (e: InterruptedException) {
//                    e.printStackTrace()
//                    continue
//                }catch (e: NullPointerException) {
//                    continue
//                }
//
//                // currentX value will be excced the limit of double type range
//                // To overcome this problem comment of this line
//                // currentX = (System.currentTimeMillis() / 1000) * 8 + 0.6;
//                val msgObj = handler.obtainMessage()
//                val b = Bundle()
//                b.putString("ACCELERATION_VALUE", accelerationY.toString())
//                msgObj.data = b
//                handler.sendMessage(msgObj)
//            }
//        }
//    }



