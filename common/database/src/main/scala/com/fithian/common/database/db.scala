package com.fithian.common.database

import java.sql.Driver
import org.sqlite.JDBC
import org.slf4j.LoggerFactory
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.meta.MTable
import com.fithian.common.config.ConfigLoader
import com.paytronix.utils.scala.result.{Result, Okay, Failed, tryCatchValue}

object db {
    case class DatabaseWrapper(database: Database) {
        private val logger = LoggerFactory.getLogger(getClass)
        logger.info("starting database instance")
        database withSession {
            implicit session => {
                Seq(
                    if (MTable.getTables(device_enums.baseTableRow.tableName).list.isEmpty) Some(device_enums.ddl) else None,
                    if (MTable.getTables(devices.baseTableRow.tableName).list.isEmpty) Some(devices.ddl) else None,
                    if (MTable.getTables(counters.baseTableRow.tableName).list.isEmpty) Some(counters.ddl) else None,
                    if (MTable.getTables(counter_details.baseTableRow.tableName).list.isEmpty) Some(counter_details.ddl) else None,
                    if (MTable.getTables(transactions.baseTableRow.tableName).list.isEmpty) Some(transactions.ddl) else None
                ).flatten.foreach(_.create)
                if (!device_enums.filter(_.deviceEnumId === device_enums.ID_BOTTLE_OPENER).exists.run)
                    device_enums += DeviceEnum(device_enums.ID_BOTTLE_OPENER, "bottle opener")
                if (!devices.filter(_.deviceId === 1).exists.run)
                    devices += Device(1, device_enums.ID_BOTTLE_OPENER)
                if (!counters.filter(_.counterId === 1).exists.run)
                    counters += Counter(1, 1)
            }
        }
    }
    val config = ConfigLoader.databaseConfig
    val driver = Class.forName(config.driver).newInstance().asInstanceOf[Driver]
    config.file.orThrow
    lazy val instance = DatabaseWrapper(Database.forDriver(driver, config.url))
    def get = instance.database
}
