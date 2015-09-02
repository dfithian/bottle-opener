package com.fithian.database

import akka.actor.Actor
import com.fithian.common.actor.{ActorOps, ActorStateMessage}
import com.fithian.common.database.{db, DeviceEnum, Device, Counter, CounterDetail, Transaction}

class DatabaseSupervisor extends Actor with ActorOps {
    def start() = db.get
    def stop() = context stop self
    def receive = {
        case asm: ActorStateMessage => actorStateMessage(asm)
    }
}
