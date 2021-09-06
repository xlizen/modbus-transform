package com.boyinet.modbus.transform.pojo

import javafx.beans.property.*

data class SerialPortModel(
    val portName: StringProperty = SimpleStringProperty("COM1"),
    val baudRate: IntegerProperty = SimpleIntegerProperty(9600),
    val dataBits: IntegerProperty = SimpleIntegerProperty(8),
    val stopBits: IntegerProperty = SimpleIntegerProperty(1),
    val parity: StringProperty = SimpleStringProperty("NONE")
)
