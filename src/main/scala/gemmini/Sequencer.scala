package gemmini

import chisel3._
import chisel3.util._

class Sequencer[T <: Data](interconnectConfig : InterconnectConfig[T], sequenceTableSize: Int, controlPatternTableSize: Int, meshRows: Int, meshColumns: Int) extends Module { 
  val sequencing_elements: Seq[Seq[SequencingElement]] = Seq.fill(meshRows, meshColumns)(Module(new SequencingElement(sequenceTableSize, controlPatternTableSize, interconnectConfig.verticalBroadcastType.getWidth)))
  val se_line_coalescer_word_sel_width = sequencing_elements(0)(0).se_line_coalescer.word_sel_width

  val io = IO(new Bundle {
    val cycle_fire = Input(Bool())
    val sequencer_reset = Input(Bool())
    
    // which configuration pattern to use
    val next_control_pattern_indexes = Output(Vec(meshRows, Vec(meshColumns, UInt(log2Ceil(controlPatternTableSize).W))))

    // true when the configuration pattern to use changes
    val new_control_pattern = Output(Vec(meshRows, Vec(meshColumns, Bool())))

    // reconfiguration
    val rcfg = Input(new TableReconfigurationControl(sequenceTableSize, se_line_coalescer_word_sel_width))
    val row_select = Input(UInt(log2Ceil(meshRows).W))
    val write_data = Input(Vec(meshColumns, UInt(interconnectConfig.verticalBroadcastType.getWidth.W)))
  })


  // Global Fire Counter
  val fire_counter = RegInit(0.U(32.W))
  when(io.sequencer_reset) {
    fire_counter := 0.U
  }.elsewhen(io.cycle_fire) {
    fire_counter := fire_counter + 1.U
  }

  // Sequencinig elements
  for (r <- 0 until meshRows) {
    for (c <- 0 until meshColumns) {
      sequencing_elements(r)(c).io.fire_counter := fire_counter
      sequencing_elements(r)(c).io.sequencer_reset := io.sequencer_reset
      io.next_control_pattern_indexes(r)(c) := sequencing_elements(r)(c).io.next_control_pattern_index
      io.new_control_pattern(r)(c) := sequencing_elements(r)(c).io.new_control_pattern
    }
  }

  // Reconfiguration
  for (r <- 0 until meshRows) {
    for (c <- 0 until meshColumns) {
      sequencing_elements(r)(c).io.rcfg.write_enable := Mux(io.row_select === r.U, io.rcfg.write_enable, false.B)
      sequencing_elements(r)(c).io.rcfg.line_index := io.rcfg.line_index
      sequencing_elements(r)(c).io.rcfg.word_sel := io.rcfg.word_sel
      sequencing_elements(r)(c).io.write_data := io.write_data(c)
    }
  }
}
