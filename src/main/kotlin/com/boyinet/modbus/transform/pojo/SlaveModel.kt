package com.boyinet.modbus.transform.pojo

import com.boyinet.modbus.transform.repository.Slave
import javafx.beans.property.*

class SlaveModel {


    var id: LongProperty = SimpleLongProperty(0)
    var check: BooleanProperty = SimpleBooleanProperty(false)
    var slaveAddr: StringProperty = SimpleStringProperty("")
    var regType: StringProperty = SimpleStringProperty("布尔")
    var func: StringProperty = SimpleStringProperty("01")
    var regAddr: StringProperty = SimpleStringProperty("")
    var regNum: StringProperty = SimpleStringProperty("")
    var mapAddr: IntegerProperty = SimpleIntegerProperty(0)


    fun set(_slave: Slave) {
        id.value = _slave.id
        check.value = _slave.check
        slaveAddr.value = _slave.slaveAddr.toString()
        regType.value = _slave.regType
        func.value = _slave.func
        regAddr.value = _slave.regAddr.toString()
        regNum.value = _slave.regNum.toString()
        mapAddr.value = _slave.mapAddr
    }
}