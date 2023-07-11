package gemmini

import chisel3._
import chisel3.util._
import chisel3.experimental._

class MeshReconfigurationController(meshRows: Int, meshColumns: Int, sequenceTableSize: Int, controlPatternTableSize: Int) extends Module {
  val se_word_number = 2
  val cpg_word_number = 2
  val se_word_sel_width = 1
  val cpg_word_sel_width = 1

  val io = IO(new Bundle {
    val start = Input(Bool())
    val done = Output(Bool())

    // sequencer reconfiguration
    val sequencer_rcfg = Output(new TableReconfigurationControl(sequenceTableSize, se_word_sel_width))
    val sequencer_row_select = Output(UInt(log2Ceil(meshRows).W))

    // mesh reconfiguration
    val mesh_rcfg = Output(new TableReconfigurationControl(controlPatternTableSize, cpg_word_sel_width))
    val mesh_rcfg_active = Output(Bool())
  })

  object State extends ChiselEnum {
    val seqLoop, cpgLoop, done = Value
  }

  val state = RegInit(State.done)

  val seqR = RegInit(0.U(log2Ceil(meshRows).W))
  val seqL = RegInit(0.U(log2Ceil(sequenceTableSize).W))
  val seqW = RegInit(0.U(se_word_sel_width.W))
  
  val cpgL = RegInit(0.U(log2Ceil(controlPatternTableSize).W))
  val cpgW = RegInit(0.U(cpg_word_sel_width.W))
  val cpgR = RegInit(0.U(log2Ceil(meshRows).W))

  io.done := state === State.done

  io.sequencer_row_select := seqR
  io.sequencer_rcfg.line_index := seqL
  io.sequencer_rcfg.word_sel := seqW
  io.sequencer_rcfg.write_enable := state === State.seqLoop

  io.mesh_rcfg.line_index := cpgL
  io.mesh_rcfg.word_sel := cpgW
  io.mesh_rcfg.write_enable := cpgR === (meshRows - 1).U
  io.mesh_rcfg_active := state === State.cpgLoop

  switch(state) {
    is(State.seqLoop) {
      when (seqW === (se_word_number - 1).U) {
        seqW := 0.U
        when (seqL === (sequenceTableSize - 1).U) {
          seqL := 0.U
          when (seqR === (meshRows - 1).U) {
            state := State.cpgLoop

            seqR := 0.U
            seqL := 0.U
            seqW := 0.U

            cpgL := 0.U
            cpgW := 0.U
            cpgR := 0.U
          } .otherwise {
            seqR := seqR + 1.U
          }
        } .otherwise {
          seqL := seqL + 1.U
        }
      } .otherwise {
        seqW := seqW + 1.U
      }
    }
    is(State.cpgLoop) {
      when (cpgR === (meshRows - 1).U) {
        cpgR := 0.U
        when (cpgW === (cpg_word_number - 1).U) {
          cpgW := 0.U
          when (cpgL === (controlPatternTableSize - 1).U) {
            state := State.done

            seqR := 0.U
            seqL := 0.U
            seqW := 0.U
                  
            cpgL := 0.U
            cpgW := 0.U
            cpgR := 0.U
          } .otherwise {
            cpgL := cpgL + 1.U
          }
        } .otherwise {
          cpgW := cpgW + 1.U
        }
      } .otherwise {
        cpgR := cpgR + 1.U
      }
    }
    is(State.done) {
      when (io.start) {
        state := State.seqLoop
        seqR := 0.U
        seqL := 0.U
        seqW := 0.U
          
        cpgL := 0.U
        cpgW := 0.U
        cpgR := 0.U
      }
    }
  }

  
  
}