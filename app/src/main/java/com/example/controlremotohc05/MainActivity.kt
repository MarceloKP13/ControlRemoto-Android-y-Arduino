package com.example.controlremotohc05

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
        const val SOLICITAR_ACTIVAR_BT = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adaptadorDirecciones = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        adaptadorNombres = ArrayAdapter(this, android.R.layout.simple_list_item_1)

        val launcherResultado = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->
            if (resultado.resultCode == SOLICITAR_ACTIVAR_BT) {
                Log.i("MainActivity", "ACTIVIDAD REGISTRADA")
            }
        }

        adaptadorBluetooth = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if (adaptadorBluetooth == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth está disponible en este dispositivo", Toast.LENGTH_SHORT).show()
        }

        binding.activar.setOnClickListener {
            if (adaptadorBluetooth.isEnabled) {
                Toast.makeText(this, "Bluetooth ya se encuentra activado", Toast.LENGTH_SHORT).show()
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (verificarPermisosBluetooth()) {
                    launcherResultado.launch(enableBtIntent)
                } else {
                    solicitarPermisosBluetooth()
                }
            }
        }

        binding.botonblue.setOnClickListener {
            if (verificarPermisosBluetooth()) {
                listarDispositivosEmparejados()
            } else {
                solicitarPermisosBluetooth()
            }
        }

        binding.conectar.setOnClickListener {
            val selectedDeviceAddress = binding.spinDispositivos.selectedItem.toString()
            if (selectedDeviceAddress.isNotEmpty()) {
                val dispositivo = adaptadorBluetooth.getRemoteDevice(selectedDeviceAddress)
                conectarDispositivo(dispositivo)
            } else {
                Toast.makeText(this, "Seleccione un dispositivo", Toast.LENGTH_SHORT).show()
            }
        }

        binding.up.setOnClickListener { enviarComando("A") }
        binding.stop.setOnClickListener { enviarComando("P") }
        binding.down.setOnClickListener { enviarComando("B") }
        binding.left.setOnClickListener { enviarComando("I") }
        binding.right.setOnClickListener { enviarComando("D") }
    }

    @SuppressLint("MissingPermission")
    private fun listarDispositivosEmparejados() {
        val dispositivosEmparejados: Set<BluetoothDevice>? = adaptadorBluetooth.bondedDevices
        adaptadorDirecciones?.clear()
        adaptadorNombres?.clear()

        dispositivosEmparejados?.forEach { dispositivo ->
            val nombreDispositivo = dispositivo.name
            val direccionDispositivo = dispositivo.address // MAC address
            adaptadorDirecciones?.add(direccionDispositivo)
            adaptadorNombres?.add(nombreDispositivo)
        }

        findViewById<Spinner>(R.id.spinDispositivos).adapter = adaptadorNombres
    }

    private fun conectarDispositivo(dispositivo: BluetoothDevice) {
        if (verificarPermisosBluetooth()) {
            try {
                socketBluetooth = dispositivo.createRfcommSocketToServiceRecord(UUID_SERVICIO)
                socketBluetooth?.connect()
                conectado = true
                Toast.makeText(this, "Conectado a ${dispositivo.name}", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al conectar con ${dispositivo.name}: ${e.message}", Toast.LENGTH_LONG).show()
                try {
                    socketBluetooth?.close()
                } catch (closeException: IOException) {
                    closeException.printStackTrace()
                }
            }
        } else {
            solicitarPermisosBluetooth()
            Toast.makeText(this, "Permiso de Bluetooth no concedido", Toast.LENGTH_SHORT).show()
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

    private fun verificarPermisosBluetooth(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisosBluetooth() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

