package gemmini

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import chisel3.stage.ChiselStage

class LineCoalescerTest extends AnyFlatSpec with ChiselScalatestTester {
  "LineCoalescer" should "coalesce words into lines correctly for 3 words" in {
    (new ChiselStage).emitVerilog(new LineCoalescer(4, 10))
    test(new LineCoalescer(4, 10)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        println(dut.word_number)
        dut.io.word_select.poke(0)
        dut.io.in.valid.poke(true)
        dut.io.in.bits.poke(1)
        dut.clock.step(1)
        dut.io.word_select.poke(1)
        dut.io.in.bits.poke(2)
        dut.clock.step(1)
        dut.io.word_select.poke(2)
        dut.io.in.bits.poke(3)
        dut.io.out.valid.expect(true)
        dut.io.out.bits.expect(291)
        dut.clock.step(1)
    }
  }
  "LineCoalescer" should "coalesce words into lines correctly for 2 words" in {
    (new ChiselStage).emitVerilog(new LineCoalescer(4, 6))
    test(new LineCoalescer(4, 6)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        println(dut.word_number)
        dut.io.word_select.poke(0)
        dut.io.in.valid.poke(true)
        dut.io.in.bits.poke(1)
        dut.clock.step(1)
        dut.io.word_select.poke(1)
        dut.io.in.bits.poke(2)
        dut.io.out.valid.expect(true)
        dut.io.out.bits.expect(18)
        dut.clock.step(1)
    }
  }
  "LineCoalescer" should "work when the line is smaller than the word" in {
    (new ChiselStage).emitVerilog(new LineCoalescer(6, 4))
    test(new LineCoalescer(6, 4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        println(dut.word_number)
        println(dut.io.word_select.getWidth)
        //dut.io.word_select.poke(0)
        dut.io.in.valid.poke(true)
        dut.io.in.bits.poke(5)
        dut.io.out.valid.expect(true)
        dut.io.out.bits.expect(5)
        dut.clock.step(1)
    }
  }
}
