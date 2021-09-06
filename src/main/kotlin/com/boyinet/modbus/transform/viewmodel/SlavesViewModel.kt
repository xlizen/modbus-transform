package com.boyinet.modbus.transform.viewmodel

import com.boyinet.modbus.transform.repository.Slave
import com.boyinet.modbus.transform.repository.slaves
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.removeIf
import org.ktorm.entity.toMutableList
import org.ktorm.entity.update
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SlavesViewModel {

    val log: Logger = LoggerFactory.getLogger(SlavesViewModel::class.java)

    @Autowired
    lateinit var dataBase: Database

    val coliAvailableArray = Array(128) { true }
    val discreteInputAvailableArray = Array(128) { true }
    val inputRegisterAvailableArray = Array(128) { true }
    val holdingRegisterAvailableArray = Array(128) { true }


    var coliAddressArray = mutableListOf<Int>()
    var discreteInputAddressArray = mutableListOf<Int>()
    var holdingRegisterAddressArray = mutableListOf<Int>()
    var inputRegisterAddressArray = mutableListOf<Int>()


    val mapAddrList: ObservableList<Number> = FXCollections.observableArrayList()

    val slaveViewModel by lazy {
        SlaveViewModel()
    }

    var slaveNeedUpdate: Slave? = null
    var indexNeedUpdate: Int = 0

    var operateType: OperateType = OperateType.ADD

    val slaves: ObservableList<Slave> by lazy {
        val slaveList = dataBase.slaves.toMutableList()
        buildAvailableArray(slaveList)
        FXCollections.observableArrayList(slaveList)
    }

    fun update(slave: Slave) {
        dataBase.slaves.update(slave)
        setArrayAvailable(slave, false)
        buildAddressArray()
        slaves[this.indexNeedUpdate] = slave
    }

    fun delete(slave: Slave) {
        dataBase.slaves.removeIf { it.id eq slave.id }
        slaves.remove(slave)
        setArrayAvailable(slave, true)
        buildAddressArray()

    }

    fun save(slave: Slave) {
        dataBase.slaves.add(slave)
        slaves.add(slave)
        setArrayAvailable(slave, false)
        buildAddressArray()
    }

    fun resetAddr(slave: Slave, index: Int) {
        this.slaveNeedUpdate = slave
        this.indexNeedUpdate = index
        setArrayAvailable(slave, true)
        buildAddressArray(slave.func)
    }

    fun setAddr() {
        slaveNeedUpdate?.let {
            setArrayAvailable(this.slaveNeedUpdate!!, false)
            buildAddressArray()
            this.slaveNeedUpdate = null
        }
    }

    private fun buildAvailableArray(slaveList: List<Slave>) {
        if (slaveList.isNotEmpty()) {
            slaveList.forEach {
                setArrayAvailable(it, false)
            }
        }
        buildAddressArray()
    }

    private fun setArrayAvailable(it: Slave, available: Boolean) {
        when (it.func) {
            "01" -> {
                for (i in it.mapAddr until it.mapAddr + it.regNum) {
                    coliAvailableArray[i] = available
                }
            }
            "02" -> {
                for (i in it.mapAddr until it.mapAddr + it.regNum) {
                    discreteInputAvailableArray[i] = available
                }
            }
            "03" -> {
                when (it.regType) {
                    "16位" -> {
                        for (i in it.mapAddr until it.mapAddr + it.regNum) {
                            holdingRegisterAvailableArray[i] = available
                        }
                    }
                    "32位" -> {
                        for (i in it.mapAddr until (it.mapAddr + it.regNum).times(2)) {
                            holdingRegisterAvailableArray[i] = available
                        }
                    }
                }
            }
            "04" -> {
                when (it.regType) {
                    "16位" -> {
                        for (i in it.mapAddr until it.mapAddr + it.regNum) {
                            inputRegisterAvailableArray[i] = available
                        }
                    }
                    "32位" -> {
                        for (i in it.mapAddr until (it.mapAddr + it.regNum).times(2)) {
                            inputRegisterAvailableArray[i] = available
                        }
                    }
                }
            }
        }
    }

    private fun buildAddressArray(func: String = "01") {
        coliAddressArray = coliAvailableArray.mapIndexed { index, available ->
            if (available) index else null
        }.filterNotNull().toMutableList()
        log.info("[coli可用地址]:{}", coliAddressArray)

        discreteInputAddressArray = discreteInputAvailableArray.mapIndexed { index, available ->
            if (available) index else null
        }.filterNotNull().toMutableList()
        log.info("[discreteInput可用地址]:{}", discreteInputAddressArray)

        holdingRegisterAddressArray = holdingRegisterAvailableArray.mapIndexed { index, available ->
            if (available) index else null
        }.filterNotNull().toMutableList()
        log.info("[holdingRegister可用地址]:{}", holdingRegisterAddressArray)


        inputRegisterAddressArray = inputRegisterAvailableArray.mapIndexed { index, available ->
            if (available) index else null
        }.filterNotNull().toMutableList()
        log.info("[inputRegister可用地址]:{}", inputRegisterAddressArray)

        updateMapAddr(func)
    }


    fun updateMapAddr(func: String) {
        when (func) {
            "01" -> mapAddrList.setAll(coliAddressArray)
            "02" -> mapAddrList.setAll(discreteInputAddressArray)
            "03" -> mapAddrList.setAll(holdingRegisterAddressArray)
            "04" -> mapAddrList.setAll(inputRegisterAddressArray)
        }
    }

    enum class OperateType {
        ADD,
        UPDATE
    }
}