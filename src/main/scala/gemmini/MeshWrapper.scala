package gemmini

import chisel3._
import chisel3.util._
import chisel3.experimental._

class MeshWrapper[T <: Data: Arithmetic](meshRows: Int, meshColumns: Int, interconnectConfig : InterconnectConfig[T], sequenceTableSize: Int, controlPatternTableSize: Int) extends Module {
      val io = IO(new Bundle {
        // sequencer IO
        val cycle_fire = Input(Bool())
        val sequencer_reset = Input(Bool())

        // mesh IO
        val in_h_grid = Input(Vec(meshRows, interconnectConfig.horizontalGridType))
        val in_v_grid = Input(Vec(meshColumns, interconnectConfig.verticalGridType))
        val in_v = Input(Vec(meshColumns, interconnectConfig.interPEType))
        val in_h = Input(Vec(meshRows, interconnectConfig.interPEType))

        val out_h_grid = Output(Vec(meshRows, interconnectConfig.horizontalGridType)) // do i need this?
        val out_v_grid = Output(Vec(meshColumns, interconnectConfig.verticalGridType)) //do i need this?
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

      mesh.io.in_h_grid := io.in_h_grid
      mesh.io.in_v_grid := io.in_v_grid
      mesh.io.in_v := io.in_v
      mesh.io.in_h := io.in_h
      io.out_h_grid := mesh.io.out_h_grid
      io.out_v_grid := mesh.io.out_v_grid
      io.out := mesh.io.out

      // sequencer to mesh
      mesh.io.next_control_pattern_indexes := sequencer.io.next_control_pattern_indexes
      mesh.io.new_control_pattern := sequencer.io.new_control_pattern

      // sequencer rcfg
      sequencer.io.rcfg := rcfg_controller.io.sequencer_rcfg
      sequencer.io.row_select := rcfg_controller.io.sequencer_row_select
      for (c <- 0 until meshColumns) {
        sequencer.io.write_data(c) := io.in_v_grid(c).asUInt
      }

      // mesh rcfg
      mesh.io.rcfg := rcfg_controller.io.mesh_rcfg
      mesh.io.rcfg_active := rcfg_controller.io.mesh_rcfg_active

      rcfg_controller.io.start := io.rcfg_start
      io.rcfg_done := rcfg_controller.io.done
    
    }