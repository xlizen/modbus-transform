package com.boyinet.modbus.transform.controller

import com.boyinet.modbus.transform.pojo.SerialPortModel
import com.boyinet.modbus.transform.view.SlaveTableView
import com.boyinet.modbus.transform.viewmodel.SlavesViewModel
import com.intelligt.modbus.jlibmodbus.Modbus
import com.intelligt.modbus.jlibmodbus.data.ModbusCoils
import com.intelligt.modbus.jlibmodbus.data.ModbusHoldingRegisters
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory
import com.intelligt.modbus.jlibmodbus.msg.request.ReadCoilsRequest
import com.intelligt.modbus.jlibmodbus.msg.request.ReadDiscreteInputsRequest
import com.intelligt.modbus.jlibmodbus.msg.request.ReadHoldingRegistersRequest
import com.intelligt.modbus.jlibmodbus.msg.request.ReadInputRegistersRequest
import com.intelligt.modbus.jlibmodbus.msg.response.ReadCoilsResponse
import com.intelligt.modbus.jlibmodbus.msg.response.ReadDiscreteInputsResponse
import com.intelligt.modbus.jlibmodbus.msg.response.ReadHoldingRegistersResponse
import com.intelligt.modbus.jlibmodbus.msg.response.ReadInputRegistersResponse
import com.intelligt.modbus.jlibmodbus.serial.SerialParameters
import com.intelligt.modbus.jlibmodbus.serial.SerialPort
import com.intelligt.modbus.jlibmodbus.serial.SerialPortFactoryJSSC
import com.intelligt.modbus.jlibmodbus.serial.SerialUtils
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlave
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveFactory
import com.intelligt.modbus.jlibmodbus.utils.DataUtils
import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport
import de.felixroske.jfxsupport.FXMLController
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.layout.VBox
import javafx.stage.Modality
import jfxtras.styles.jmetro.FlatAlert
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.Style
import jssc.SerialPortException
import jssc.SerialPortList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


@FXMLController
class IndexController : Initializable {

    val log: Logger = LoggerFactory.getLogger(IndexController::class.java)

    @Autowired
    lateinit var slavesViewModel: SlavesViewModel

    companion object {
        val PARITY_LIST: List<String> = listOf("NONE", "ODD", "EvEN")
    }

    @FXML
    lateinit var root: VBox

    @FXML
    lateinit var portNameRx: ComboBox<String>

    @FXML
    lateinit var baudRateRx: ComboBox<Number>

    @FXML
    lateinit var dataBitsRx: ComboBox<Number>

    @FXML
    lateinit var stopBitsRx: ComboBox<Number>

    @FXML
    lateinit var parityRx: ComboBox<String>


    @FXML
    lateinit var portNameTx: ComboBox<String>

    @FXML
    lateinit var baudRateTx: ComboBox<Number>

    @FXML
    lateinit var dataBitsTx: ComboBox<Number>

    @FXML
    lateinit var stopBitsTx: ComboBox<Number>

    @FXML
    lateinit var parityTx: ComboBox<String>

    @FXML
    lateinit var rxButton: Button

    @FXML
    lateinit var txButton: Button


    private var rxStatus = false

    @Volatile
    private var txStatus = false

    lateinit var master: ModbusMaster

    lateinit var slave: ModbusSlave

    val lock: ReentrantLock = ReentrantLock()

    var portModelRx: SerialPortModel = SerialPortModel()

    var portModelTx: SerialPortModel = SerialPortModel()

    //保持寄存器
    val holdingRegisters: ModbusHoldingRegisters = ModbusHoldingRegisters(127)

    //线圈
    val coils: ModbusCoils = ModbusCoils(127)

    //输入寄存器
    val inputRegisters: ModbusHoldingRegisters = ModbusHoldingRegisters(127)

    //离散线圈
    val discreteInputs: ModbusCoils = ModbusCoils(127)


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val jMetro = JMetro(Style.LIGHT)
        jMetro.parent = root
        val portNames: Array<String> = SerialPortList.getPortNames()
        val observableArrayList = FXCollections.observableArrayList(*portNames)
        portNameRx.items = observableArrayList
        portNameRx.valueProperty().bindBidirectional(portModelRx.portName)
        baudRateRx.valueProperty().bindBidirectional(portModelRx.baudRate)
        dataBitsRx.valueProperty().bindBidirectional(portModelRx.dataBits)
        stopBitsRx.valueProperty().bindBidirectional(portModelRx.stopBits)
        parityRx.valueProperty().bindBidirectional(portModelRx.parity)
        portNameTx.items = observableArrayList
        portNameTx.valueProperty().bindBidirectional(portModelTx.portName)
        baudRateTx.valueProperty().bindBidirectional(portModelTx.baudRate)
        dataBitsTx.valueProperty().bindBidirectional(portModelTx.dataBits)
        stopBitsTx.valueProperty().bindBidirectional(portModelTx.stopBits)
        parityTx.valueProperty().bindBidirectional(portModelTx.parity)
    }

    @Throws(SerialPortException::class)
    fun openTx(event: ActionEvent?) {
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG)
        if (txStatus) {
            try {
                lock.lock()
                master.disconnect()
                txStatus = false
                txButton.text = "读 取"
            } catch (e: ModbusIOException) {
                log.warn("serial exception:${e.message}", e)
            } finally {
                lock.unlock()
            }
        } else {
            try {
                val sp = SerialParameters().apply {
                    device = portModelTx.portName.value
                    setBaudRate(SerialPort.BaudRate.getBaudRate(portModelTx.baudRate.value))
                    dataBits = portModelTx.dataBits.value
                    parity = SerialPort.Parity.getParity(PARITY_LIST.indexOf(portModelTx.parity.value))
                    stopBits = portModelTx.dataBits.value
                }
                SerialUtils.setSerialPortFactory(SerialPortFactoryJSSC())
                master = ModbusMasterFactory.createModbusMasterRTU(sp)
                master.connect()
                txStatus = master.isConnected
                txButton.text = "断 开"
                println("Modbus Master status:$txStatus")
                //you can invoke #connect method manually, otherwise it'll be invoked automatically
                Thread {
                    while (txStatus) {
                        lock.lock()
                        if (txStatus) {
                            try {
                                buildDataHolder(master)
                            } catch (e: Exception) {
                                log.warn("modbus exception:${e.message}", e)
                            }
                        }
                        lock.unlock()
                        TimeUnit.MICROSECONDS.sleep(10)
                    }
                }.start()
            } catch (e: Exception) {
                log.warn("serial exception:${e.message}", e)
                val alert = FlatAlert(Alert.AlertType.WARNING, e.message)
                alert.show()
            }
        }
    }

    @Throws(SerialPortException::class)
    fun openRx(event: ActionEvent?) {
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG)
        if (rxStatus) {
            try {
                slave.shutdown()
                rxStatus = false
                rxButton.text = "监 听"
            } catch (e: ModbusIOException) {
                e.printStackTrace()
            }
        } else {
            try {

                val sp = SerialParameters().apply {
                    device = portModelRx.portName.value
                    setBaudRate(SerialPort.BaudRate.getBaudRate(portModelRx.baudRate.value))
                    dataBits = portModelRx.dataBits.value
                    parity = SerialPort.Parity.getParity(PARITY_LIST.indexOf(portModelRx.parity.value))
                    stopBits = portModelRx.dataBits.value
                }

                SerialUtils.setSerialPortFactory(SerialPortFactoryJSSC())

                slave = ModbusSlaveFactory.createModbusSlaveRTU(sp).apply {
                    serverAddress = 1
                    isBroadcastEnabled = true
                    readTimeout = 10000
                    dataHolder.coils = coils
                    dataHolder.discreteInputs = discreteInputs
                    dataHolder.holdingRegisters = holdingRegisters
                    dataHolder.inputRegisters = inputRegisters
                }
                slave.listen()
                rxStatus = slave.isListening
                rxButton.text = "断 开"
            } catch (e: Exception) {
                log.warn("serial exception:${e.message}", e)
                val alert = Alert(Alert.AlertType.WARNING, e.message)
                alert.show()
            }
        }
    }

    @Throws(IOException::class)
    fun openSlaveDialog() {
        AbstractJavaFxApplicationSupport.showView(SlaveTableView::class.java, Modality.WINDOW_MODAL)
    }

    private fun buildDataHolder(master: ModbusMaster) {
        val observableList = slavesViewModel.slaves
        if (observableList.isNotEmpty()) {
            observableList.forEach {
                val slaveAddr: Int = it.slaveAddr
                val regAddr: Int = it.regAddr
                val regNum: Int = it.regNum
                val mapAddr: Int = it.mapAddr
                val func: String = it.func
                when (it.regType) {
                    "布尔" -> when (func) {
                        "01" -> {
                            val readCoilsRequest = ReadCoilsRequest().apply {
                                serverAddress = slaveAddr
                                startAddress = regAddr
                                quantity = regNum
                            }
                            val readCoilsResponse: ReadCoilsResponse =
                                readCoilsRequest.response as ReadCoilsResponse
                            master.processRequest(readCoilsRequest)
                            val responseCoils: BooleanArray = DataUtils.toBitsArray(
                                readCoilsResponse.bytes,
                                readCoilsResponse.byteCount * 8
                            )
                            var i = 0
                            while (i < regNum) {
                                coils.set(mapAddr + i, responseCoils[i])
                                i++
                            }
                        }
                        "02" -> {
                            val readDiscreteInputsRequest = ReadDiscreteInputsRequest().apply {
                                serverAddress = slaveAddr
                                startAddress = regAddr
                                quantity = regNum
                            }
                            val readDiscreteInputsResponse: ReadDiscreteInputsResponse =
                                readDiscreteInputsRequest.response as ReadDiscreteInputsResponse
                            master.processRequest(readDiscreteInputsRequest)
                            val responseDiscreteInputs: BooleanArray = DataUtils.toBitsArray(
                                readDiscreteInputsResponse.bytes,
                                readDiscreteInputsResponse.byteCount * 8
                            )
                            var i = 0
                            while (i < regNum) {
                                discreteInputs.set(mapAddr, responseDiscreteInputs[i])
                                i++
                            }
                        }
                    }
                    "16位" -> when (func) {
                        "03" -> {
                            val readHoldingRegistersRequest = ReadHoldingRegistersRequest().apply {
                                serverAddress = slaveAddr
                                startAddress = regAddr
                                quantity = regNum
                            }
                            val readHoldingRegistersRequestResponse: ReadHoldingRegistersResponse =
                                readHoldingRegistersRequest.response as ReadHoldingRegistersResponse
                            master.processRequest(readHoldingRegistersRequest)
                            val responseHoldingRegisters: ModbusHoldingRegisters =
                                readHoldingRegistersRequestResponse.holdingRegisters
                            var i = 0
                            while (i < regNum) {
                                holdingRegisters.setInt16At(mapAddr + i, responseHoldingRegisters.getInt16At(i))
                                i++
                            }
                        }
                        "04" -> {
                            val readInputRegistersRequest = ReadInputRegistersRequest().apply {
                                serverAddress = slaveAddr
                                startAddress = regAddr
                                quantity = regNum
                            }
                            val readInputRegistersRequestResponse: ReadInputRegistersResponse =
                                readInputRegistersRequest.response as ReadInputRegistersResponse
                            master.processRequest(readInputRegistersRequest)
                            val responseInputRegisters: ModbusHoldingRegisters =
                                readInputRegistersRequestResponse.holdingRegisters
                            var i = 0
                            while (i < regNum) {
                                inputRegisters.setInt16At(mapAddr + i, responseInputRegisters.getInt16At(i))
                                i++
                            }
                        }
                    }
                    "32位" -> when (func) {
                        "03" -> {
                            val readHoldingRegistersRequest = ReadHoldingRegistersRequest().apply {
                                serverAddress = slaveAddr
                                startAddress = regAddr
                                quantity = regNum * 2
                            }
                            val readHoldingRegistersRequestResponse: ReadHoldingRegistersResponse =
                                readHoldingRegistersRequest.response as ReadHoldingRegistersResponse
                            master.processRequest(readHoldingRegistersRequest)
                            val responseHoldingRegisters: ModbusHoldingRegisters =
                                readHoldingRegistersRequestResponse.holdingRegisters
                            var i = 0
                            while (i < regNum) {
                                holdingRegisters.setInt32At(mapAddr + i * 2, responseHoldingRegisters.getInt32At(i))
                                i++
                            }
                        }
                        "04" -> {
                            val readInputRegistersRequest = ReadInputRegistersRequest().apply {
                                serverAddress = slaveAddr
                                startAddress = regAddr
                                quantity = regNum * 2
                            }
                            val readInputRegistersRequestResponse: ReadInputRegistersResponse =
                                readInputRegistersRequest.response as ReadInputRegistersResponse
                            master.processRequest(readInputRegistersRequest)
                            val responseInputRegisters: ModbusHoldingRegisters =
                                readInputRegistersRequestResponse.holdingRegisters
                            var i = 0
                            while (i < regNum) {
                                inputRegisters.setInt16At(mapAddr + i * 2, responseInputRegisters.getInt32At(i))
                                i++
                            }
                        }
                    }
                }
            }
        }
    }
}