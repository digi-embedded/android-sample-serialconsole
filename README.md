Serial Console Sample Application
=================================

This example demonstrates the usage of the Serial Port API. The application
opens a bi-directional serial port connection with a set of parameters. Users
can view all incoming serial port data and send messages back.

Demo requirements
-----------------

To run this example you need:

* A compatible development board to host the application.
* A USB connection between the board and the host PC in order to transfer and
  launch the application.
* A serial connection between the board and another machine (PC for example)
  using a serial cable.

Demo setup
----------

Make sure the hardware is set up correctly:

1. The development board is powered on.
2. The board is connected directly to the PC by the micro USB cable.
3. There is a serial connection between the device and another machine. You must
   setup the application to:
   * ConnectCore 8X SBC Pro:  `/dev/ttyLP0` or `/dev/ttyMCA0`.
   * ConnectCore 8M Mini Development Kit: `/dev/ttymxc2` or `/dev/ttymxc3`.

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

Compatible with
---------------

* ConnectCore 6 SBC
* ConnectCore 6 SBC v3
* ConnectCore 8X SBC Pro
* ConnectCore 8M Mini Development Kit

License
---------

Copyright (c) 2014-2021, Digi International Inc. <support@digi.com>

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.