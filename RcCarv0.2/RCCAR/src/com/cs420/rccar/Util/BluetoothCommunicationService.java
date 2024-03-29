package com.cs420.rccar.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;

/**
 * A service which handles all states of a Bluetooth connection
 * between two paired Android devices
 * Used with Permission from the Android Developer Website
 *
 * @author David Widen
 * @author Jessie Young
 * @author Thomas Cheng
 *
 */
public class BluetoothCommunicationService
{
	private static UUID MY_UUID = UUID.fromString("437ee550-fde6-11e0-be50-0800200c9a66");
	private static String AppName = "RcCar";
	
	private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    /**
     * Default constructor to create a new BluetoothCommunicationService
     * @param context the context of the activity that called this method
     * @param handler the handler which this service will report to
     */
    public BluetoothCommunicationService(Context context, Handler handler)
    {
    	mAdapter = BluetoothAdapter.getDefaultAdapter();
    	mState = STATE_NONE;
    	mHandler = handler;
    }
    
    /**
     * Set the current state of the connection
     * @param state the current state of the connection
     */
    private synchronized void setState(int state)
    {
    	mState = state;
    }
    
    /**
     * Get the current state of the connection
     * @return the state of the connection
     */
    public synchronized int getState()
    {
    	return mState;
    }
    /**
     * Inherited from Thread, starts the BluetoothCommunicationService
     */
    public synchronized void start()
    {
    	if (mConnectThread != null)
    	{
    		mConnectThread.cancel();
    		mConnectThread = null;
    	}
    	
    	if (mConnectedThread != null)
    	{
    		mConnectedThread.cancel();
    		mConnectedThread = null;
    	}
    	
    	setState(STATE_LISTEN);
    	
    	if (mAcceptThread == null)
    	{
    		mAcceptThread = new AcceptThread();
    		mAcceptThread.start();
    	}
    }
    
    /**
     * This device is connected to the remote device via BluetoothSocket
     * @param socket the socket which the devices use to communication
     * @param device the remove device this device is in communication with
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
    {
    	if (mConnectThread != null)
    	{
    		mConnectThread.cancel();
    		mConnectThread = null;
    	}
    	
    	if (mConnectedThread != null)
    	{
    		mConnectedThread.cancel();
    		mConnectThread = null;
    	}
    	
    	if (mAcceptThread != null)
    	{
    		mAcceptThread.cancel();
    		mAcceptThread = null;
    	}
    	
    	mConnectedThread = new ConnectedThread(socket);
    	mConnectedThread.start();
    	
    	//Message handler stuff here
    	
    	setState(STATE_CONNECTED);
    }
    
    /**
     * The connection has failed, and will attempt to restart itself
     */
    private void connectionFailed()
    {
    	//Message handler, send message back to activity
    	
    	//Start service over!
    	BluetoothCommunicationService.this.start();
    }
    
    /**
     * The connection has been lost, and will attempt to restart itself
     */
    private void connectionLost()
    {
    	//Message handler, send message back to activity
    	
    	//Start service over!
    	BluetoothCommunicationService.this.start();
    }
    
    /**
     * Connect this device to an external Bluetooth device
     * @param device the device to connect this Bluetooth device to
     */
    public synchronized void connect(BluetoothDevice device) 
    {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING)
        {
            if (mConnectThread != null) 
            {
            	mConnectThread.cancel(); 
            	mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) 
        {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    
    /**
     * This class encapsulates a BluetoothServerSocket to use for communication
     * with an external Bluetooth device
     * Used with permission, originally developed on the Android Developer website
     * 
     * @author David
     *
     */
    private class AcceptThread extends Thread
    {
    	private final BluetoothServerSocket mServerSocket;
    	
    	public AcceptThread()
    	{
    		BluetoothServerSocket temp = null;
    		
    		try
    		{
    			temp = mAdapter.listenUsingRfcommWithServiceRecord(AppName, MY_UUID);
    		}
    		catch (IOException e)
    		{
    			//Add processing here for null value
    		}
    		
    		mServerSocket = temp;
    	}
    	
    	/**
    	 * Inherited from the Thread class, this starts the thread
    	 */
    	public void run()
    	{
    		BluetoothSocket socket = null;
    		
    		while (mState != STATE_CONNECTED)
    		{
    			try
    			{
    				socket = mServerSocket.accept();
    			}
    			catch (IOException e)
    			{
    				break;
    			}
    		}
    		
    		if (socket != null)
    		{
    			synchronized (BluetoothCommunicationService.this)
    			{
    				switch (mState)
    				{
    					case STATE_LISTEN:
    					case STATE_CONNECTING:
    						connected(socket, socket.getRemoteDevice());
    						break;
    					case STATE_NONE:
    					case STATE_CONNECTED:
    						try
    						{
    							socket.close();
    						}
    						catch (IOException e)
    						{
    							//add processing for not-closing
    						}
    						break;
    				}
    			}
    		}
    	}
    	
    	/**
    	 * Cancels the thread, and closes the socket
    	 */
    	public void cancel()
    	{
    		try
    		{
    			mServerSocket.close();
    		}
    		catch (IOException e)
    		{
    			//add processing for not-closing
    		}
    	}
    }
    
    /**
     * Encapsulates a BluetoothSocket and an external BluetoothDevice so they can be
     * connected to this Android device
     * Used with permission, originally developed on the Android Developer website
     * 
     * @author David
     *
     */
    private class ConnectThread extends Thread
    {
    	private final BluetoothSocket mSocket;
    	private final BluetoothDevice mDevice;
    	
    	/**
    	 * Default constructor, initializes the BluetoothSocket
    	 * @param device the BluetoothDevice to connect to
    	 */
    	public ConnectThread(BluetoothDevice device)
    	{
    		mDevice = device;
    		BluetoothSocket temp = null;
    		
    		try
    		{
    			temp = device.createRfcommSocketToServiceRecord(MY_UUID);
    		}
    		catch (IOException e)
    		{
    			//add processing for null temp
    		}
    		
    		mSocket = temp;
    	}
    	
    	/**
    	 * Inherited from Thread, starts the ConnectThread
    	 */
    	public void run()
    	{
    		mAdapter.cancelDiscovery();
    		
    		try
    		{
    			mSocket.connect();
    		}
    		catch (IOException e)
    		{
    			try
    			{
    				mSocket.close();
    			}
    			catch (IOException e1)
    			{
    				//add processing for failure to close
    			}
    			connectionFailed();
    			return;
    		}
    		
    		synchronized (BluetoothCommunicationService.this)
    		{
    			mConnectThread = null;
    		}
    		
    		connected(mSocket, mDevice);
    	}
    	
    	/**
    	 * Closes the socket, and stops the thread
    	 */
    	public void cancel()
    	{
    		try
    		{
    			mSocket.close();
    		}
    		catch (IOException e)
    		{
    			//add processing for failure to close
    		}
    	}
    }
    
    /**
     * Cancel all running threads
     */
    public synchronized void stop() 
    {

        if (mConnectThread != null) 
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) 
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) 
        {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_NONE);
    }
    
    /**
     * Write data to the external BluetoothDevice
     * @param out the byte[] to send via Bluetooth
     */
    public void write(byte[] out) 
    {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) 
        {
            if (mState != STATE_CONNECTED) 
            	return;
            
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    
    /**
     * Encapsulates a BluetoothSocket for remote communication to an external Bluetooth device
     * Allows for direct communication between the two devices via Bluetooth
     * Used with permission, originally developed on the Android Developer website
     * 
     * @author David
     *
     */
    private class ConnectedThread extends Thread
    {
    	private final BluetoothSocket mSocket;
    	private final InputStream mInputStream;
    	private final OutputStream mOutputStream;
    	
    	/**
    	 * Default Constructor for a ConnectedThread
    	 * @param socket the BluetoothSocket to communicate with
    	 */
    	public ConnectedThread(BluetoothSocket socket)
    	{
    		mSocket = socket;
    		InputStream tempIn = null;
    		OutputStream tempOut = null;
    		
    		try
    		{
    			tempIn = socket.getInputStream();
    			tempOut = socket.getOutputStream();
    		}
    		catch (IOException e)
    		{
    			//add processing for null streams
    		}
    		
    		mInputStream = tempIn;
    		mOutputStream = tempOut;
    	}
    	
    	/**
    	 * Inherited from Thread, starts the ConnectedThread
    	 */
    	public void run()
    	{
    		byte[] buffer = new byte[1024];
    		int bytes;
    		
    		while (true)
    		{
    			try
    			{
    				bytes = mInputStream.read(buffer);
    				//handler stuff?  send to activity?
    			}
    			catch (IOException e)
    			{
    				connectionLost();
    				BluetoothCommunicationService.this.start();
    				break;
    			}
    		}
    	}
    	
    	/**
    	 * Write to the external Bluetooth device
    	 * @param buffer the byte[] to send via Bluetooth
    	 */
    	public void write(byte[] buffer)
    	{
    		try
    		{
    			mOutputStream.write(buffer);
    			//handler stuff?  send to activity?
    		}
    		catch (IOException e)
    		{
    			//handle failure to write data
    		}
    	}
    	
    	/**
    	 * Close the socket and stop the thread
    	 */
    	public void cancel()
    	{
    		try
    		{
    			mSocket.close();
    		}
    		catch (IOException e)
    		{
    			//handle failure to close socket
    		}
    	}
    }
}
