package com.example.controlremotohc05

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
        lateinit var direccionDispositivo: String
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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
                } else {
                    launcherResultado.launch(enableBtIntent)
                }
            }
        }

        binding.desactivar.setOnClickListener {
            if (!adaptadorBluetooth.isEnabled) {
                Toast.makeText(this, "Bluetooth ya se encuentra desactivado", Toast.LENGTH_SHORT).show()
            } else {
                adaptadorBluetooth.disable()
                Toast.makeText(this, "Se ha desactivado el Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }

        binding.botonblue.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            } else {
                listarDispositivosEmparejados()
            }
        }

        binding.up.setOnClickListener { enviarComando("A") }
        binding.stop.setOnClickListener { enviarComando("P") }
        binding.down.setOnClickListener { enviarComando("B") }
        binding.left.setOnClickListener { enviarComando("I") }
        binding.right.setOnClickListener { enviarComando("D") }
    }

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
//ffff