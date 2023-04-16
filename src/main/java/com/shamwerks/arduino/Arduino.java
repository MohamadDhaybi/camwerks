package com.shamwerks.arduino;

/*import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;*/

import com.fazecast.jSerialComm.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.shamwerks.camwerks.CamWerks;


public class Arduino implements SerialPortDataListener {
    @Override
    public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
    
    SerialPort serialPort = null;
    
    private boolean connected = false;
    
    private boolean dataReceived = false;
    private String  data ;
    
    private enum ArduinoCommand {
    						PING, 
    						STEP, 
    						MEASURE,
    						SET_DIRECTION
    					} 

    private String appName;
    private BufferedReader input;
    private OutputStream output;
    
    final Lock lock = new ReentrantLock();
    final Condition measureReceived  = lock.newCondition(); 
    
    private static final int TIME_OUT  = 1000; // Port open timeout
    private static final int DATA_RATE = 9600; // Arduino serial port

    public boolean initialize() {
        try {
            SerialPort [] AvailablePorts = SerialPort.getCommPorts();
		
		    System.out.println("\n\n SerialPort Data Transmission");

		    // use the for loop to print the available serial ports
		    System.out.print("\n\n Available Ports ");
            for (int i = 0; i<AvailablePorts.length ; i++)
            {
                System.out.println(i + " - " + AvailablePorts[i].getSystemPortName() + " -> " + AvailablePorts[i].getDescriptivePortName());
            }

            //Open the first Available port
            serialPort = AvailablePorts[0];

            if (serialPort.openPort())
			System.out.println(serialPort.getPortDescription() + " port opened.");
            
            if (serialPort == null) {
                System.out.println("Oops... Could not connect to Arduino");
                return false;
            }
        
            // set port parameters
            serialPort.setComPortParameters(DATA_RATE,
                                8,
                                SerialPort.ONE_STOP_BIT,
                                SerialPort.NO_PARITY);
            
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);
            
            // add event listeners
            serialPort.addDataListener(this);
            //serialPort.notifyOnDataAvailable(true);

            // Give the Arduino some time
            try { Thread.sleep(2000); } catch (InterruptedException ie) {}
            
            return true;
        }
        catch ( Exception e ) { 
            e.printStackTrace();
        }
        return false;
    }
    
    
    public double getMeasure() throws ArduinoException{
    	String arduinoReturn = sendCommand(ArduinoCommand.MEASURE.toString());
    	if(arduinoReturn==null){
    		System.out.println("ERROR!! arduinoReturn==null!!");
        	return 0.0;
    	}
    		
    	return Double.parseDouble( arduinoReturn );
    }
    
    public void doStep() throws ArduinoException{
    	String arduinoReturn = sendCommand(ArduinoCommand.STEP.toString());
    	if (!arduinoReturn.equals("STEP_ACK")) throw new ArduinoException("Incorrect Return Value!");
    }
    
    public void setDirection(int dir) throws ArduinoException{
    	String arduinoReturn = sendCommand(  ArduinoCommand.SET_DIRECTION + " " + dir  );
    	if (!arduinoReturn.equals("DIR_ACK")) throw new ArduinoException("Incorrect Return Value!");
    }
    
    public boolean ping() throws ArduinoException{
    	String arduinoReturn = sendCommand(ArduinoCommand.PING.toString());
    	if (arduinoReturn.equals("PING_ACK")) return true;
    	else return false;
    }
    
    
    private String sendCommand(String command)  throws ArduinoException {
        try {
            System.out.println("Sending comand: " + command );
        	String out = command + "\r\n";
            
        	data = null;
        	
            // open the streams and send the "y" character
            output = serialPort.getOutputStream();
            output.write( out.getBytes() );
            int i=0;
            while (!dataReceived){
            	i++;
            	if (i>=1000){
            		 throw new ArduinoException("Time-Out!");
            	}
            	Thread.sleep(10);
            }
            dataReceived=false;
        } 
        catch (Exception e) {
        	System.err.println("sendCommand : " + e.toString());
        }
        return data;
    }

    //
    // This should be called when you stop using the port
    //
    public synchronized void close() {
        if ( serialPort != null ) {
            serialPort.removeDataListener();
            serialPort.closePort();
        }
    }

    //
    // Handle serial port event
    //
    public void serialEvent(SerialPortEvent oEvent) {
        //System.out.println("Event received: " + oEvent.toString());
        try {
            switch (oEvent.getEventType() ) {
                case SerialPort.LISTENING_EVENT_DATA_AVAILABLE: 
                    if ( input == null ) {
                        input = new BufferedReader(  new InputStreamReader(  serialPort.getInputStream()  )  );
                    }
                    data = input.readLine();
                    System.out.println("RECEIVED FROM ARDUINO : " + data);
                    dataReceived = true;
                    break;
                default:
                    break;
            }
        } 
        catch (Exception e) {
            System.err.println("serialEvent : " + e.toString());
        }
    }

    public Arduino() {
        appName = getClass().getName();
    }
    
    
    public void disconnect()
    {
        //close the serial port
        try
        {
            //writeData(0, 0);

            serialPort.removeDataListener();
            serialPort.closePort();
            input.close();
            output.close();
            setConnected(false);
            //window.keybindingController.toggleControls();

            //logText = "Disconnected.";
        }
        catch (Exception e)
        {
            System.out.println("Failed to close " + serialPort.getDescriptivePortName() + "(" + e.toString() + ")");
        }
    }    

    
    public void setConnected(boolean connected){
    	this.connected = connected;
    }
    
    public boolean isConnected(){
    	return connected;
    }
    
    public static void main(String[] args)  {
    	Arduino arduinoConnect = new Arduino();
        if ( arduinoConnect.initialize() ) {
            try{
                while(true)
                {
                    double measure = arduinoConnect.getMeasure();
                    System.out.println(measure);
                    try { Thread.sleep(20); } catch (InterruptedException ie) {}
                }
            }
        catch (ArduinoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
            arduinoConnect.close();
        }

        // Wait 5 seconds then shutdown
        try { Thread.sleep(2000); } catch (InterruptedException ie) {}
        System.out.println("I'm out of here.");
    }
    
}