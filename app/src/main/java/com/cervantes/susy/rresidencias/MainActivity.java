package com.cervantes.susy.rresidencias;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnConectarArduino;
    private TextView textView;
    private Boolean found;
    private RelativeLayout positionOfPopUp;
    private BluetoothAdapter bluetoothAdapter;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String MAC_HC05 = "20:16:01:25:65:23"; //DIRECCION MAC DEL HC-05
    private BluetoothDevice deviceHC05;
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothSocket socket;
    byte buffer[];
    Boolean stopThread;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(getApplicationContext(),"El bluetooth se desactivo",Toast.LENGTH_SHORT).show();
                        solicitarActivacionBluetooth();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);

        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {             //CHECA SI EXISTE UN ADAPTADOR BLUETOOTH EN EL DISPOSITIVO
            Toast.makeText(getApplicationContext(),"Este dispositivo no soporta Bluetooth",Toast.LENGTH_SHORT).show();
            finishAndRemoveTask ();
        }

        positionOfPopUp = (RelativeLayout) findViewById(R.id.popUpPosition);
        btnConectarArduino = (Button) findViewById(R.id.btnConectarArduino);
    }

    @Override
    protected void onStart(){
        super.onStart();

        //REGISTRO PARA LA EMISION DE LOS CAMBIOS EN EL ADAPTADOR DEL BLUETOOTH
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume(){
        super.onResume();

        //SI EL BLUETOOTH ESTA APAGADO PEDIR PERMISO PARA PRENDERLO
        if(!bluetoothAdapter.isEnabled()){
            solicitarActivacionBluetooth();
        }

        //VINCULACION CON EL MODULO HC05
        btnConectarArduino.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bluetoothAdapter.isEnabled()){
                    if(BTinit()){
                        if(BTconectar()){
                            beginListenForData();
                        }
                    }
                }
                /*if(bluetoothAdapter.isEnabled()){ //SI ESTA PRENDIDO
                    //SE OBTIENE UNA LISTA DE LOS DISPOSITIVOS BLUETOOTH QUE HAN SIDO EMPAREJADOS CON EL CELULAR
                    Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                    if(bondedDevices.isEmpty()) {       //SI LA LISTA ESTA VACIA
                        Toast.makeText(getApplicationContext(),"Porfavor emparaje el dispositivo primero",Toast.LENGTH_SHORT).show();
                    } else {                            //SI NO ESTA VACIA
                        for (BluetoothDevice iterator : bondedDevices) {
                            //SE RECORRE LA LISTA DE DISPOSOTIVOS Y SE COMPARA CON LA MAC DEL HC-05
                            if(iterator.getAddress().equals(MAC_HC05)) //Replace with iterator.getName() if comparing Device names.
                            {                           //EL DISPOSITIVO ENCONTRADO SE LE ASIGNA A deviceHC05
                                Toast.makeText(getApplicationContext(),"Conexion exitosa",Toast.LENGTH_SHORT).show();
                                deviceHC05=iterator;    //device is an object of type BluetoothDevice
                                found=true;
                                break;
                            }else{
                                Toast.makeText(getApplicationContext(),"Conexion fallida",Toast.LENGTH_SHORT).show();
                            }
                        } }
                }*/
            }
        });

    }

    @Override
    protected void onStop(){
        super.onStop();

        //Quitar el registro del adaptador bluetooth
        unregisterReceiver(mReceiver);
    }

    //RESULTADO DE LA ACTIVIDAD SOLICITUD DE ACTIVACION DE BLUETOOTH
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent ACTION_STATE_CHANGED){
        if(requestCode == 0){
            if(resultCode == RESULT_OK){
                Toast.makeText(getApplicationContext(),"Bluetooth activado con exito",Toast.LENGTH_SHORT).show();
            }else if(resultCode == RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),"Es necesario activar el bluetooth",Toast.LENGTH_SHORT).show();
                finishAndRemoveTask ();
            }
        }
    }

    private void solicitarActivacionBluetooth(){
        Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableAdapter, 0);
    }

    public boolean BTinit()
    {
        boolean found=false;
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Debes de emparejar el dispositivo primero",Toast.LENGTH_SHORT).show();
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(MAC_HC05))
                {
                    deviceHC05=iterator;
                    found=true;
                    break;
                }
            }
        }
        return found;
    }

    public boolean BTconectar()
    {
        boolean connected;
        try {
            socket = deviceHC05.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
            connected = true;
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            Toast.makeText(getApplicationContext(),"Conexión exitosa",Toast.LENGTH_SHORT).show();
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return connected;
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string=new String(rawBytes,"UTF-8");
                            handler.post(new Runnable() {
                                public void run()
                                {
                                    textView.setText(string);
                                }
                            });
                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }
}
