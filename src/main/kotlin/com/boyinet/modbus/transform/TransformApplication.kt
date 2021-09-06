package com.boyinet.modbus.transform

import com.boyinet.modbus.transform.view.IndexView
import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class TransformApplication : AbstractJavaFxApplicationSupport()

fun main(args: Array<String>) {
    AbstractJavaFxApplicationSupport.launch(TransformApplication::class.java, IndexView::class.java, args)
}