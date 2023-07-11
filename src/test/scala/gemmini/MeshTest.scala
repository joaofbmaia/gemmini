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

class MeshTest extends AnyFlatSpec with ChiselScalatestTester {
  "Mesh" should "perform OS matrix multiplication correctly" in {
    val interconnectConfig = CommonInterconnectConfigs.DefaultICConfig
    val sequenceTableSize = 4
    val controlPatternTableSize = 4
    val meshRows = 2
    val meshColumns = 2

    val seq_config = Source.fromFile("/home/asuka/thesis/chipyard/generators/gemmini/OS_GEMM_bit_seq_load.bin").map(_.toByte).grouped(4)
    val mesh_config = Source.fromFile("/home/asuka/thesis/chipyard/generators/gemmini/OS_GEMM_bit_cpg_load.bin").map(_.toByte).grouped(4)

    class MeshWrapper[T <: Data: Arithmetic](meshRows: Int, meshColumns: Int, interconnectConfig : InterconnectConfig[T], sequenceTableSize: Int, controlPatternTableSize: Int) extends Module {
      val io = IO(new Bundle {
        // sequencer IO
        val cycle_fire = Input(Bool())
        val sequencer_reset = Input(Bool())

        // mesh IO
        val in_h_bcast = Input(Vec(meshRows, interconnectConfig.horizontalBroadcastType))
        val in_v_bcast = Input(Vec(meshColumns, interconnectConfig.verticalBroadcastType))
        val in_v = Input(Vec(meshColumns, interconnectConfig.interPEType))
        val in_h = Input(Vec(meshRows, interconnectConfig.interPEType))

        val out_h_bcast = Output(Vec(meshRows, interconnectConfig.horizontalBroadcastType)) // do i need this?
        val out_v_bcast = Output(Vec(meshColumns, interconnectConfig.verticalBroadcastType)) //do i need this?
        val out = Output(Vec(meshColumns, interconnectConfig.interPEType))

        // reconfiguration
        val rcfg_start = Input(Bool())
        val rcfg_done = Output(Bool())
      })

      val sequencer = Module(new Sequencer(interconnectConfig, sequenceTableSize, controlPatternTableSize, meshRows, meshColumns))
      val mesh = Module(new Mesh(interconnectConfig, controlPatternTableSize, meshRows, meshColumns))
      val rcfg_controller = Module(new MeshReconfigurationController(meshRows, meshColumns, sequenceTableSize, controlPatternTableSize))

      // sequencer external IO
      sequencer.io.cycle_fire := io.cycle_fire && rcfg_controller.io.done
      sequencer.io.sequencer_reset := io.sequencer_reset

      // mesh external IO
      mesh.io.valid := io.cycle_fire && rcfg_controller.io.done

      mesh.io.in_h_bcast := io.in_h_bcast
      mesh.io.in_v_bcast := io.in_v_bcast
      mesh.io.in_v := io.in_v
      mesh.io.in_h := io.in_h
      io.out_h_bcast := mesh.io.out_h_bcast
      io.out_v_bcast := mesh.io.out_v_bcast
      io.out := mesh.io.out

      // sequencer to mesh
      mesh.io.next_control_pattern_indexes := sequencer.io.next_control_pattern_indexes
      mesh.io.new_control_pattern := sequencer.io.new_control_pattern

      // sequencer rcfg
      sequencer.io.rcfg := rcfg_controller.io.sequencer_rcfg
      sequencer.io.row_select := rcfg_controller.io.sequencer_row_select
      for (c <- 0 until meshColumns) {
        sequencer.io.write_data(c) := io.in_v_bcast(c).asUInt
      }

      // mesh rcfg
      mesh.io.rcfg := rcfg_controller.io.mesh_rcfg
      mesh.io.rcfg_active := rcfg_controller.io.mesh_rcfg_active

      rcfg_controller.io.start := io.rcfg_start
      io.rcfg_done := rcfg_controller.io.done
    
    }
    
    (new ChiselStage).emitVerilog(new MeshWrapper(meshRows, meshColumns, interconnectConfig, sequenceTableSize, controlPatternTableSize))
    test(new MeshWrapper(meshRows, meshColumns, interconnectConfig, sequenceTableSize, controlPatternTableSize)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        // lets write to the sequencer memory
        dut.io.rcfg_start.poke(true)
        dut.clock.step(1)
        dut.io.rcfg_start.poke(false)
        for (r <- 0 until meshRows) {
          for (l <- 0 until sequenceTableSize) {
            for (w <- 0 until 2) {
              for (c <- 0 until meshColumns) {
                dut.io.in_v_bcast(c).poke(BigInt(seq_config.next().toArray))
              }
              dut.clock.step(1)
            }
          }
        }
        // sequencer is programmed

        // lets write to the cgp memory
        for (l <- 0 until controlPatternTableSize) {
          for (w <- 0 until 2) {
            for (r <- 0 until meshRows) {
              for (c <- 0 until meshColumns) {
                dut.io.in_v_bcast(c).poke(BigInt(mesh_config.next().toArray))
              }
              dut.clock.step(1)
            }
          }
        }
        dut.io.rcfg_done.expect(true)
        // cgp is programmed
        
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

