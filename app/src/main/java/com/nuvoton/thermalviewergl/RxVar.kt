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

    val observable = BehaviorSubject.createDefault(value)

    var process: T = defaultValue
    set(value) {
        field = value
        flowable.onNext(field)
    }

    val flowable = PublishProcessor.create<T>()

    override fun toString(): String {
        return "RxVar(defaultValue=$defaultValue, value=$value)"
    }
}

class ImageBehaviorSubject(size: Int) {
    val subject: PublishSubject<ByteArray> = PublishSubject.create()
    var byteArray: ByteArray = ByteArray(size)

    fun updateImageData(byteBuffer: ByteBuffer) {
//        synchronized(byteArray) {
            byteBuffer.get(byteArray)
            subject.onNext(byteArray)
//        }
    }
}