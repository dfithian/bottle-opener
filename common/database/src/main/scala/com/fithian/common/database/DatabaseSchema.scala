package com.fithian.common.database

import java.sql.Timestamp
import scala.slick.driver.SQLiteDriver.simple._

case class DeviceEnum(deviceEnumId: Int, label: String)
object device_enums extends TableQuery(DeviceEnumSchema(_)) {
    val ID_BOTTLE_OPENER = 1
}
case class DeviceEnumSchema(tag: Tag) extends Table[DeviceEnum](tag, "device_enum") {
    def deviceEnumId = column[Int]("device_enum_id", O.PrimaryKey)
    def label = column[String]("label")
    def * = (deviceEnumId, label) <> (DeviceEnum.tupled, DeviceEnum.unapply)
}
case class Device(deviceId: Int, deviceEnumId: Int)
object devices extends TableQuery(DeviceSchema(_))
case class DeviceSchema(tag: Tag) extends Table[Device](tag, "device") {
    def deviceId = column[Int]("device_id", O.PrimaryKey, O.AutoInc)
    def deviceEnumId = column[Int]("device_enum_id")
    def * = (deviceId, deviceEnumId) <> (Device.tupled, Device.unapply)
    def deviceEnum = foreignKey("fk_device_enum__device", deviceEnumId, device_enums)(_.deviceEnumId)
}
case class Counter(counterId: Int, deviceId: Int)
object counters extends TableQuery(CounterSchema(_))
case class CounterSchema(tag: Tag) extends Table[Counter](tag, "counter") {
    def counterId = column[Int]("counter_id", O.PrimaryKey, O.AutoInc)
    def deviceId = column[Int]("device_id")
    def * = (counterId, deviceId) <> (Counter.tupled, Counter.unapply)
    def device = foreignKey("fk_device__counter", deviceId, devices)(_.deviceId)
}

case class Transaction(transactionId: Option[Int], counterId: Int, deviceId: Int, createdDatetime: Timestamp)
object transactions extends TableQuery(TransactionSchema(_))
case class TransactionSchema(tag: Tag) extends Table[Transaction](tag, "transaction") {
    def transactionId = column[Int]("transaction_id", O.PrimaryKey, O.AutoInc)
    def counterId = column[Int]("counter_id")
    def deviceId = column[Int]("device_id")
    def createdDatetime = column[Timestamp]("created_datetime")
    def * = (transactionId.?, counterId, deviceId, createdDatetime) <> (Transaction.tupled, Transaction.unapply)
    def device = foreignKey("fk_device__transaction", deviceId, devices)(_.deviceId)
    def counter = foreignKey("fk_counter__transaction", counterId, counters)(_.counterId)
}
case class CounterDetail(counterDetailId: Option[Int], transactionId: Int, counterId: Int, deviceId: Int, createdDatetime: Timestamp)
object counter_details extends TableQuery(CounterDetailSchema(_))
case class CounterDetailSchema(tag: Tag) extends Table[CounterDetail](tag, "counter_detail") {
    def counterDetailId = column[Int]("counter_detail_id", O.PrimaryKey, O.AutoInc)
    def transactionId = column[Int]("transaction_id")
    def counterId = column[Int]("counter_id")
    def deviceId = column[Int]("device_id")
    def createdDatetime = column[Timestamp]("created_datetime")
    def * = (counterDetailId.?, transactionId, counterId, deviceId, createdDatetime) <> (CounterDetail.tupled, CounterDetail.unapply)
    def device = foreignKey("fk_device__counter_detail", deviceId, devices)(_.deviceId)
    def counter = foreignKey("fk_counter__counter_detail", counterId, counters)(_.counterId)
    def transaction = foreignKey("fk_transaction__counter_detail", transactionId, transactions)(_.transactionId)
}
