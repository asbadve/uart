package com.ajinkyabadve.uartcommunication;

/**
 * Created by asbad on 07-12-2017.
 */

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;

public class UARTHelper extends UartDeviceCallback {

    private static final String TAG = UARTHelper.class.getSimpleName();
    private static final String UART_NAME = "UART0";
    private static final int BAUD_RATE = 115200;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = 1;
    StringBuilder stringBuilder = new StringBuilder();
    private static final int CHUNK_SIZE = 1024;

    interface KeyReceivedListener {
        void onStringReceived(String data);
    }

    private HandlerThread inputThread;
    private UartDevice uartDevice;
    private KeyReceivedListener listener;

    public void init(KeyReceivedListener listener) {
        this.listener = listener;

        inputThread = new HandlerThread("UARTThread");
        inputThread.start();

        try {
            uartDevice = new PeripheralManagerService().openUartDevice(UART_NAME);
            uartDevice.setBaudrate(BAUD_RATE);
            uartDevice.setDataSize(DATA_BITS);
            uartDevice.setParity(UartDevice.PARITY_NONE);
            uartDevice.setStopBits(STOP_BITS);
            uartDevice.registerUartDeviceCallback(this, new Handler(inputThread.getLooper()));

            String ready = "Ready to scan\r\n";
            uartDevice.write(ready.getBytes(), ready.length());
        } catch (IOException e) {
            Log.e(TAG, "Unable to open UART device", e);
        }
    }

    public void close() {
        listener = null;

        try {
            uartDevice.unregisterUartDeviceCallback(this);
            uartDevice.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing UART device:", e);
        }

        inputThread.quitSafely();
    }

    @Override
    public boolean onUartDeviceDataAvailable(UartDevice uart) {
        try {
            byte[] buffer = new byte[CHUNK_SIZE];
            while (uartDevice.read(buffer, buffer.length) > 0) {

                for (int i = 0; i < buffer.length; i++) {
                    char character = (char) buffer[i];
                    if (character != '\u0000') {
                        if (character == '`') {//its start
//                            Log.d(TAG, "onUartDeviceDataAvailable: start" + character);
                            stringBuilder = null;
                            stringBuilder = new StringBuilder();
                        }  else if (character == '!') {//end
                            Log.d(TAG, "onUartDeviceDataAvailable: value " + stringBuilder.toString());
                            listener.onStringReceived(stringBuilder.toString());
                        } else {
//                            Log.d(TAG, "onUartDeviceDataAvailable: actual value" + character);
                            stringBuilder.append(character);
                        }
                    }
//                    Log.d(TAG, "onUartDeviceDataAvailable() called with: uart = [" + buffer[i] + "]");
                }
//                String string = new String(buffer);
//                Log.d(TAG, "onUartDeviceDataAvailable: " + stringBuilder.toString());
//                int key = (int) buffer[0];

            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to transfer data over UART", e);
        }
        return true;
    }

    @Override
    public void onUartDeviceError(UartDevice uart, int error) {
        Log.w(TAG, uart + ": Error event " + error);
    }
}