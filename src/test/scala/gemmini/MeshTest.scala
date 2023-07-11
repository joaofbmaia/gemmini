package gemmini

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.experimental.{expose}
import chiseltest.simulator.WriteVcdAnnotation
import chisel3.stage.ChiselStage

import scala.io.Source
import chipsalliance.rocketchip.config

class MeshTest extends AnyFlatSpec with ChiselScalatestTester {
  "Mesh" should "perform OS matrix multiplication correctly" in {
    val interconnectConfig = CommonInterconnectConfigs.DefaultICConfig
    val sequenceTableSize = 4
    val controlPatternTableSize = 4
    val meshRows = 2
    val meshColumns = 2

    val bitstream = Source.fromFile("/home/asuka/thesis/chipyard/generators/gemmini/OS_GEMM_bit_mesh.bin").map(_.toByte).grouped(4)
    
    (new ChiselStage).emitVerilog(new MeshWrapper(meshRows, meshColumns, interconnectConfig, sequenceTableSize, controlPatternTableSize))
    test(new MeshWrapper(meshRows, meshColumns, interconnectConfig, sequenceTableSize, controlPatternTableSize)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        // lets write the configuration
        dut.io.rcfg_start.poke(true)
        dut.clock.step(1)
        dut.io.rcfg_start.poke(false)

        val se_word_number = 2
        val cpg_word_number = 2
        val config_cycles = (meshRows * sequenceTableSize * se_word_number) + (controlPatternTableSize * cpg_word_number * meshRows)

        for (i <- 0 until config_cycles) {
          for (c <- 0 until meshColumns) {
            dut.io.in_v_bcast(c).poke(BigInt(bitstream.next().toArray))
          }
          dut.clock.step(1)
        }
        
        // lets reset
        dut.io.cycle_fire.poke(true)
        dut.io.sequencer_reset.poke(true.B)
        dut.clock.step(1)
        dut.io.sequencer_reset.poke(false.B)
        // extra clock cycle to update cpg registers
        dut.clock.step(1)

        dut.clock.step(1)
        
        dut.io.in_v(0).poke(11) // d21
        dut.clock.step(1)
        dut.io.in_v(0).poke(9) // d11
        dut.io.in_v(1).poke(12) // d22
        dut.clock.step(1)
        dut.io.in_v(1).poke(10) // d12
        dut.io.in_v_bcast(0).poke(5) // b11
        dut.io.in_h_bcast(0).poke(1) // a11
        dut.clock.step(1)
        dut.io.in_v_bcast(0).poke(7) // b21
        dut.io.in_v_bcast(1).poke(6) // b12
        dut.io.in_h_bcast(0).poke(2) // a12
        dut.io.in_h_bcast(1).poke(3) // a21
        dut.clock.step(1)
        dut.io.in_v_bcast(1).poke(8) // b22
        dut.io.in_h_bcast(1).poke(4) // a22
        dut.clock.step(2)
        dut.io.out(0).expect(54) // c21
        dut.clock.step(1)
        dut.io.out(0).expect(28) // c11
        dut.io.out(1).expect(62) // c22
        dut.clock.step(1)
        dut.io.out(1).expect(32) // c12

        
    }
  }
}

