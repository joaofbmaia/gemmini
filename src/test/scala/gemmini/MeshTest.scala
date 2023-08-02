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

    val bitstream = Source.fromFile("/home/asuka/thesis/chipyard/generators/gemmini/OS_GEMM_2x2_bit_full.bin", "ISO8859-1").map(_.toByte).grouped(4)
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
            dut.io.in_v_grid(c).poke(BigInt(bitstream.next().toArray))
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
        dut.io.in_v_grid(0).poke(5) // b11
        dut.io.in_h_grid(0).poke(1) // a11
        dut.clock.step(1)
        dut.io.in_v_grid(0).poke(7) // b21
        dut.io.in_v_grid(1).poke(6) // b12
        dut.io.in_h_grid(0).poke(2) // a12
        dut.io.in_h_grid(1).poke(3) // a21
        dut.clock.step(1)
        dut.io.in_v_grid(1).poke(8) // b22
        dut.io.in_h_grid(1).poke(4) // a22
        dut.clock.step(1)
        dut.io.out(0).expect(54) // c21
        dut.clock.step(1)
        dut.io.out(0).expect(28) // c11
        dut.io.out(1).expect(62) // c22
        dut.clock.step(1)
        dut.io.out(1).expect(32) // c12

        
    }
  }

  it should "perform WS matrix multiplication correctly" in {
    val interconnectConfig = CommonInterconnectConfigs.DefaultICConfig
    val sequenceTableSize = 4
    val controlPatternTableSize = 4
    val meshRows = 2
    val meshColumns = 2

    val bitstream = Source.fromFile("/home/asuka/thesis/chipyard/generators/gemmini/WS_GEMM_2x2_bit_full.bin", "ISO8859-1").map(_.toByte).grouped(4)
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
            dut.io.in_v_grid(c).poke(BigInt(bitstream.next().toArray))
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
        
        dut.io.in_v(0).poke(7) // b21
        dut.clock.step(1)
        dut.io.in_v(0).poke(5) // b11
        dut.io.in_v(1).poke(8) // b22
        dut.clock.step(1)
        dut.io.in_v(1).poke(6) // b12
        dut.io.in_v_grid(0).poke(9) // d11
        dut.io.in_h_grid(0).poke(1) // a11
        dut.clock.step(1)
        dut.io.in_v_grid(0).poke(11) // d21
        dut.io.in_v_grid(1).poke(10) // d12
        dut.io.in_h_grid(0).poke(3) // a21
        dut.io.in_h_grid(1).poke(2) // a12
        dut.io.out_v_grid(0).expect(28) // c11
        dut.clock.step(1)
        dut.io.in_v_grid(1).poke(12) // d22
        dut.io.in_h_grid(1).poke(4) // a22
        dut.io.out_v_grid(0).expect(54) // c21
        dut.io.out_v_grid(1).expect(32) // c12
        dut.clock.step(1)
        dut.io.out_v_grid(1).expect(62) // c22
        dut.clock.step(1)
    }
  }

  it should "perform tiled OS GEMM correctly" in {
    def tiled_OS_test(mesh_dim: Int, m: Int, k: Int, n: Int) {
      val interconnectConfig = CommonInterconnectConfigs.DefaultICConfig
      val sequenceTableSize = 4
      val controlPatternTableSize = 4
      val meshRows = mesh_dim
      val meshColumns = mesh_dim

      assert(meshRows == meshColumns)
      assert(m % meshRows == 0)
      assert(k % meshRows == 0)
      assert(n % meshColumns == 0)

      val base_dir = "test_files/"
      val dimXdim = s"${mesh_dim}x${mesh_dim}"
      val mXkXn = s"${m}x${k}x${n}"
      val base_name = "OS_GEMM_" + mXkXn + "_tiled_" + dimXdim + "_"

      val bitstream = Source.fromFile(base_dir + base_name + "bit_full.bin", "ISO8859-1").map(_.toByte).grouped(interconnectConfig.interPEType.getWidth / 8)
      val a_stream = Source.fromFile(base_dir + base_name + "a_stream.bin", "ISO8859-1").map(_.toByte).grouped(interconnectConfig.horizontalGridType.getWidth / 8)
      val b_stream = Source.fromFile(base_dir + base_name + "b_stream.bin", "ISO8859-1").map(_.toByte).grouped(interconnectConfig.verticalGridType.getWidth / 8)
      val d_stream = Source.fromFile(base_dir + base_name + "d_stream.bin", "ISO8859-1").map(_.toByte).grouped(interconnectConfig.interPEType.getWidth / 8)
      val c_stream = Source.fromFile(base_dir + base_name + "c_stream.bin", "ISO8859-1").map(_.toByte).grouped(interconnectConfig.interPEType.getWidth / 8)

      //(new ChiselStage).emitVerilog(new MeshWrapper(meshRows, meshColumns, interconnectConfig, sequenceTableSize, controlPatternTableSize))
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
              dut.io.in_v_grid(c).poke(BigInt(bitstream.next().toArray))
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

          // one idle cycle
          dut.clock.step(1)

          while (c_stream.hasNext) {
            for (c <- 0 until meshColumns) {
              dut.io.in_h_grid(c).poke(BigInt(a_stream.next().toArray))
              dut.io.in_v_grid(c).poke(BigInt(b_stream.next().toArray))
              dut.io.in_v(c).poke(BigInt(d_stream.next().toArray))
              // println(s"expecting ${BigInt(c_stream.next().toArray)}")
              // println(s"got ${dut.io.out(c).peek()}")
              dut.io.out(c).expect(BigInt(c_stream.next().toArray))
            }
            dut.clock.step(1)
            
          }
      }
    }

    tiled_OS_test(mesh_dim = 2, m = 2, k = 2, n = 2)
    tiled_OS_test(mesh_dim = 2, m = 4, k = 2, n = 2)
    tiled_OS_test(mesh_dim = 2, m = 2, k = 4, n = 2)
    tiled_OS_test(mesh_dim = 2, m = 2, k = 4, n = 4)
    tiled_OS_test(mesh_dim = 2, m = 4, k = 4, n = 2)
    tiled_OS_test(mesh_dim = 2, m = 4, k = 2, n = 4)
    tiled_OS_test(mesh_dim = 2, m = 2, k = 4, n = 4)
    tiled_OS_test(mesh_dim = 2, m = 4, k = 4, n = 4)
    tiled_OS_test(mesh_dim = 2, m = 8, k = 8, n = 8)

    tiled_OS_test(mesh_dim = 3, m = 3, k = 3, n = 3)
    tiled_OS_test(mesh_dim = 3, m = 6, k = 3, n = 3)
    tiled_OS_test(mesh_dim = 3, m = 3, k = 6, n = 3)
    tiled_OS_test(mesh_dim = 3, m = 3, k = 6, n = 6)
    tiled_OS_test(mesh_dim = 3, m = 6, k = 6, n = 3)
    tiled_OS_test(mesh_dim = 3, m = 6, k = 3, n = 6)
    tiled_OS_test(mesh_dim = 3, m = 3, k = 6, n = 6)
    tiled_OS_test(mesh_dim = 3, m = 6, k = 6, n = 6)
    tiled_OS_test(mesh_dim = 3, m = 9, k = 9, n = 9)

    tiled_OS_test(mesh_dim = 4, m = 4, k = 4, n = 4)
    tiled_OS_test(mesh_dim = 4, m = 8, k = 4, n = 4)
    tiled_OS_test(mesh_dim = 4, m = 4, k = 8, n = 4)
    tiled_OS_test(mesh_dim = 4, m = 4, k = 8, n = 8)
    tiled_OS_test(mesh_dim = 4, m = 8, k = 8, n = 4)
    tiled_OS_test(mesh_dim = 4, m = 8, k = 4, n = 8)
    tiled_OS_test(mesh_dim = 4, m = 4, k = 8, n = 8)
    tiled_OS_test(mesh_dim = 4, m = 8, k = 8, n = 8)
    tiled_OS_test(mesh_dim = 4, m = 16, k = 16, n = 16)

    // tiled_OS_test(mesh_dim = 8, m = 8, k = 8, n = 8)
    // tiled_OS_test(mesh_dim = 8, m = 24, k = 24, n = 24)

    // tiled_OS_test(mesh_dim = 16, m = 16, k = 16, n = 16)
    // tiled_OS_test(mesh_dim = 16, m = 32, k = 32, n = 32)

  }
}

