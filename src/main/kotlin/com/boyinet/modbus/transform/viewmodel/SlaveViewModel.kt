package com.boyinet.modbus.transform.viewmodel

import com.boyinet.modbus.transform.pojo.SlaveModel
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class SlaveViewModel {

    private val boolFunc = listOf("01", "02", "15")
    private val numberFunc = listOf("03", "04", "16")

    val funcList: ObservableList<String> = FXCollections.observableArrayList(boolFunc)


    val slaveModel: SlaveModel by lazy {
        SlaveModel()
    }

    fun updateFunc(regType: String) {
        when (regType) {
            "16位", "32位" -> {
                funcList.setAll(numberFunc)
            }
            else -> {
                funcList.setAll(boolFunc)
            }
        }
    }
}