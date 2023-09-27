package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import chisel3.stage.ChiselStage

class VariablePipeTest extends AnyFlatSpec with ChiselScalatestTester {
  "VariablePipe" should "work :')" in {
    (new ChiselStage).emitVerilog(new VariablePipe(UInt(32.W), 4))
    test(new VariablePipe(UInt(32.W), 4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.io.in.valid.poke(true)
        dut.io.in.bits.poke(69)
        dut.io.latency.poke(0)
        dut.io.out.bits.expect(69)
        dut.clock.step(1)
        dut.io.latency.poke(1)
        dut.clock.step(1)
        dut.io.out.bits.expect(69)
        dut.io.in.bits.poke(70)
        dut.clock.step(1)
        dut.io.out.bits.expect(70)
        dut.io.latency.poke(4)
        dut.io.in.bits.poke(71)
        dut.clock.step(1)
        dut.io.in.bits.poke(37)
        dut.io.in.valid.poke(false)
        dut.clock.step(1)
        dut.io.in.valid.poke(true)
        dut.io.in.bits.poke(72)
        dut.clock.step(1)
        dut.io.out.bits.expect(69)
        dut.clock.step(1)
        dut.io.out.bits.expect(70)
        dut.clock.step(1)
        dut.io.out.bits.expect(71)
        dut.clock.step(1)
        dut.io.out.bits.expect(72)
        dut.clock.step(1)
    }
  }
}
