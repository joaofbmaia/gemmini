package gemmini

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object BigALUSel extends ChiselEnum {
  val MUL, ADD, SUB = Value
}

object SmallALUSel extends ChiselEnum {
  val ADD, SUB = Value
}

class FUControl[T <: Data : Arithmetic](interconnectConfig : InterconnectConfig[T]) extends Bundle {
    val big_alu_sel = BigALUSel()
    val small_alu_sel = SmallALUSel()
    val shift = UInt(log2Up(interconnectConfig.interPEType.getWidth).W) // TODO this isn't correct for Floats
}

class FU[T <: Data](interconnectConfig : InterconnectConfig[T])(implicit ev: Arithmetic[T]) extends Module {
    import ev._
    val io = IO(new Bundle {
        val a = Input(interconnectConfig.interPEType)
        val b = Input(interconnectConfig.interPEType)
        val c = Input(interconnectConfig.interPEType)
        val p = Output(interconnectConfig.interPEType)
        val control = Input(new FUControl(interconnectConfig))
    })

    val big_alu = Wire(interconnectConfig.interPEType)
    val small_alu = Wire(interconnectConfig.interPEType)

    big_alu := io.a * io.b // default
    switch (io.control.big_alu_sel) {
        is (BigALUSel.MUL) {
            big_alu := io.a * io.b
        }
        is (BigALUSel.ADD) {
            big_alu := io.a + io.b
        }
        is (BigALUSel.SUB) {
            big_alu := io.a - io.b
        }
    }

    small_alu := big_alu + io.c // default
    switch (io.control.small_alu_sel) {
        is (SmallALUSel.ADD) {
            small_alu := big_alu + io.c
        }
        is (SmallALUSel.SUB) {
            small_alu := big_alu - io.c
        }
    }

    io.p := (small_alu >> io.control.shift)
}