Hey guys,

So Its 2:09 AM and I've decided to stop coding.

Check out RcCarv0.1 (version 0.1)

I added a class to util called BluetoothCommunicationService

This was modeled very closely to the specifications designed by:
http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/BluetoothChatService.html

I didn't include all of the comments, but you can see that HTML file for them.
Please try and finish implementing the communication stuff (I'll also be doing
this so don't worry).  Currently, I'm deviating slightly from our design for
testing purposes.  I'm treating MainActivity.java like the MasterButtonActivity.java.
This means that I'm having the MainActivity try and send (cough write) data to
SlaveActivity.java.  I've started to implement some of this.  The way you need
to think about it is as follows:

Our design is different than the chat one, only slightly.  The chat program requires
an activity to be able to write data and also view fetched data.  Our program requires
this but in 2 steps.  The Master class (for now, MainActivity.java) should be sending 
(again, think "write") data and the SlaveActivity should be able to receive this data
(we will try and make the Slave.java class receive this at a later date).  Because of
this fact, we need to do some different message passing schemes.  The chat program 
simply throws messages back to the Activity.  Ours will need to do this.  I wouldn't 
worry about this if you try and implement anything without me, because I know how to 
get around this issue (the solution requires passing extra data to constructors).  

Before trying to implement any of the stuff, please read (and understand) the Bluetooth
Android developer article, as well as the BluetoothChat application.  These two combined
will allow us to get the bluetooth stuff to work!