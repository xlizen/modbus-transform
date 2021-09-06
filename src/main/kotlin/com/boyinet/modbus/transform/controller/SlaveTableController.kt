package com.boyinet.modbus.transform.controller

import com.boyinet.modbus.transform.pojo.SlaveModel
import com.boyinet.modbus.transform.repository.Slave
import com.boyinet.modbus.transform.viewmodel.SlavesViewModel
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXDialog
import de.felixroske.jfxsupport.FXMLController
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.util.Callback
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.Style
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.Assert
import java.net.URL
import java.util.*


@FXMLController
class SlaveTableController : Initializable {

    @Autowired
    lateinit var slavesViewModel: SlavesViewModel

    @FXML
    lateinit var rootPane: StackPane

    @FXML
    lateinit var slaveTable: TableView<Slave>

    @FXML
    lateinit var slaveAddrColumn: TableColumn<Slave, Int>

    @FXML
    lateinit var regTypeColumn: TableColumn<Slave, String>

    @FXML
    lateinit var funcColumn: TableColumn<Slave, String>

    @FXML
    lateinit var regStartAddrColumn: TableColumn<Slave, Int>

    @FXML
    lateinit var regNumColumn: TableColumn<Slave, Int>

    @FXML
    lateinit var mapStartAddrColumn: TableColumn<Slave, Int>

    lateinit var operateColumn: TableColumn<Slave, HBox>

    @FXML
    lateinit var slaveDialog: JFXDialog

    @FXML
    lateinit var slaveAddrTf: TextField

    @FXML
    lateinit var regTypeCb: ComboBox<String>

    @FXML
    lateinit var funcCb: ComboBox<String>

    @FXML
    lateinit var regAddrTf: TextField

    @FXML
    lateinit var regNumTf: TextField

    @FXML
    lateinit var mapAddrCb: ComboBox<Number>

    var mapAddrIndex: Int = 0

    lateinit var slaveModel: SlaveModel


    fun openAddDialog(event: ActionEvent?) {
        slavesViewModel.operateType = SlavesViewModel.OperateType.ADD
        slaveDialog.show(rootPane)
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val jMetro = JMetro(Style.LIGHT)
        jMetro.parent = rootPane

        val slaves = slavesViewModel.slaves
        val slaveViewModel = slavesViewModel.slaveViewModel
        slaveModel = slaveViewModel.slaveModel

        bindTableProperty()
        renderTable(slaves)

        slaveModel.mapAddr.value = slavesViewModel.mapAddrList[0].toInt()
        bindDialogProperty()

        funcCb.items = slaveViewModel.funcList
        mapAddrCb.items = slavesViewModel.mapAddrList

        regTypeCb.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            slaveModel.func.value = ""
            slaveViewModel.updateFunc(newValue)
            funcCb.selectionModel.selectFirst()
        }

        funcCb.selectionModel.selectedItemProperty().addListener { _, _, newValue: String ->
            slavesViewModel.updateMapAddr(newValue)
            mapAddrCb.selectionModel.selectFirst()
        }

        mapAddrCb.selectionModel.selectedIndexProperty().addListener { _, _, newValue: Number ->
            run {
                mapAddrIndex = newValue.toInt()
            }
        }

        slaveDialog.onDialogClosed = EventHandler {
            resetViewModel()
        }
    }

    private fun bindDialogProperty() {
        slaveAddrTf.textProperty().bindBidirectional(slaveModel.slaveAddr)
        regTypeCb.valueProperty().bindBidirectional(slaveModel.regType)
        funcCb.valueProperty().bindBidirectional(slaveModel.func)
        regAddrTf.textProperty().bindBidirectional(slaveModel.regAddr)
        regNumTf.textProperty().bindBidirectional(slaveModel.regNum)
        mapAddrCb.valueProperty().bindBidirectional(slaveModel.mapAddr)
    }

    private fun renderTable(slaves: ObservableList<Slave>) {
        //内容居中
        slaveTable.columns.forEach {
            it.cellFactory = Callback {
                object : TableCell<Slave?, Any?>() {
                    override fun updateItem(item: Any?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = item?.toString()
                        alignment = Pos.CENTER
                    }
                }
            }
        }

        //渲染操作列
        operateColumn = TableColumn("操作")
        operateColumn.prefWidth = 180.0
        operateColumn.cellFactory = Callback {
            object : TableCell<Slave?, HBox?>() {
                override fun updateItem(item: HBox?, empty: Boolean) {
                    super.updateItem(item, empty)
                    val hBox = HBox()
                    val edit = JFXButton("编 辑")
                    edit.styleClass.add("btn-warning")
                    edit.onMouseClicked = EventHandler {
                        slaveModel.set(slaves[index])
                        slavesViewModel.resetAddr(slaves[index], index)
                        slavesViewModel.operateType = SlavesViewModel.OperateType.UPDATE
                        slaveDialog.show(rootPane)
                    }
                    val delete = JFXButton("删 除")
                    delete.styleClass.add("btn-danger")
                    delete.onMouseClicked = EventHandler {
                        val slave = slaves[index]
                        slavesViewModel.delete(slave)
                    }
                    hBox.alignment = Pos.CENTER
                    hBox.spacing = 10.0
                    hBox.children.addAll(edit, delete)
                    graphic = if (empty) null else hBox
                }
            }
        }
        slaveTable.columns.add(operateColumn)
        slaveTable.items = slaves
        slaveTable.isTableMenuButtonVisible = true
    }

    private fun bindTableProperty() {
        slaveAddrColumn.cellValueFactory = PropertyValueFactory("slaveAddr")
        regTypeColumn.cellValueFactory = PropertyValueFactory("regType")
        funcColumn.cellValueFactory = PropertyValueFactory("func")
        regStartAddrColumn.cellValueFactory = PropertyValueFactory("regAddr")
        regNumColumn.cellValueFactory = PropertyValueFactory("regNum")
        mapStartAddrColumn.cellValueFactory = PropertyValueFactory("mapAddr")
    }

    fun cancel(event: ActionEvent?) {
        slaveDialog.close()
    }

    private fun resetViewModel() {
        slaveModel.run {
            slaveAddr.value = ""
            regType.value = "布尔"
            func.value = "01"
            regAddr.value = ""
            regNum.value = ""
            mapAddr.value = slavesViewModel.coliAddressArray[0]

        }
        if (slavesViewModel.operateType == SlavesViewModel.OperateType.UPDATE) {
            slavesViewModel.setAddr()
        }
    }


    fun save(event: ActionEvent?) {
        try {
            validation()
            val slave = Slave {
                slaveAddr = slaveAddrTf.text.toInt()
                regType = regTypeCb.value
                func = funcCb.value
                regAddr = regAddrTf.text.toInt()
                regNum = regNumTf.text.toInt()
                mapAddr = mapAddrCb.value.toInt()
            }
            when (slavesViewModel.operateType) {
                SlavesViewModel.OperateType.ADD -> slavesViewModel.save(slave)
                SlavesViewModel.OperateType.UPDATE -> {
                    slave.id = slaveModel.id.value
                    slavesViewModel.update(slave)

                }
            }
            slaveDialog.close()
        } catch (e: IllegalArgumentException) {
            val alert = Alert(Alert.AlertType.WARNING, e.message)
            alert.show()
        }
    }

    private fun validation() {
        Assert.hasText(slaveAddrTf.text, "从机地址为空")
        Assert.hasText(regTypeCb.value, "寄存器类型为空")
        Assert.hasText(funcCb.value, "功能码为空")
        Assert.hasText(regAddrTf.text, "寄存器起始地址为空")
        Assert.hasText(regNumTf.text, "寄存器数量为空")
        Assert.notNull(mapAddrCb.value, "映射地址为空")
        val regNumInt = regNumTf.text.toInt()
        val mapAddrInt = mapAddrCb.value.toInt()
        var endAddrInt = mapAddrInt + regNumInt - 1
        when (regTypeCb.value) {
            "16位" -> {
                when (funcCb.value) {
                    "03" -> {
                        if (endAddrInt != slavesViewModel.holdingRegisterAddressArray[mapAddrIndex + regNumInt - 1]) {
                            throw IllegalArgumentException("映射地址不连续请重新选择")
                        }
                    }
                    "04" -> {
                        if (endAddrInt != slavesViewModel.inputRegisterAddressArray[mapAddrIndex + regNumInt - 1]) {
                            throw IllegalArgumentException("映射地址不连续请重新选择")
                        }
                    }
                }
            }
            "32位" -> {
                endAddrInt = mapAddrInt + regNumInt.times(2) - 1
                when (funcCb.value) {
                    "03" -> {
                        if (endAddrInt != slavesViewModel.holdingRegisterAddressArray[mapAddrIndex + regNumInt.times(2) - 1]
                        ) {
                            throw IllegalArgumentException("映射地址不连续请重新选择")
                        }
                    }
                    "04" -> {
                        if (endAddrInt != slavesViewModel.inputRegisterAddressArray[mapAddrIndex + regNumInt.times(2) - 1]
                        ) {
                            throw IllegalArgumentException("映射地址不连续请重新选择")
                        }
                    }
                }
            }
            "布尔" -> {
                when (funcCb.value) {
                    "01" -> {
                        if (endAddrInt != slavesViewModel.coliAddressArray[mapAddrIndex + regNumInt - 1]) {
                            throw IllegalArgumentException("映射地址不连续请重新选择")
                        }
                    }
                    "02" -> {
                        if (endAddrInt != slavesViewModel.discreteInputAddressArray[mapAddrIndex + regNumInt - 1]) {
                            throw IllegalArgumentException("映射地址不连续请重新选择")
                        }
                    }
                }
            }
        }
    }

}