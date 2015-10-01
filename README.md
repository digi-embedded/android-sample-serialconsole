Serial Console Sample Application
=================================

This example demonstrates the usage of the Serial Port API. The application
opens a bi-directional serial port connection with a set of parameters. Users
can view all incoming serial port data and send messages back.

Demo requirements
-----------------

To run this example you need:

* One compatible device to host the application.
* Network connection between the device and the host PC in order to transfer
  and launch the application.
* Establish remote target connection to your Digi hardware before running this
  application.
* A serial connection between the device and another machine (PC for example)
  using a serial cable.

Demo setup
----------

Make sure the hardware is set up correctly:

1. The device is powered on.
2. The device is connected directly to the PC or to the Local Area Network (LAN)
   by the Ethernet cable.
3. There is a serial connection between the device and another machine. You need
   to use the J29 UART connector and setup the application to `/dev/ttymxc0`,
   `/dev/ttymxc2` or `/dev/ttymxc4` to successfully run this application.

Demo run
--------

The example is already configured, so all you need to do is to build and 
launch the project.
  
When application starts, it displays a dialog asking for the serial connection
parameters. Fill them according the serial connection you established with the
other machine:

* Serial Port
* Enable local echo (to display write messages in the console)
* Baud Rate (9600 by default)
* Data Bits (8 by default)
* Stop Bits (1 by default)
* Parity (NONE by default)
* Flow Control (NONE by default)

Click **Close** when you are done.

The application displays a large black message box where all incoming messages
are written.

At the bottom, a toolbar allows the following actions:

* **Connect/Disconnect**: Opens/closes the serial port connection.
* **Clear message list**: Removes all messages from the console.
* **Setup**: Reconfigures serial connection parameters.
* **Send message**: Sends a message to the other machine using the serial
  connection.

Tested on
---------

* ConnectCore Wi-i.MX51
* ConnectCore Wi-i.MX53
* ConnectCard for i.MX28
* ConnectCore 6 Adapter Board
* ConnectCore 6 SBC
* ConnectCore 6 SBC v2