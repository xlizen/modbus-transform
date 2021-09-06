package com.boyinet.modbus.transform.repository

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface Slave : Entity<Slave> {
    companion object : Entity.Factory<Slave>()

    var id: Long
    var check: Boolean
    var slaveAddr: Int
    var regType: String
    var func: String
    var regAddr: Int
    var regNum: Int
    var mapAddr: Int
}

object Slaves : Table<Slave>("slave") {

    var id = long("id").primaryKey().bindTo { it.id }
    var check = boolean("is_check").bindTo { it.check }
    var slaveAddr = int("slave_addr").bindTo { it.slaveAddr }
    var regType = varchar("reg_type").bindTo { it.regType }
    var func = varchar("func").bindTo { it.func }
    var regAddr = int("reg_addr").bindTo { it.regAddr }
    var regNum = int("reg_num").bindTo { it.regNum }
    var mapAddr = int("map_addr").bindTo { it.mapAddr }
}

val Database.slaves get() = this.sequenceOf(Slaves)
