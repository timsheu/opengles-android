package com.nuvoton.thermalviewergl

import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.nio.ByteBuffer

class RxVar<T> (private val defaultValue: T) {
    var value: T = defaultValue
        set(value) {
            field = value
            observable.onNext(field)
        }

    val observable = PublishSubject.create<T>().toSerialized()

    override fun toString(): String {
        return "RxVar(defaultValue=$defaultValue, value=$value)"
    }
}