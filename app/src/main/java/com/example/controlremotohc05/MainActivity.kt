package com.example.controlremotohc05

import android.Manifest
import android.R
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.controlremotohc05.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var adaptadorBluetooth: BluetoothAdapter
    var adaptadorDirecciones: ArrayAdapter<String>? = null
    var adaptadorNombres: ArrayAdapter<String>? = null

    companion object {
        var UUID_SERVICIO: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private var socketBluetooth: BluetoothSocket? = null
        var conectado: Boolean = false
        lateinit var direccionDispositivo: String
        const val SOLICITAR_ACTIVAR_BT = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lateinit var binding: ActivityMainBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adaptadorDirecciones = ArrayAdapter(this, R.layout.simple_list_item_1)
        adaptadorNombres = ArrayAdapter(this, R.layout.simple_list_item_1)


        val launcherResultado = registerForActivityResult(
            StartActivityForResult()
        ) { resultado ->
            if (resultado.resultCode == SOLICITAR_ACTIVAR_BT) {
                Log.i("MainActivity", "ACTIVIDAD REGISTRADA")
            }
        }

        adaptadorBluetooth = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if (adaptadorBluetooth == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Bluetooth está disponible en este dispositivo", Toast.LENGTH_LONG).show()
        }

        binding.activar.setOnClickListener {
            if (adaptadorBluetooth.isEnabled) {

                Toast.makeText(this, "Bluetooth ya se encuentra activado", Toast.LENGTH_LONG).show()
            } else {

                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i("MainActivity", "ActivityCompat#requestPermissions")
                }
                launcherResultado.launch(enableBtIntent)
            }
        }

        //Boton apagar bluetooth
        binding.desactivar.setOnClickListener {
            if (!adaptadorBluetooth.isEnabled) {

                Toast.makeText(this, "Bluetooth ya se encuentra desactivado", Toast.LENGTH_LONG).show()
            } else {

                adaptadorBluetooth.disable()
                Toast.makeText(this, "Se ha desactivado el bluetooth", Toast.LENGTH_LONG).show()
            }
        }

        binding.botonblue.setOnClickListener {
            if (adaptadorBluetooth.isEnabled) {
                Toast.makeText(this, "Bluetooth ya se encuentra activado", Toast.LENGTH_LONG).show()
            } else {
                val intentActivarBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i("MainActivity", "ActivityCompat#requestPermissions")
                }
                launcherResultado.launch(intentActivarBluetooth)
            }
        }

        binding.botonblue.setOnClickListener {
            if (!adaptadorBluetooth.isEnabled) {
                Toast.makeText(this, "Bluetooth ya se encuentra desactivado", Toast.LENGTH_LONG).show()
            } else {
                adaptadorBluetooth.disable()
                Toast.makeText(this, "Se ha desactivado el Bluetooth", Toast.LENGTH_LONG).show()
            }
        }

        binding.botonblue.setOnClickListener {
            if (adaptadorBluetooth.isEnabled) {
                val dispositivosEmparejados: Set<BluetoothDevice>? = adaptadorBluetooth.bondedDevices
                adaptadorDirecciones!!.clear()
                adaptadorNombres!!.clear()

                dispositivosEmparejados?.forEach { dispositivo ->
                    val nombreDispositivo = dispositivo.name
                    val direccionDispositivo = dispositivo.address // MAC address
                    adaptadorDirecciones!!.add(direccionDispositivo)
                    adaptadorNombres!!.add(nombreDispositivo)
                }

                binding.spinDispositivos.adapter = adaptadorNombres
            } else {
                val sinDispositivos = "Ningún dispositivo pudo ser emparejado"
                adaptadorDirecciones!!.add(sinDispositivos)
                adaptadorNombres!!.add(sinDispositivos)
                Toast.makeText(this, "Primero vincule un dispositivo Bluetooth", Toast.LENGTH_LONG).show()
            }
        }

        binding.botonblue.setOnClickListener {
            try {
                if (socketBluetooth == null || !conectado) {
                    val posicionSpin = binding.spinDispositivos.selectedItemPosition
                    direccionDispositivo = adaptadorDirecciones!!.getItem(posicionSpin).toString()
                    Toast.makeText(this, direccionDispositivo, Toast.LENGTH_LONG).show()
                    adaptadorBluetooth.cancelDiscovery()
                    val dispositivo: BluetoothDevice = adaptadorBluetooth.getRemoteDevice(direccionDispositivo)
                    socketBluetooth = dispositivo.createInsecureRfcommSocketToServiceRecord(UUID_SERVICIO)
                    socketBluetooth!!.connect()
                }

                Toast.makeText(this, "CONEXIÓN EXITOSA", Toast.LENGTH_LONG).show()
                Log.i("MainActivity", "CONEXIÓN EXITOSA")

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "ERROR DE CONEXIÓN", Toast.LENGTH_LONG).show()
                Log.i("MainActivity", "ERROR DE CONEXIÓN")
            }
        }

        binding.up.setOnClickListener {
            enviarComando("A")
        }

        binding.stop.setOnClickListener {
            enviarComando("P")
        }

        binding.down.setOnClickListener {
            enviarComando("B")
        }

        binding.left.setOnClickListener {
            enviarComando("I")
        }

        binding.right.setOnClickListener {
            enviarComando("D")
        }

    }

    private fun enviarComando(comando: String) {
        if (socketBluetooth != null) {
            try {
                socketBluetooth!!.outputStream.write(comando.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al enviar el comando", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No hay conexión Bluetooth establecida", Toast.LENGTH_SHORT).show()
        }
    }
}
